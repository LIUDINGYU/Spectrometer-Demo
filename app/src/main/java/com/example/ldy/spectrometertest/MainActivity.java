package com.example.ldy.spectrometertest;

import android.app.ActionBar;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.oceanoptics.spectrometer.Embed2000Plus;
import com.oceanoptics.spectrometer.Spectrum;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String ACTION_USB_PERMISSION = "com.oceanoptics.spectrometer.USB_PERMISSION";
    private boolean isHasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

    private String publicImagePath;

    static Embed2000Plus embed;

    //图像绘制相关变量
    private XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    LinearLayout chart;
    GraphicalView view;

    public static final int EXTERNAL_STORAGE_REQ_CODE = 10;

    //called when the activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //以下为读写SD卡的动态权限申请
        int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.
                    WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQ_CODE);
        }


        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        Button btnPermission = (Button) findViewById(R.id.getUsbPermission);
        btnPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: start device permission");
                requestDevicePermission();
            }
        });

        Button btnGetSpectrum = (Button) findViewById(R.id.getSpectrum);
        btnGetSpectrum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (embed != null) {
                    double[] wavelength = getWavelengthFromSpectrometer();
                    double[] intensity = getSpectrum();
                    showSpectrum(wavelength, intensity);
                    Log.d(TAG, "onClick: embed is " + embed);
                }
            }

            private double[] getWavelengthFromSpectrometer() {
                try {
                    return embed.getAllWavelengths();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            private double[] getSpectrum() {
                try {
                    embed.setIntegrationTime(10000);    // 积分时间可修改，改为1 x0ms，最开始为100ms
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }

                int numberOfPixels = embed.getNumberOfPixels();
                int numberOfDarkPixels = embed.getNumberOfDarkPixels();
                Spectrum ds = new Spectrum(numberOfPixels, numberOfDarkPixels);
                try {
                    embed.getSpectrum(ds);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                return ds.getSpectrum();
            }

            //用自定义的DrawPic方法，绘制折线图，并生成相应的GraphicalIntent
            //final Intent intent = DrawPic(wavelength, intensity);
            //输入量为两个double[]数组，返回值为GraphicalIntent类型，利用startActivity(intent)就可以启动全屏显示
            private void showSpectrum(double[] wavelength, double[] intensity) {
                String info = null;
                for (int i = 0; i < wavelength.length; i++) {
                    info += "pixel" + i + "value is" + wavelength[i] + "," + intensity[i];
                }
                //将波长和强度信息作为txt保存到本地
                saveSpectrumToTxt(info);

                //将波长信息绘图，并且加载到chart中
                chart = (LinearLayout) findViewById(R.id.chart);
                view = DrawLine(wavelength,intensity, dataset, renderer);
                chart.removeAllViews();
                chart.addView(view, ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
            }
        });

        ImageButton btnSaveImage = (ImageButton) findViewById(R.id.saveImage);
        btnSaveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if(view == null){
                //   Toast.makeText(MainActivity.this,"There is no spectra to load.",Toast.LENGTH_LONG).show();
                // }
                //else {
                Bitmap bitmap = loadBitmapFromView(view);
                SaveImage(bitmap);

                // }
            }

            private Bitmap loadBitmapFromView(View v) {
                int w = v.getWidth();
                int h = v.getHeight();
                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                c.drawColor(Color.WHITE);

                v.layout(0, 0, w, h);
                v.draw(c);
                Log.d(TAG, "loadBitmapFromView: 完成Bitmap图像生成");
                return bmp;
            }

            private void SaveImage(final Bitmap bitmap) {
                //Log.d(TAG, "SaveImage: inner of SaveImage method");
                view.setDrawingCacheEnabled(true);
                view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);
                view.setDrawingCacheBackgroundColor(Color.WHITE);

                if (isHasSDCard) {
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/" + System.currentTimeMillis() + ".jpg";
                    publicImagePath = path;
                    final File file = new File(path);
                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        Toast.makeText(MainActivity.this, "Image saved: " + publicImagePath, Toast.LENGTH_SHORT).show();

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Uri uri = Uri.fromFile(file);
                                getBaseContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                            }
                        });
                        fos.flush();
                        fos.close();
                    } catch (FileNotFoundException e) {
                        Looper.prepare();
                        Toast toast = Toast.makeText(getApplicationContext(), "保存失败", Toast.LENGTH_SHORT);
                        toast.show();
                        Looper.loop();
                    } catch (MalformedURLException e) {
                        Looper.prepare();
                        Toast toast = Toast.makeText(getApplicationContext(), "保存失败", Toast.LENGTH_SHORT);
                        toast.show();
                        Looper.loop();
                    } catch (IOException e) {
                        Looper.prepare();
                        Toast toast = Toast.makeText(getApplicationContext(), "保存失败", Toast.LENGTH_SHORT);
                        toast.show();
                        Looper.loop();
                    }
                }
            }
        });

        ImageButton sendEmail = (ImageButton) findViewById(R.id.sendEmail);
        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: inner of sendEmail");
                //发送图片到指定邮箱的方法实现
                Intent email = new Intent(Intent.ACTION_SEND);
                //附件
                File file = new File(publicImagePath);
                //邮件发送类型
                email.setType("image/jpg");
                //邮件接收者
                String emailReceiver = new String("1151951829@qq.com");
                String emailTitle = "这是标题";
                String emailContent = "这是邮件正文";

                //设置邮件
                email.putExtra(Intent.EXTRA_EMAIL, emailReceiver);
                email.putExtra(Intent.EXTRA_SUBJECT, emailTitle);
                email.putExtra(Intent.EXTRA_TEXT, emailContent);
                //email.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(file));
                email.putExtra(Intent.EXTRA_STREAM, getUriForFile(MainActivity.this, file));

                //调用系统的邮件系统
                startActivity(Intent.createChooser(email, "请选择邮件发送软件"));
            }
        });

        findViewById(R.id.aboutOpen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        });
    }

    //获取目标图片的URI，返回可供临时访问的URI
    private static Uri getUriForFile(Context context, File file) {
        if (context == null || file == null) { //对空文件简单拦截
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(context, "com.example.ldy.spectrometertest.fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    private void requestDevicePermission() {
        Log.d(TAG, "requestDevicePermission: inner of requestDevicePermission");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList == null) {
            Log.d(TAG, "requestDevicePermission: deviceList = null");
            return;
        }
        for (UsbDevice device : deviceList.values()) {
            if (isEmbed(device)) {
                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                        new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(device, mPermissionIntent);
                Log.d(TAG, "requestDevicePermission: device found");
                return;
            }
        }
    }

    private boolean isEmbed(UsbDevice device) {
        final int VendorID = 0x2457;
        final int ProductId = 0x101E;
        return (device.getVendorId() == VendorID && device.getProductId() == ProductId);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                            embed = Embed2000Plus.getInstance();
                            embed.setupContext(usbManager, MainActivity.this, ACTION_USB_PERMISSION);
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    private void saveSpectrumToTxt(String saveContent){
        Log.d(TAG, "saveSpectrumToTxt: start of saveTxt");
        BufferedWriter out = null;
        if(isHasSDCard) {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/Txtfiles/" + System.currentTimeMillis()+ ".txt";
            Log.d(TAG, "saveSpectrumToTxt: "+path);
            try{      //////这里运行错误，txt文件保存段需要重写 - 20190417
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path,true)));
                out.newLine();
                out.write(saveContent);
                Toast.makeText(this,"保存成功",Toast.LENGTH_SHORT).show();

            } catch (Exception e){
                e.printStackTrace();
            } finally{
                if(out!= null){
                    try{
                        out.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
            Log.d(TAG, "saveSpectrumToTxt: isHasSDCard is true. End of saveTxt");
        }
    }

    private GraphicalView DrawLine(double[] xValue, double[] yValue, XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer) {
        /*double[] xValue = {896.35,897.98,899.6,901.22,902.85,904.47,906.1,907.72,909.34,910.97,912.59,914.21,
                915.84,917.46,919.08,920.71,922.33,923.95,925.58,927.2,928.82,930.44,932.07,933.69,935.31,936.93,
                938.56,940.18,941.8,943.42,945.05,946.67,948.29,949.91,951.53,953.16,954.78,956.4,958.02,959.64,
                961.26,962.88,964.51,966.13,967.75,969.37,970.99,972.61,974.23,975.85
        };
        double[] yValue = {3036.65,3034.91,3043.28,3050.26,3055.14,3058.28,3064.91,3075.37,3082.7,3094.21,3107.81,3126.64,
                3150.36,3176.17,3196.4,3216.63,3231.98,3252.56,3259.89,3265.12,3271.05,3279.07,3279.77,3277.67,3282.21,
                3292.32,3300.7,3305.93,3314.65,3324.06,3325.11,3327.9,3318.14,3325.81,3326.86,3335.58,3330.34,
                3329.3,3328.25,3334.88,3337.67,3336.62,3338.37,3340.81,3340.46,3340.11,3334.53,3341.85,3341.16,3348.13
        };*/
        XYSeriesRenderer lineRenderer = new XYSeriesRenderer();
        XYSeries line1 = new XYSeries("wavelength");
        for (int i = 0; i < xValue.length; i++) {
            line1.add(xValue[i], yValue[i]);
        }

        lineRenderer.setColor(Color.BLUE);           //图线颜色
        lineRenderer.setPointStyle(PointStyle.POINT);   //数据点模式
        lineRenderer.setDisplayChartValues(false);      //是否显示各点数据，否
        lineRenderer.setChartValuesTextSize(30);        //点数据字体大小

        renderer.setXLabelsPadding(20);            //x轴下标签与x轴的距离
        renderer.setYLabelsPadding(20);            //y轴左标签与y轴的距离
        renderer.setXTitle("wavelength");          //x轴标题
        renderer.setYTitle("intensity");         //y轴标题
        renderer.setAxisTitleTextSize(30);         //轴标题字体大小
        //renderer.setXLabels(5);                 //x轴显示的坐标个数
        //renderer.setYLabels(5);
        renderer.setLabelsTextSize(30);           //轴标签字体大小
        //renderer.setXAxisMin(895);               //x轴坐标显示的第一个坐标数值
        //renderer.setXAxisMax(945);
        //renderer.setYAxisMin();
        //renderer.setYAxisMax(945);
        renderer.setMargins(new int[] {20,120,100,20});  //图表四周的范围，上左下右
        //renderer.setPanEnabled(true, true);
        renderer.setFitLegend(true);
        renderer.setShowGrid(false);

        dataset.addSeries(line1);
        renderer.addSeriesRenderer(lineRenderer);
        view = ChartFactory.getLineChartView(this, dataset, renderer);

        return view;
    }

}
