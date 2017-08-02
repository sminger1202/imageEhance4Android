package com.example.showimg;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.File;
import java.io.IOException;

import static android.view.Window.FEATURE_NO_TITLE;

public class MainActivity extends AppCompatActivity {
    int drawableIdx = 0;
    ImageView imageView;
    TextView textView;
    int res[] = {
            R.drawable.chartlr,
            R.drawable.cirimg2,
            R.drawable.dot_right,
            R.drawable.lineimg
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        imageView = (ImageView)findViewById(R.id.img);
        textView = (TextView)findViewById(R.id.imei);

        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        textView.setText("imei:"+imei);
        final Message msg = new Message();

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // 要做的事情
                drawableIdx = drawableIdx % res.length;
                imageView.setImageResource(res[drawableIdx]);
                drawableIdx++;
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message msg = new Message();
                    msg.arg1 = 1;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }
}
