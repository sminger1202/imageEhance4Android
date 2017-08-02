package com.example.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;


/**
 * Created by shiming on 2017/7/26.
 * E-mail :sminger1202@gmail.com
 */

public class WXAccessibilityService extends AccessibilityService {
    String TAG = this.getClass().getSimpleName();
    int currentpage;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        Log.i("demo", Integer.toString(eventType));

        String className;
        AccessibilityNodeInfo nodeInfo;
        List<AccessibilityNodeInfo> list;

        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                    }
                }

                //break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                className = event.getClassName().toString();
                nodeInfo = getRootInActiveWindow();
                if (nodeInfo == null) {
                    return;
                }
                list = nodeInfo.findAccessibilityNodeInfosByText("CLICK");
                if(list != null ) {
                    if (list.isEmpty()) {
                        Log.i("click", "click 列表为空");

                        AccessibilityNodeInfo node = findNodeInfosByText(nodeInfo, "CLICK");
                        if (node != null) {
                            performClick(node);
                        }
                    }
                }

                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:

                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.i(TAG, "I am in view clicked.");
                className = event.getClassName().toString();
                nodeInfo = getRootInActiveWindow();
                if (nodeInfo == null) {
                    Log.i(TAG, "noteinfo is null");
                    return;
                }
                list = nodeInfo.findAccessibilityNodeInfosByText("CLICK");
                if(list != null ) {
                    if (list.isEmpty()) {
                        Log.i("click", "click 列表为空");

                        AccessibilityNodeInfo node = findNodeInfosByText(nodeInfo, "CLICK");
                        if (node != null) {
                            performClick(node);
                        }
                    } else {
                        Log.i("click", "click 列表不为空");
                        AccessibilityNodeInfo node = findNodeInfosByText(nodeInfo, "CLICK");
                        if (node != null) {
                            Log.i(TAG, "node is not null");
                            performClick(node);
                        } else {
                            Log.i(TAG, "node is null");
                        }
                    }
                } else {
                    Log.i(TAG, "list is empty");
                }
                break;

        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "onInterrupt");
    }

    @Override
    protected void onServiceConnected() {

        Log.e(TAG, "onServiceConnected");
    }

    //通过文本查找节点
    public AccessibilityNodeInfo findNodeInfosByText(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
        if(list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public void performClick(AccessibilityNodeInfo nodeInfo) {
        if(nodeInfo == null) {
            return;
        }
        if(nodeInfo.isClickable()) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            performClick(nodeInfo.getParent());
        }
    }

}
