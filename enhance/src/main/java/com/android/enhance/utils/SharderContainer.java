package com.android.enhance.utils;

/**
 * Created by shiming on 2017/7/26.
 * E-mail :sminger1202@gmail.com
 */

public class SharderContainer {
    static {
        System.loadLibrary("shader");
    }

    public static native String getVertexShader(String name);
    public static native String getFragmentShader(String name);

}
