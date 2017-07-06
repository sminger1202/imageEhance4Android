package com.example.playerdemo;

import android.content.Context;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.MediaController;


/**
 * Created by shiming on 2017/5/17.
 */

public class VideoPreviewView extends GLSurfaceView
        implements MediaPlayer.OnPreparedListener, MediaController.MediaPlayerControl{

    private String TAG = this.getClass().getSimpleName();
    private Context mContext;
    private MediaController mMediaController;
    private MediaPlayer mMediaPlayer;
    private VarifyRender mVarifyRender;
    private boolean isMediaPlayerReady;
    private long mDurationTime;
    private boolean mMediaControllerAttached = false;

    public VideoPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "VideoPreviewView construct2.");
        mContext = context;
        setEGLContextClientVersion(2);
        mVarifyRender = new VarifyRender(this.getContext(), this);
        mVarifyRender.setGLSurfaceView(this);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setScreenOnWhilePlaying(true);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                Log.d(TAG, "video size change :" + width + "x" + height);
            }
        });
        mVarifyRender.setMediaPlayer( mMediaPlayer );
    }

    public void setMediaController(MediaController controller) {
        if (controller != null) {
            controller.hide();
        }
        mMediaController = controller;
    }

    @Override
    public void onMeasure(int w, int h) {
        setMeasuredDimension(GLUtil.sWidth, GLUtil.sHeight);
    }
    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void seekTo(int pos) {

        mMediaPlayer.seekTo((int)mDurationTime * pos / 100);
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void onResume(){
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMediaPlayer.pause();
    }
    public void stopPlayback() {
        Log.d(TAG, "stopPlayback");

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        isMediaPlayerReady = false;
    }

    public boolean isMediaPlayerReady() {
        return isMediaPlayerReady;
    }
    public void onFrameAvailable(){
        this.requestRender();
    }



    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        Log.d(TAG, "onTouchEvent");
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        Point size = new Point();
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(size);
        float devRatio = ((float) (size.y)) / size.x;
        float mpRatio = ((float)mp.getVideoHeight()) / mp.getVideoWidth();
        if (devRatio > mpRatio) {
            GLUtil.sWidth = size.x;
            GLUtil.sHeight = ( int )(size.x * mpRatio);
        } else {
            GLUtil.sHeight = size.y;
            GLUtil.sWidth = (int )(size.y / mpRatio);
        }
        requestLayout();
        invalidate();
        mVarifyRender.isReady = true;
        mp.start();
        isMediaPlayerReady = true;

        mDurationTime = mMediaPlayer.getDuration();
        Log.d(TAG, "onPrepared  dev" + size.x +"x"+ size.y);
        Log.d(TAG, "onPrepared  mp" + mp.getVideoWidth() +"x"+ mp.getVideoHeight());
        Log.d(TAG, "onPrepared  final" + GLUtil.sWidth +"x"+ GLUtil.sHeight);
    }
}