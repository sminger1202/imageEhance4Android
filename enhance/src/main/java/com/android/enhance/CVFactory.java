package com.android.enhance;

import android.content.Context;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by shiming on 2017/5/25.
 */

public class CVFactory {
    public static String ENHANCE = "enhance";
    public static String LUMINANCE = "luminance";
    public static String DRAGO = "drago";
    public static String DRAGOTMO = "dragoTMO";
    public static String[] engineList= {ENHANCE, LUMINANCE, DRAGO, DRAGOTMO};
    public static IEngine mEngine;
    public static Map<String, IEngine> EngineMap= new TreeMap<String, IEngine>();
    public static synchronized IEngine getEngineInstance(Context context, String EngineType) {
        try {
            if (EngineMap.containsKey(EngineType)) {
                mEngine = EngineMap.get(EngineType);
            } else {
                if (EngineType == ENHANCE) {
                    mEngine = new EnhanceEngine(context);
                    EngineMap.put(ENHANCE, mEngine);
                }
                if (EngineType == LUMINANCE ) {
                    mEngine = new LuminanceEngine(context);
                    EngineMap.put(LUMINANCE, mEngine);
                }
                if (EngineType == DRAGO ) {
                    mEngine = new DragoEngine(context);
                    EngineMap.put(DRAGO, mEngine);
                }
                if (EngineType == DRAGOTMO) {
                    mEngine = new DragoTMO(context);
                    EngineMap.put(DRAGOTMO, mEngine);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mEngine;
    }
    public static synchronized void Destory() {
        for (String type: engineList ) {
            if (EngineMap.containsKey(type)) {
                EngineMap.get(type).release();
                EngineMap.remove(type);
            }

        }
    }
}
