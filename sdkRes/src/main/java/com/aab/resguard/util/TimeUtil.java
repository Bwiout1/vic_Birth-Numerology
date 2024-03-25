package com.aab.resguard.util;

import androidx.annotation.VisibleForTesting;

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
public class TimeUtil {
    private static long sTime;
    private static long eTime;
    public static void start(){
        sTime = System.currentTimeMillis();
        eTime = 0;
    }

    public static long watch(){
        if(eTime == 0)
            return System.currentTimeMillis() - sTime;
        return eTime - sTime;
    }

    public static long end(){
        eTime = System.currentTimeMillis();
        return watch();
    }
}
