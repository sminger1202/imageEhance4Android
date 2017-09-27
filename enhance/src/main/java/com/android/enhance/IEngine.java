package com.android.enhance;

/**
 * Created by shiming on 2017/6/7.
 */

public interface IEngine {
    int EFFECT_COEFFICIENT = 0x0001;
    int EFFECT_MVP         = 0x0002;
    void setParameter(int field , float value);
    void setParameters(int field, float[] value);
    @Deprecated
    void apply(int srcTextureId, int dstTextureId, int width, int height);
    void apply(int[] srcTextureId, int dstTextureId, int width, int height);
    void release();
}
