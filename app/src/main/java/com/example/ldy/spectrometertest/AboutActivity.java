/*
This activity helps show the basic parameters of the miniature spectrometer we used(Ocean Optics
EMBED2000+) and some information of the developer.
*/
package com.example.ldy.spectrometertest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.oceanoptics.spectrometer.Coefficients;
import com.oceanoptics.spectrometer.Embed2000Plus;

import java.io.IOException;


public class AboutActivity extends Activity {
    private static final String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final TextView message = (TextView)findViewById(R.id.aboutMessage);
        final Embed2000Plus embed = MainActivity.embed;

        Log.d(TAG, "onCreate: embed is "+embed);

        Button btnSerialNumber = (Button)findViewById(R.id.btnSerialNum);
        btnSerialNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "ClickOnSerialNum: embed is "+embed);
                if (embed != null) {
                    String serialNumber = embed.getSerialNumber();
                    message.setText(serialNumber);
                    Log.d(TAG, "onClick: serialNumber is "+serialNumber);
                }
            }

        });

        Button btnWavelengthCoeff = (Button)findViewById(R.id.btnWavelengthCoeff);
        btnWavelengthCoeff.setOnClickListener(new View.OnClickListener() {
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
                message.setText(info);
            }
        });

        Button btnNonlinearityCoeff = (Button)findViewById(R.id.btnNonlinearityCoeff);
        btnNonlinearityCoeff.setOnClickListener(new View.OnClickListener() {
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
                message.setText(info);
            }
        });
        ImageButton aboutClose = (ImageButton)findViewById(R.id.aboutClose);
        aboutClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(AboutActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

    }

}
