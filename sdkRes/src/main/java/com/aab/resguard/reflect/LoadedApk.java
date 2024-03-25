package com.aab.resguard.reflect;

import android.content.pm.ApplicationInfo;
import android.content.res.Resources;

import com.aab.resguard.AppExtResManager;
import com.aab.resguard.ResourcesHookerUtil;
import com.aab.resguard.util.AabResGuardDbgSwitch;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class LoadedApk {
    private static final Class<?> clzLoadedApk;

    static {
        Class<?> clz;
        try {
            clz = Class.forName("android.app.LoadedApk");
        } catch (ClassNotFoundException e) {
            clz = null;

            if (AabResGuardDbgSwitch.LOG_ENABLE) e.printStackTrace();
        }
        clzLoadedApk = clz;
    }

    private static final Map<String, Field> fieldMap = new HashMap<>();

    private final Object core;
    private final Reflect reflect;

    public LoadedApk(String pkgName) {
//        ActivityThread sCurrentActivityThread;
        Object sCurrentActivityThread = Reflect.onClass("android.app.ActivityThread").get("sCurrentActivityThread");

//         ArrayMap<String, WeakReference<LoadedApk>> mPackages
        Map<String, WeakReference<Object>> mPackages = Reflect.on(sCurrentActivityThread).get("mPackages");

        core = mPackages.get(pkgName).get();
        reflect = core == null ? null : Reflect.on(core);

    }


    private <T> T getField(String name) {
        return reflect == null ? null : reflect.get(name);
    }

    private <T> void setField(String name, T val) {
        if (reflect != null) {
            reflect.set(name, val);
        }
    }


    public void hook() {
        if (core == null)
            return;

        //1.
        Resources res = getField("mResources");
        ResourcesHookerUtil.addAssetPath(res, true);

        //2
        String[] mSplitResDirs = getField("mSplitResDirs");
        boolean hasAppended = false;
        if (mSplitResDirs != null) {
            for (String resDir : mSplitResDirs) {
                if (resDir != null && resDir.equals(AppExtResManager.getInstance().getDecodedResPath())) {
                    hasAppended = true;
                    break;
                }
            }
        }
        if (!hasAppended) {
            if (mSplitResDirs == null) {
                mSplitResDirs = new String[]{AppExtResManager.getInstance().getDecodedResPath()};
            } else {
                String[] tmp = new String[mSplitResDirs.length + 1];
                System.arraycopy(mSplitResDirs, 0, tmp, 0, mSplitResDirs.length);
                tmp[tmp.length - 1] = AppExtResManager.getInstance().getDecodedResPath();
                mSplitResDirs = tmp;
            }
            setField("mSplitResDirs", mSplitResDirs);
        }

        //3
        ApplicationInfo appInfo = getField("mApplicationInfo");
        AppExtResManager.getInstance().hookAppInfo(appInfo);
    }
}
