package com.vt.sdk;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.LogUtil;
import com.Switch;
import com.flurry.android.FlurryAgent;
import com.flurry.android.FlurryPerformance;
import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.YandexMetricaConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Analytics {
    private final AtomicBoolean appMetricaRdy = new AtomicBoolean();

    public void init(Context context, String flurryKey, String appMetricaKey) {
        Context appContext = context == null ? null : context.getApplicationContext();
        if (appContext == null)
            return;

        if (!TextUtils.isEmpty(flurryKey.trim())) {
            new FlurryAgent.Builder()
                    .withLogEnabled(Switch.LOG_ON)
                    .withLogLevel(Switch.LOG_ON ? Log.VERBOSE : Log.ERROR)
                    .withCaptureUncaughtExceptions(true)
                    .withIncludeBackgroundSessionsInMetrics(true)
                    .withPerformanceMetrics(FlurryPerformance.ALL)
                    .build(appContext, flurryKey.trim());

            if (Switch.LOG_ON) {
                LogUtil.d(getLogTag(), "init flurry, FlurryAgent.isInitialized()=" + FlurryAgent.isInitialized());
            }
        }

        if (!TextUtils.isEmpty(appMetricaKey.trim())) {
            YandexMetricaConfig config = YandexMetricaConfig.newConfigBuilder(appMetricaKey.trim())
                    .withCrashReporting(true)
                    .build();
            YandexMetrica.activate(appContext, config);
            YandexMetrica.enableActivityAutoTracking((Application) appContext);
            appMetricaRdy.set(true);
        }
    }

    public void logEvent(String evt,
                         Pair<String, String>... params) {
        if (TextUtils.isEmpty(evt))
            return;


        if (FlurryAgent.isInitialized()) {
            ThreadUtils.runInBackground(() -> {
                if (params == null || params.length == 0) {
                    if (Switch.LOG_ON) {
                        LogUtil.d(getLogTag(), "logEvent, evt=" + evt);
                    }
                    FlurryAgent.logEvent(evt);
                } else {
                    Map<String, String> data = new HashMap<>();
                    for (Pair<String, String> it : params) {
                        data.put(it.first, it.second);
                    }

                    if (Switch.LOG_ON) {
                        LogUtil.d(getLogTag(), "logEvent, evt=" + evt + ", params=" + data);
                    }
                    FlurryAgent.logEvent(evt, data);
                }
            });
        } else if (Switch.LOG_ON) {
            Log.e(getLogTag(), "logEvent, FlurryAgent.isInitialized()=false, evt=" + evt);
        }

        if (appMetricaRdy.get()) {
            ThreadUtils.runInBackground(() -> {
                if (params == null || params.length == 0) {
                    YandexMetrica.reportEvent(evt);
                } else {
                    Map<String, Object> data = new HashMap<>();
                    for (Pair<String, String> it : params) {
                        data.put(it.first, it.second);
                    }
                    YandexMetrica.reportEvent(evt, data);
                }
            });
        }
    }

    public void logError() {

    }

    private String getLogTag(){
        return "Analytics";
    }
}
