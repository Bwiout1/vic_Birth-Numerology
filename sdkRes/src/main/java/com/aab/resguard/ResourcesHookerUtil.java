package com.aab.resguard;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;

import com.aab.resguard.reflect.Reflect;
import com.aab.resguard.reflect.ResourcesManager;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ResourcesHookerUtil {
    private static final Set<Object> coveredSet = Collections.newSetFromMap(new WeakHashMap<>());
    private static final ResourcesManager resManager = ResourcesManager.getInstance();

    /**
     * @param res
     * @param forcedUpdateConfig, multiple res may map to one single resImpl, dur app launching phase,
     *                            the resImpl may have processed, however, not all of its res haven't been
     *                            updated config. In this case, forcedUpdateConfig = true.
     *                            After launching stage, only the res whose resImpl need update asset path
     *                            can update config. In this case, forcedUpdateConfig = false.
     */
    public static void addAssetPath(Resources res, boolean forcedUpdateConfig){
        if (res!=null && coveredSet.add(res)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Object mResourcesImpl = Reflect.on(res).get("mResourcesImpl");
                if (mResourcesImpl != null && coveredSet.add(mResourcesImpl)) {
                    if (resManager.isApkRes(mResourcesImpl)) {
                        AssetManager assetManager = Reflect.on(mResourcesImpl).call("getAssets").get();
                        int cookie = Reflect.on(assetManager).call("addAssetPath", AppExtResManager.getInstance().getDecodedResPath()).get();

                        forcedUpdateConfig = true;
                    }
                }
            } else {
                AssetManager assetManager = Reflect.on(res).get("mAssets");
                if (assetManager != null && coveredSet.add(assetManager)) {
                    int cookie = Reflect.on(assetManager).call("addAssetPath", AppExtResManager.getInstance().getDecodedResPath()).get();
                    forcedUpdateConfig = true;
                }
            }



            if (forcedUpdateConfig) {
                res.updateConfiguration(res.getConfiguration(), res.getDisplayMetrics());
            }
        }
    }

    public static void addAssetPath(Object resImpl){
        if(resImpl!=null && coveredSet.add(resImpl)){
            AssetManager assetManager = Reflect.on(resImpl).call("getAssets").get();
            Reflect.on(assetManager).call("addAssetPath", AppExtResManager.getInstance().getDecodedResPath()).get();
        }
    }
}


