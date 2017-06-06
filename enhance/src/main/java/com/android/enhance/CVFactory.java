package com.android.enhance;

/**
 * Created by shiming on 2017/5/25.
 */

public class CVFactory {
    public static String ENHANCE = "enhance";
    public static Engine mEngine;
    public static synchronized Engine getEngineInstance(String EngineType) {
        try {
            if (EngineType == ENHANCE && mEngine == null) {
                mEngine = EnhanceEngine.class.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mEngine;
    }
}
