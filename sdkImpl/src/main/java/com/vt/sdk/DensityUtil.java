package com.vt.sdk;

import android.content.res.Resources;

public class DensityUtil {

    private final static float density= Resources.getSystem().getDisplayMetrics().density;

    public static int dp2px(float dpValue) {
        return (int) (0.5f + dpValue * density);
    }
}
