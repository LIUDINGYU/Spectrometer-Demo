package com.example.ldy.spectrometertest;

import android.app.ActionBar;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.oceanoptics.spectrometer.Coefficients;
import com.oceanoptics.spectrometer.Embed2000Plus;
import com.oceanoptics.spectrometer.Spectrum;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String ACTION_USB_PERMISSION = "com.oceanoptics.spectrometer.USB_PERMISSION";

    private Embed2000Plus embed;
    LinearLayout chart = (LinearLayout)findViewById(R.id.chart);
    GraphicalView gv;
    private String savePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
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

        Button btnSerial = (Button) findViewById(R.id.getSerialNumber);
        btnSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (embed != null) {
                    String serialNumber = embed.getSerialNumber();
                    TextView message = (TextView) findViewById(R.id.message);
                    message.setText(serialNumber);
                }
            }
        });

        Button btnWLCoeff = (Button) findViewById(R.id.getWavelengthCoeff);
        btnWLCoeff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (embed != null) {
                    try {
                        showWavelengthInfo(embed.getWavelengthCalibrationCoefficients());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void showWavelengthInfo(Coefficients coefficients) {
                String info = "intercept is " + coefficients._WlIntercept + "\r\n";
                info += "first coeff is " + coefficients._WlFirst + "\r\n";
                info += " second coeff is " + coefficients._WlSecond + "\r\n";
                info += " third coeff is " + coefficients._WlThird;
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG).show();
            }
        });

        Button btnGetNonLineariltyCoefficients = (Button) findViewById(R.id.getNonLineariltyCoefficients);
        btnGetNonLineariltyCoefficients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (embed != null) {
                    try {
                        Coefficients coeffs = embed.getNonlinearityCoefficients();
                        showCoefficients(coeffs);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void showCoefficients(Coefficients coeffs) {
                String info = "NonLinearity 0 is: " + coeffs._NlCoef0 + "\r\n";
                info += "NonLinearity 1 is: " + coeffs._NlCoef1 + "\r\n";
                info += "NonLinearity 2 is: " + coeffs._NlCoef2 + "\r\n";
                info += "NonLinearity 3 is: " + coeffs._NlCoef3 + "\r\n";
                info += "NonLinearity 4 is: " + coeffs._NlCoef4 + "\r\n";
                info += "NonLinearity 5 is: " + coeffs._NlCoef5 + "\r\n";
                info += "NonLinearity 6 is: " + coeffs._NlCoef6 + "\r\n";
                info += "NonLinearity 7 is: " + coeffs._NlCoef7 + "\r\n";
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG).show();
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
                    embed.setIntegrationTime(100000);	// 100 ms
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
                for (int i = 0; i < wavelength.length; i++) {
                    Log.d(TAG, "pixel " + i + " value is " + + wavelength[i] + ", " + intensity[i]);
                }
                //DrawPic(wavelength, intensity);
            }

            private void DrawPic(double[] xValue, double[] yValue){
                XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

                XYSeries line1 = new XYSeries("Wavelength");
                XYSeriesRenderer lineRenderer = new XYSeriesRenderer();

                for (int i = 0; i < xValue.length; i++){
                    line1.add(xValue[i], yValue[i]);
                }
                lineRenderer.setColor(Color.BLUE);
                lineRenderer.setPointStyle(PointStyle.SQUARE);
                lineRenderer.setDisplayChartValues(false);
                lineRenderer.setChartValuesTextSize(30);

                renderer.setXLabelsPadding(20);
                renderer.setYLabelsPadding(-20);
                renderer.setXTitle("wavelength");
                renderer.setYTitle("intensity");
                renderer.setAxisTitleTextSize(50);
                renderer.setXLabels(10);
                renderer.setYLabels(10);
                renderer.setLabelsTextSize(30);
                renderer.setXAxisMin(0);
                renderer.setXAxisMax(10);
                renderer.setYAxisMin(100);
                //renderer.setRange(new double[] {0d,10d,100d,150d});
                //renderer.setPanEnabled(true, true);
                renderer.setShowGrid(true);

                dataset.addSeries(line1);
                renderer.addSeriesRenderer(lineRenderer);
                //final Intent intent = ChartFactory.getLineChartIntent(this, dataset, renderer);
                gv = ChartFactory.getLineChartView(MainActivity.this, dataset, renderer);
                chart.addView(gv, ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
            }
        });

        Button btnSaveImage = (Button)findViewById(R.id.saveImage);
        btnSaveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap img = loadBitmapFromView(chart);
                SaveImage(img);
            }
            //根据GraphicalView返回相应Bitmap类型图片，供ViewSavaToImage调用
            private Bitmap loadBitmapFromView(View v){
                int w = v.getWidth();
                int h = v.getHeight();
                Bitmap bmp = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                c.drawColor(Color.WHITE);

                v.layout(0,0,w,h);
                v.draw(c);
                Log.d(TAG, "loadBitmapFromView: 完成Bitmap图像生成");
                return bmp;
            }

            //将Bitmap类型图片保存到本地相册中
            private void SaveImage(final Bitmap bitmap){
                Log.d(TAG, "SaveImage: 已进入SaveImage");
                gv.setDrawingCacheEnabled(true);
                gv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);
                gv.setDrawingCacheBackgroundColor(Color.WHITE);

                boolean isHasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
                Log.d(TAG, "SaveImage: 完成SD卡判断： "+isHasSDCard);

                if(isHasSDCard){
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/"+System.currentTimeMillis() + ".jpg";
                    savePath = path;
                    final File file = new File(path);
                    try{
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG,90,fos);

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Uri uri = Uri.fromFile(file);
                                getBaseContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,uri));
                            }
                        });
                        fos.flush();
                        fos.close();
                    }catch (FileNotFoundException e){
                        Looper.prepare();
                        Toast toast = Toast.makeText(getApplicationContext(), "保存失败",Toast.LENGTH_SHORT);
                        toast.show();
                        Looper.loop();
                    }catch (MalformedURLException e){
                        Looper.prepare();
                        Toast toast = Toast.makeText(getApplicationContext(), "保存失败",Toast.LENGTH_SHORT);
                        toast.show();
                        Looper.loop();
                    }catch (IOException e){
                        Looper.prepare();
                        Toast toast = Toast.makeText(getApplicationContext(), "保存失败",Toast.LENGTH_SHORT);
                        toast.show();
                        Looper.loop();
                    }
                }
            }
        });

        Button btnSendEmail = (Button)findViewById(R.id.sendToEmail);
        btnSendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: inner of sendEmail");
                //发送图片到指定邮箱的方法实现
                Intent email = new Intent(Intent.ACTION_SEND);
                //附件
                File file = new File(savePath);
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
                email.putExtra(Intent.EXTRA_STREAM, getUriForFile(MainActivity.this,file));

                //调用系统的邮件系统
                startActivity(Intent.createChooser(email, "请选择邮件发送软件"));
            }

        });
    }

    private void requestDevicePermission() {
        Log.d(TAG, "requestDevicePermission: inner of requestDevicePermission");
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String,UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList == null) {
            Log.d(TAG, "requestDevicePermission: deviceList = null");
            return;
        }
        for (UsbDevice device : deviceList.values()) {
            if (isEmbed(device)) {
                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
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
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                            embed = Embed2000Plus.getInstance();
                            embed.setupContext(usbManager, MainActivity.this, ACTION_USB_PERMISSION);
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

    //获取目标图片的URI，返回可供临时访问的URI
    private static Uri getUriForFile(Context context, File file){
        if(context == null || file == null){ //对空文件简单拦截
            throw new NullPointerException();
        }
        Uri uri;
        if(Build.VERSION.SDK_INT >= 24){
            uri = FileProvider.getUriForFile(context, "com.example.ldy.spectrometertest.fileprovider",file);
        }
        else{
            uri = Uri.fromFile(file);
        }
        return uri;
    }
}
