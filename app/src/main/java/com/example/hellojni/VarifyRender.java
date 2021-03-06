package com.example.hellojni;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import com.android.enhance.CVFactory;
import com.android.enhance.IEngine;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by shiming on 2017/5/18.
 */

public class VarifyRender implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener{
    private String TAG = this.getClass().getSimpleName();
    private Context mContext;
    private int mTextureId;
    private int mTextureIdEnhance;
    private int mFrameBufferObj;
    private SurfaceTexture mSurfaceTexture;
    private GLSurfaceView mGLSurfaceView;
    public static int mProgram;
    public static int mProgramEnhance;
    private MediaPlayer mPlayer;
    public boolean isReady = false;
    private float[] transform = new float[16];
    private long num = 0;
    static IEngine mEnhanceEngine;


    public VarifyRender(Context context, GLSurfaceView glSurfaceView){
        mContext = context;
        mGLSurfaceView = glSurfaceView;
    }
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        GLUtil.initDrawable();
        mProgram = GLUtil.createProgram(GLUtil.VERTEX_SHADER, GLUtil.FRAGMENT_SHADER_EXT);
        mProgramEnhance = GLUtil.createProgram(GLUtil.VERTEX_SHADER, GLUtil.FRAGMENT_SHADER_EXT_ENH);

        mEnhanceEngine = CVFactory.getEngineInstance(mContext, CVFactory.ENHANCE);

        GLUtil.localAttriAndOthers(mProgram);
        GLUtil.localAttriAndOthersEhn(mProgramEnhance);
        mTextureId = GLUtil.createExtTextureObject();
        mTextureIdEnhance = GLUtil.createInerTextureObject();
        Log.i(TAG, "mTexid :" + mTextureId + " mTidEn" +   mTextureIdEnhance);
        //initComputerPlatform();
        mFrameBufferObj = GLUtil.createFBO();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        startPreview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLUtil.isChanged = true;
//        GLUtil.sHeight = height;
//        GLUtil.sWidth = width;
        if(GLUtil.mData != null ){
            GLUtil.mData.clear();
        }
        GLUtil.mData = ByteBuffer.allocateDirect(GLUtil.sWidth * GLUtil.sWidth * 4);
        mSurfaceTexture.setDefaultBufferSize(width, height);
        Log.d(TAG, "Surface size: " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (isReady) {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(this.transform);
            /**
             * Issues the draw call.  Does the full setup on every call.
             *
             * @param mvpMatrix       The 4x4 projection matrix.
             * @param vertexBuffer    Buffer with vertex position data.
             * @param firstVertex     Index of first vertex to use in vertexBuffer.
             * @param vertexCount     Number of vertices in vertexBuffer.
             * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
             * @param vertexStride    Width, in bytes, of the position data for each vertex (often
             *                        vertexCount * sizeof(float)).
             * @param texMatrix       A 4x4 transformation matrix for texture coords.  (Primarily intended
             *                        for use with SurfaceTexture.)
             * @param texBuffer       Buffer with vertex texture data.
             * @param texStride       Width, in bytes, of the texture data for each vertex.
             *
             *        static void draw(int programHandle, float[] mvpMatrix,
             *                        FloatBuffer vertexBuffer, int firstVertex, int vertexCount,
             *                        int coordsPerVertex, int vertexStride,
             *                        float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
             */

//            Debug.startMethodTracing("enhance");

            GLUtil.drawWithCopy(mProgramEnhance, GLUtil.IDENTITY_MATRIX, GLUtil.vertexArray, 0, GLUtil.vertexCount,
                        GLUtil.coordsPerVertex, GLUtil.vertexStride, this.transform, GLUtil.texCoordArray,
                        mTextureId, GLUtil.texCoordStride, mTextureIdEnhance, mFrameBufferObj);

            /**
             * work
             */
//
//            if (!GLUtil.sIsEnhance) {
//                GLUtil.draw(mProgram, GLUtil.IDENTITY_MATRIX, GLUtil.vertexArray, 0, GLUtil.vertexCount,
//                        GLUtil.coordsPerVertex, GLUtil.vertexStride, this.transform, GLUtil.texCoordArray,
//                        mTextureId, GLUtil.texCoordStride);
//            } else {
//                GLUtil.draw(mProgramEnhance, GLUtil.IDENTITY_MATRIX, GLUtil.vertexArray, 0, GLUtil.vertexCount,
//                        GLUtil.coordsPerVertex, GLUtil.vertexStride, this.transform, GLUtil.texCoordArray,
//                        mTextureId, GLUtil.texCoordStride);
//            }

//            Debug.stopMethodTracing();
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Log.d(TAG, "onFrameAvailable :" + num++);
        mGLSurfaceView.requestRender();

    }

    public void setGLSurfaceView(GLSurfaceView glSurfaceView) {
        glSurfaceView.setRenderer(this);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void startPreview() {
        try {
            mPlayer.setDataSource("/sdcard/testfile.mp4");
//            mPlayer.setDataSource("/sdcard/2014国际足联球迷庆典.mp4");
            mPlayer.setLooping(false);
            mPlayer.prepare();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.start();
                }
            });
            mPlayer.setSurface(new Surface(this.mSurfaceTexture));
            Log.d(TAG, "Video starting playback at: " + mPlayer.getVideoWidth() + "x" + mPlayer.getVideoHeight());
        } catch (IOException e) {
            throw new RuntimeException("Could not open input video!", e);
        }
    }

    public void setMediaPlayer(MediaPlayer mp) {
        mPlayer = mp;
    }
}
