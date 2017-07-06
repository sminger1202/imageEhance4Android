package com.android.enhance;

import android.content.Context;
import android.opengl.GLES20;

/**
 * Created by shiming on 2017/7/6.
 */

public class ReduxEngine extends EngineBase {
    public ReduxEngine(Context context) {
        init(context);
    }
    @Override
    public void setParameter(int field, float value) {

    }

    @Override
    public void setParameters(int field, float[] value) {

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

    @Override
    public void apply(int srcTextureId, int dstTextureId, int width, int height) {

    }

    public float[] getValue(int srcTexture, int channels) {
        float[] value = new float[channels];

        return value;
    }

    @Override
    public void release() {

    }
}
