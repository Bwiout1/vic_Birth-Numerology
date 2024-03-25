package com.vt.sdk;


import android.text.TextUtils;

import com.LogUtil;
import com.Switch;
import com.flurry.android.FlurryConfig;
import com.flurry.android.FlurryConfigListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Random;

import com.sun.dex.core.CryptoUtil;
import com.sun.dex.core.util.HexStrUtil;


public class RemoteConfigManager {
    private final static String TAG = "config";

    private final SdkManager sdkManager;

    public RemoteConfigManager(SdkManager sdkManager) {
        this.sdkManager = sdkManager;
        FlurryConfig.getInstance().registerListener(new FlurryConfigListener() {
            @Override
            public void onFetchSuccess() {
                if (Switch.LOG_ON) {
                    LogUtil.d(TAG, "onFetchSuccess");
                }
                FlurryConfig.getInstance().activateConfig();
            }

            @Override
            public void onFetchNoChange() {
                if (Switch.LOG_ON) {
                    LogUtil.d(TAG, "onFetchNoChange");
                }
            }

            @Override
            public void onFetchError(boolean isRetrying) {
                if (Switch.LOG_ON) {
                    LogUtil.d(TAG, "onFetchError, isRetrying=" + isRetrying);
                }
            }

            @Override
            public void onActivateComplete(boolean isCache) {
                if (Switch.LOG_ON) {
                    LogUtil.d(TAG, "onActivateComplete, isCache=" + isCache);
                }

                decryptConfig();
                parsePangleConfig();
                parseFairbidConfig();

                ThreadUtils.runInUIThread(sdkManager::onRemoteConfigReady);
            }
        });
    }

    public void pullRemoteConfig() {
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "pullRemoteConfig...");
        }
        FlurryConfig.getInstance().fetchConfig();
    }

    private JSONObject configJson;

    private void decryptConfig() {
        String cipher = FlurryConfig.getInstance().getString("_flurry", "").trim();
        if (TextUtils.isEmpty(cipher))
            return;

        try {
            String data = new String(CryptoUtil.decAndRemovePadding(sdkManager.getApp().getPackageName(),
                    HexStrUtil.toByteArray(cipher)));
            if (Switch.LOG_ON) {
                LogUtil.v(TAG, "clear flurry data:" + data);
            }

            if (!TextUtils.isEmpty(data)) {
                configJson = new JSONObject(data);
            }
        } catch (Exception ignore) {
            if (Switch.LOG_ON) {
                ignore.printStackTrace();
            }
        }

    }

    private String pangleId;
    private String pangleIntersId;
    private String pangleNativeId;
    private int pangleTime;

    private void parsePangleConfig() {
        if (configJson != null) {
            String rawConfig = configJson.optString("ads_fallback_pangle", "");
            try {
                JSONObject json = new JSONObject(rawConfig);
                pangleId = json.optString("app", "").trim();
                pangleIntersId = json.optString("int", "").trim();
                pangleNativeId = json.optString("native", "").trim();
                pangleTime = json.optInt("times", 0);
            } catch (JSONException e) {
                if (Switch.LOG_ON) {
                    e.printStackTrace();
                }
            }
            if (Switch.LOG_ON) {
                LogUtil.d(TAG, "parsePangle");
            }
        }
    }

    private String dtId;
    private String dtIntersId;
    private int dtTime;

    private void parseFairbidConfig() {
        if (configJson != null) {
            String rawConfig = configJson.optString("ads_fallback_fairbid", "");
            try {
                JSONObject json = new JSONObject(rawConfig);
                dtId = json.optString("app", "").trim();
                dtIntersId = json.optString("int", "").trim();
                dtTime = json.optInt("times", 0);
            } catch (JSONException e) {
                if (Switch.LOG_ON) {
                    e.printStackTrace();
                }
            }
            if (Switch.LOG_ON) {
                LogUtil.d(TAG, "parseFairbid");
            }
        }
    }


    public boolean sdkEnabled() {
        return configJson != null && configJson.optString("ads_enable", "false")
                .toLowerCase(Locale.ROOT).contains("tr");
    }

    public long getNewUserActivateTimeSeconds() {
        if (Switch.LOG_ON)
            return 50;
        return configJson == null ? 7200L : configJson.optLong("berg_newUserAdDelayT", 7200L);
    }

    public String getPangleAppId() {
        return pangleId == null ? "" : pangleId;
//        return "8025677";
    }

    public String getPangleIntersId() {
        return pangleIntersId == null ? "" : pangleIntersId;
//        return "980088188";
    }

    public String getPangleNativeId() {
        return pangleNativeId == null ? "" : pangleNativeId;
//        return "980088216";
    }

    public int getPangleDailyTime() {
        return pangleTime;
    }

    public String getDtAppId() {
        return dtId == null ? "" : dtId;
//        return "133676";
    }

    public String getDtIntersId() {
        return dtIntersId == null ? "" : dtIntersId;
//        return "693035";
    }

    public int getDtDailyTime() {
        return dtTime;
    }

    public int getWebDailyTime() {
        return configJson == null ? 0 : configJson.optInt("berg_web_times", 0);
    }

    public String getWebOfferUrl() {
        String urls = configJson == null ? "" : configJson.optString("berg_web_url", "");
        if (!TextUtils.isEmpty(urls)) {
            try {
                JSONArray json = new JSONArray(urls);
                int size = json.length();
                if (size > 0) {
                    Random random = new Random();
                    return json.optString(random.nextInt(size));
                }
            } catch (JSONException e) {
                if (Switch.LOG_ON) {
                    e.printStackTrace();
                }
            }
        }
        return "";//https://m.baidu.com";
    }

/*
{
  "ads_enable": "true",
  "ads_fallback_pangle": "{\"times\":\"5\",\"app\":\"8025677\", \"native\":\"980088216\", \"int\":\"980088188\"}",
  "ads_fallback_fairbid": "{\"times\":\"5\",\"app\":\"133676\",\"int\":\"693035\"}",
  "berg_newUserAdDelayT": "10",
  "berg_web_times": "5",
  "berg_web_url": "[\"https://lightningscannerr.xyz/\"]",
}
*/
}
