package com.example.playerdemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.opengl.GLUtils;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;

import static com.example.playerdemo.GLUtil.sIsOpenClPrepared;
import static com.example.playerdemo.GLUtil.sIsOpenGlPrepared;

public class MainActivity extends AppCompatActivity {

    final String TAG = getClass().getSimpleName();
    private static final String APPLICATION_RAW_PATH = "android.resource://com.example.playerdemo/";
    private VideoPreviewView mPreviewView;
    private Button playButton;
    private Button GLButton;
    private Button CLButton;
    private Button copyButton;
    private Switch enhanceSwitch;
    private SeekBar mSeekBar;
    public Context mContxt;
    final float GRAY = 0.3f;
    final float BLACK = 1.f;

    private static final int GRANT_PERMISSIONS_REQUEST_CODE = 101;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContxt = this;
        enhanceSwitch = (Switch) findViewById(R.id.modeToggle);
        enhanceSwitch.setChecked(GLUtil.sIsEnhance);
        enhanceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GLUtil.sIsEnhance = ! GLUtil.sIsEnhance;
                GLUtil.isChanged = true;
                if(GLUtil.sIsEnhance) {
                    Debug.startMethodTracing("/sdcard/traceMethod");
                } else {
                    Debug.stopMethodTracing();
                }
//                TelephonyManager mTm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
//                String imei = mTm.getDeviceId();
//                Log.e(TAG, "Imei: xxxxx:" + imei );
                Log.d(TAG, "toggle Enchance : " + GLUtil.sIsEnhance );
            }
        });
        playButton = (Button)findViewById(R.id.playbotton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "is playing :" + mPreviewView.isPlaying());
                if(mPreviewView.isPlaying()) {
                    mPreviewView.pause();
                } else {
                    mPreviewView.start();
                }
            }
        });
        GLButton = (Button)findViewById(R.id.toggleGL);
        GLButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetState();
                sIsOpenGlPrepared = !sIsOpenGlPrepared;
                if(sIsOpenGlPrepared) {
                    GLButton.setAlpha(BLACK);
                } else {
                    GLButton.setAlpha(GRAY);
                }
                Log.d(TAG, "GL mode :" + sIsOpenGlPrepared );
            }
        });
        CLButton = (Button)findViewById(R.id.toggleCL);
        CLButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetState();
//                sIsOpenClPrepared = initComputerPlatform();
                sIsOpenClPrepared = !sIsOpenClPrepared;
                if(sIsOpenClPrepared) {
                    CLButton.setAlpha(BLACK);
                } else {
                    CLButton.setAlpha(GRAY);
                }
                Log.d(TAG, "CL mode :" + sIsOpenClPrepared );
                Log.d(TAG, "opencl is prepared.");
            }
        });
        copyButton = (Button)findViewById(R.id.copy);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        mPreviewView = (VideoPreviewView)findViewById(R.id.playview);
        mPreviewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onclick" + GLUtil.sWidth + "x" +GLUtil.sHeight);
                WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
                float ratio = (float)( mPreviewView.getHeight() / mPreviewView.getWidth());
                GLUtil.sWidth = wm.getDefaultDisplay().getWidth();
                GLUtil.sHeight = (int)(GLUtil.sWidth * ratio);
                mPreviewView.requestLayout();
                mPreviewView.invalidate();
            }
        });
        mPreviewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        mSeekBar = (SeekBar) findViewById(R.id.progressbar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "progress :" + progress);
                mPreviewView.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void resetState() {
        GLButton.setAlpha(GRAY);
        CLButton.setAlpha(GRAY);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        super.onTouchEvent(event);

        return false;
    }
    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG,"onPause");
        mPreviewView.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");
        mPreviewView.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mPreviewView.stopPlayback();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GRANT_PERMISSIONS_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    showRationaleDialog();

                }
                return;
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Log.d(TAG, "onkeydown :" + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    private void showRationaleDialog() {
        new AlertDialog.Builder(this)
                .setMessage("读文件权限")
                .setCancelable(true)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // CameraActivity.this.finish();
                    }
                }).show();
    }

    /* A native method that is implemented by the
     * 'hello-jni' native library, which is packaged
     * with this application.
     */
    public native String  stringFromJNI();

    /* This is another native method declaration that is *not*
     * implemented by 'hello-jni'. This is simply to show that
     * you can declare as many native methods in your Java code
     * as you want, their implementation is searched in the
     * currently loaded native libraries only the first time
     * you call them.
     *
     * Trying to call this function will result in a
     * java.lang.UnsatisfiedLinkError exception !
     */
    public native String  TestJNI();

    public static native boolean initComputerPlatform();

    public static native void setTextureIds( int srcTextureId, int dstTextureId,
                                             int srcWidth, int srcHeight,
                                             int dstWidht, int dstHeight);

    public static native void renderCL();

    public static native void release();

    public static native int GLGetProgram();

    /* this is used to load the 'hello-jni' library on application
     * startup. The library has already been unpacked into
     * /data/data/com.example.hellojni/lib/libhello-jni.so at
     * installation time by the package manager.
     */
    static {
        System.loadLibrary("EGL");
        System.loadLibrary("GLESv2");
//        System.loadLibrary("OpenCL");
//        System.loadLibrary("hello-jni");
    }
}
