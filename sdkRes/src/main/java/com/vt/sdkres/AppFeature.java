package com.vt.sdkres;

import android.app.Application;

import com.vt.sdkres.dex.core.DexLoader;
import com.vt.sdk.SdkManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yb
 * @date 2023/7/13
 * @describe
 */
interface AppFeature {
    AtomicBoolean loaded = new AtomicBoolean(false);

    void init1();
    void init2();

    default void enter(Application application, String key, Object attr) {
        try {
            if (loaded.compareAndSet(false, true)) {
                DexLoader.getInstance().decryptAndLoad(application, key);
                SdkManager.enter(application, attr);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
