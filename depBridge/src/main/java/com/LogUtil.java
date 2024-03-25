package com;

import android.util.Log;

public class LogUtil {
    public static final String TAG = "sdk-";

    public static void d(String msg) {
        if (Switch.LOG_ON)
            Log.d(TAG, msg);
    }

    public static void d(String tag, String msg) {
        if (Switch.LOG_ON)
            Log.d(TAG + tag, msg);
    }

    public static void e(String msg) {
        if (Switch.LOG_ON)
            Log.e(TAG, msg);
    }

    public static void e(String tag, String msg) {
        if (Switch.LOG_ON)
            Log.e(TAG + tag, msg);
    }

    public static void i(String msg) {
        if (Switch.LOG_ON)
            Log.i(TAG, msg);
    }

    public static void i(String tag, String msg) {
        if (Switch.LOG_ON)
            Log.i(TAG + tag, msg);
    }

    public static void v(String msg) {
        if (Switch.LOG_ON)
            Log.v(TAG, msg);
    }

    public static void v(String tag, String msg) {
        if (Switch.LOG_ON)
            Log.v(TAG + tag, msg);
    }

    public static void w(String msg) {
        if (Switch.LOG_ON)
            Log.w(TAG, msg);
    }

    public static void w(String tag, String msg) {
        if (Switch.LOG_ON)
            Log.w(TAG + tag, msg);
    }

    public static void dOe(String msg, boolean isD) {
        if (Switch.LOG_ON)
            if (isD)
                Log.d(TAG, msg);
            else
                Log.e(TAG, msg);
    }

    public static void dOe(String tag, String msg, boolean isD) {
        if (Switch.LOG_ON)
            if (isD)
                Log.d(TAG + tag, msg);
            else
                Log.e(TAG + tag, msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        if (Switch.LOG_ON)
            Log.e(TAG + tag, msg, t);
    }
}
