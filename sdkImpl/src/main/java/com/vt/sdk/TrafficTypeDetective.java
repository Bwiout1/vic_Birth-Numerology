package com.vt.sdk;


import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Pair;

import com.LogUtil;
import com.Switch;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;

import java.util.Locale;


public class TrafficTypeDetective {
    private final static String TAG = "referrer";

    private final SharedPreferences storage;
    private final SdkManager sdkManager;
    private final Object attr;
    public TrafficTypeDetective(SdkManager sdkManager, Object attr){
        this.sdkManager = sdkManager;
        this.storage = sdkManager.getEncryptSP();
        this.attr = attr;
    }

    private final String type_key = "tt_paid";
    public void detect(){
        if (storage.contains(type_key)){
            ThreadUtils.runInUIThread(sdkManager::onTrafficTypeReady);
            return;
        }

        ThreadUtils.runInBackground(()->{
            InstallReferrerClient client = InstallReferrerClient.newBuilder(sdkManager.getApp()).build();
            client.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    if (Switch.LOG_ON){
                        LogUtil.d(TAG, "detect, onInstallReferrerSetupFinished, responseCode="+ responseCode);
                    }

                    try {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            String referrer = client.getInstallReferrer().getInstallReferrer();

                            if (Switch.LOG_ON){
                                LogUtil.d(TAG, "detect, onInstallReferrerSetupFinished, referrer="+ referrer);
                            }

                            parseGPReferrer(referrer);

                            ThreadUtils.runInUIThread(sdkManager::onTrafficTypeReady);

                           sdkManager.getAnalytics().logEvent("binstall",
                                   Pair.create("ref", TextUtils.isEmpty(referrer) ? "" : referrer),
                                   Pair.create("dev", sdkManager.getDeviceMetaData()),
                                   Pair.create("attr", getAttributionData()),
                                   Pair.create("isDebug", String.valueOf(BuildConfig.DEBUG))
                            );
                        }
                    } catch (Exception ignored) {
                        if(Switch.LOG_ON){
                            ignored.printStackTrace();
                        }
                    } finally {
                        client.endConnection();
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    if (Switch.LOG_ON) {
                        LogUtil.d(TAG, "detect, onInstallReferrerSetupFinished, onInstallReferrerServiceDisconnected");
                    }
                }
            });
        });
    }


    private void parseGPReferrer(String referrer){
        boolean isPaidUser = false;

        do {
            if (TextUtils.isEmpty(referrer))
                break;

            isPaidUser = !referrer.toLowerCase(Locale.ENGLISH).contains("organic");
        } while (false);

        storage.edit()
                .putBoolean(type_key, isPaidUser)
                .apply();
    }


    private String getAttributionData(){
        if (attr==null)
            return "null";

        return attr.toString();
    }

    public boolean isPaidUser(){
        if (Switch.LOG_ON)
            return true;

        if (storage.contains(type_key)){
            return storage.getBoolean(type_key, false);
        }

        return false;
    }
}

