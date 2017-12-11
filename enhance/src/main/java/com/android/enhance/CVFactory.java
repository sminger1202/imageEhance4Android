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
    public static String REDUX = "redux";
    public static String VIDEO8K = "video8k";
    public static String[] engineList= {ENHANCE, LUMINANCE, DRAGO, DRAGOTMO, REDUX, VIDEO8K};
    public static IEngine mEngine;
    public static Map<String, IEngine> EngineMap= new TreeMap<String, IEngine>();
    public static synchronized IEngine getEngineInstance(Context context, String EngineType) {
        try {
            if (EngineMap.containsKey(EngineType)) {
                mEngine = EngineMap.get(EngineType);
            } else {
                if (EngineType == ENHANCE) {
                    mEngine = new EnhanceEngine(context);
                }
                if (EngineType == LUMINANCE ) {
                    mEngine = new LuminanceEngine(context);
                }
                if (EngineType == DRAGO ) {
                    mEngine = new DragoEngine(context);
                }
                if (EngineType == DRAGOTMO) {
                    mEngine = new DragoTMO(context);
                }
                if (EngineType == REDUX) {
                    mEngine = new ReduxEngine(context);
                }
                if (EngineType == VIDEO8K) {
                    mEngine = new Video8kEngine(context);
                }
                EngineMap.put(EngineType, mEngine);
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
