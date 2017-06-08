package com.android.enhance;

/**
 * Created by shiming on 2017/6/7.
 */

public interface IEngine {
    void setParameter(String field , float value);
    void apply(int srcTextureId, int dstTextureId, int width, int height);
    void release();
}
