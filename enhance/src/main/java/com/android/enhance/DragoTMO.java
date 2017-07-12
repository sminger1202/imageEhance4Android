package com.android.enhance;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_TEXTURE_2D;

/**
 * Created by shiming on 2017/7/4.
 */

public class DragoTMO extends EngineBase {
    private float mLdMax = 100.0f;
    private float mBias = 0.95f;
    private float mLwMax = 1.f;
    private float mLwa = 0.4f;

    private float Lw_a_scaled;
    private float Lw_Max_scaled;
    private float constant1;
    private float constant2;

    private float[] mMaxMin;
    private IEngine mLuminance;
    private IEngine mDrago;
    private ReduxEngine mRedux;
    private int mLumTextureId;
    float[] floatData;
    FloatBuffer floatBuffer;
    byte[] byteData;
    ByteBuffer byteBuffer;
    DragoTMO(Context context) {
        init(context);
    }
    @Override
    public void init(Context context){
        mContext   = context;
        mLuminance = CVFactory.getEngineInstance(mContext, CVFactory.LUMINANCE);
        mDrago     = CVFactory.getEngineInstance(mContext, CVFactory.DRAGO);
        mRedux     = (ReduxEngine) CVFactory.getEngineInstance(mContext, CVFactory.REDUX);
        mLumTextureId = createInnerTextureObject();
        mMaxMin = new float[2];
    }
    @Override
    public void setParameter(int field, float value) {

    }

    @Override
    public void setParameters(int field, float[] value) {
        if (value.length == 2) {
            mLdMax = value[0];
            mBias  = value[1];
        }
    }

    @Override
    public void apply(int[] srcTextureId, int dstTextureId, int width, int height) {

    }

    @Override
    protected void localAttri() {

    }

    @Override
    protected void initFBO() {

    }

    @Override
    protected void initVBO() {

    }

    private void updata(float Ld_Max,
                        float b,
                        float Lw_Max,
                        float Lw_a) {
        //protected values are assigned/computed
        if(Ld_Max > 0.0f) {
            mLdMax = Ld_Max;
        } else {
            mLdMax = 100.0f;
        }

        if(b > 0.0f) {
            mBias = b;
        } else {
            mBias = 0.95f;
        }

        if(Lw_Max > 0.0f) {
            mLwMax = Lw_Max;
        } else {
            mLwMax = 1e6f;
        }

        if(Lw_a > 0.0f) {
            mLwa = Lw_a;
        } else {
            mLwa = 0.5f;
        }

        //constants
        Lw_a_scaled  = mLwa / (float) (Math.pow(1.0f + mBias - 0.85f, 5.0f));
        Lw_Max_scaled = mLwMax / Lw_a_scaled;

        constant1 = (float) (Math.log(b) / Math.log(0.5f));
        constant2 = (mLdMax / 100.0f) / ((float) Math.log10(1.0f + Lw_Max_scaled));
    }
    private void getMaxMin(int textureID, float[] value) {
        saveGLState();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, textureID);
        byteBuffer.position(0);
        GLES20.glReadPixels(0, 0, mWidth, mHeight ,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        byteBuffer.position(0);
        byteBuffer.get(byteData, 0, mWidth * mHeight * 4);

        int Max = 0, Min = 255;
        int Value;
        for (int i = 0; i < mWidth * mHeight * 4; i += 4) {
            Value =  byteData[i] & 0xff;
            if (Value > Max) {
                Max = Value;
            }
            if (Value < Min) {
                Min = Value;
            }
            value[0] = Max / 255.f;
            value[1] = (Min + 1) / 256.f;
//            Log.d(TAG, "byte data[" + i + "] : " + byteData[i]);
        }
        Log.d(TAG, "MaxMinValue max, min :" + Max + "," + Min + "," + value[0]+ "," + value[1]);
        restoreState();
    }
    private void getMaxMinGL(int textureID, float[] value) {
        float[] tmp = mRedux.getMinValue(textureID, mWidth, mHeight, 4);
        value[0] = tmp[3];
        value[1] = tmp[3];
    }
    @Override
    public void apply(int srcTextureId, int dstTextureId, int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;

//            byteBuffer = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
//            byteData = new byte[mWidth * mHeight * 4];
//
//            floatBuffer = FloatBuffer.allocate(mWidth * mHeight * 4);
//            floatData = new float[mWidth * mHeight * 4];
        }
        mLuminance.apply(srcTextureId, mLumTextureId, width, height);
//        getMaxMin(mLumTextureId, mMaxMin);
        getMaxMinGL(mLumTextureId, mMaxMin);
        int[] InputTextures = {srcTextureId, mLumTextureId};
        updata(mLdMax, mBias, mMaxMin[0], mMaxMin[0] ) ;
        float[] pars = { constant1, constant2, Lw_Max_scaled, Lw_a_scaled};
        mDrago.setParameters(0, pars);
        mDrago.apply(InputTextures, dstTextureId, width, height);

    }
    @Override
    public void release() {
        int[] textures = new int[1];
        textures[0] = mLumTextureId;
        GLES20.glDeleteTextures(1, textures, 0);
    }
}
