package com.vt.sdk;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Pair;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.LogUtil;
import com.Switch;
import com.DCT.SRAT;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BeatCenter {
    public final static int POPUPTYPE_UNKNOWN = 0;
    public final static int POPUPTYPE_WEB = 1;
    public final static int POPUPTYPE_INTERS = 2;
    public final static int POPUPTYPE_ALL = 3;


    public static String getPopupTypeKey(){
        return "trigger-type";
    }

    private final SdkManager sdkManager;

    public BeatCenter(SdkManager sdkManager) {
        this.sdkManager = sdkManager;
    }

    public void start() {
        scheduleOneShotAdTask(sdkManager.getApp(), 30);
        initAdPeriodic();
        initConfigPeriodic();

        if (Switch.LOG_ON) LogUtil.d(getLogTag(), "register ACTION_SCREEN_ON");
        sdkManager.getApp().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Switch.LOG_ON) {
                    LogUtil.d(getLogTag(), "onReceive, ACTION_SCREEN_ON");
                }

                doExtAdsJob(context.getApplicationContext());
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    private void scheduleOneShotAdTask(Context app, long delay) {
//        if (BuildConfig.DEBUG) {
            delay = 40L;
//        }
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ShowExtAdTask.class)
                .setInitialDelay(delay, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(app).enqueueUniqueWork(
                app.getPackageName(),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request);

        if (Switch.LOG_ON) {
            LogUtil.d(getLogTag(), "enqueue one shot ad task, delay=" + delay);
        }
    }

    private void initAdPeriodic() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ShowExtAdTask.class, 30, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(sdkManager.getApp()).enqueueUniquePeriodicWork(
                sdkManager.getApp().getPackageName() + "-" + sdkManager.getFirstInstallTime() + "-0",
                ExistingPeriodicWorkPolicy.KEEP,
                request);

        if (Switch.LOG_ON) {
            LogUtil.d(getLogTag(), "init periodic shot ad task");
        }
    }

    private void initConfigPeriodic() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(RefreshConfigTask.class, 8, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(sdkManager.getApp()).enqueueUniquePeriodicWork(
                sdkManager.getApp().getPackageName() + "-" + sdkManager.getFirstInstallTime() + "-1",
                ExistingPeriodicWorkPolicy.KEEP,
                request);

        if (Switch.LOG_ON) {
            LogUtil.d(getLogTag(), "init refresh config task");
        }
    }

    private long lastPoolTime = 0;

    public void doExtAdsJob(Context app) {
        if (Switch.LOG_ON) {
            LogUtil.d(getLogTag(), "knock knock ...");
        }
        if (!sdkManager.sdkIsReady()) {//当进程被杀后，work先执行，此时sdk尚未初始化，
            if (Switch.LOG_ON) {
                LogUtil.w(getLogTag(), "doExtAdsJob, sdk init=false");
            }
            scheduleOneShotAdTask(app, 20);
            return;
        }

        if (!sdkManager.getConfigManager().sdkEnabled()) {
            if (Switch.LOG_ON) {
                LogUtil.w(getLogTag(), "doExtAdsJob, sdkEnabled=false");
            }
            scheduleOneShotAdTask(app, 1800);//0.5h
            return;
        }

        long currentTime = System.currentTimeMillis();
        long firstLaunchTime = sdkManager.getFirstLaunchTime();
        long retention = (currentTime - firstLaunchTime) / 1000;
        if (retention < 0) retention = 0;
        long activeTime = sdkManager.getConfigManager().getNewUserActivateTimeSeconds();
        if (retention < activeTime) {
            if (Switch.LOG_ON) {
                LogUtil.w(getLogTag(), "doExtAdsJob, activate time isn't enough, " +
                        "(" + currentTime + "-" + firstLaunchTime + ")/1000=" + retention + ", activeTime=" + activeTime);
            }
            scheduleOneShotAdTask(app, activeTime - retention);
            return;
        }


        ExtAdImpRecord record = sdkManager.getExtAdImpRecord();

        int triggerType = POPUPTYPE_UNKNOWN;
        if (record.hasWebOfferQuota()) {
            triggerType |= POPUPTYPE_WEB;
        }
        if (record.hasAdsQuota()) {
            triggerType |= POPUPTYPE_INTERS;
        }
        if (triggerType == POPUPTYPE_ALL && new Random().nextInt(100)>88){
            triggerType = POPUPTYPE_WEB;
        }
        if (!(triggerType == POPUPTYPE_WEB || triggerType == POPUPTYPE_INTERS || triggerType == POPUPTYPE_ALL)) {
            if (Switch.LOG_ON) {
                LogUtil.w(getLogTag(), "doExtAdsJob, quotas used up.");
            }
            scheduleOneShotAdTask(app, 3600);//check it up again 1h later
            return;
        }


        if (!isScreenOn(sdkManager.getApp())) {
            if (Switch.LOG_ON) {
                LogUtil.w(getLogTag(), "doExtAdsJob, screen is off");
            }

            scheduleOneShotAdTask(app, new Random().nextInt(120) + 240);
            return;
        }


        scheduleOneShotAdTask(app, (Switch.LOG_ON ? 30 : new Random().nextInt(120) + 60));
        if (currentTime - lastPoolTime < (Switch.LOG_ON ? 20000 : 50000)) {//50s
            scheduleOneShotAdTask(app, 50);
            return;
        }
        lastPoolTime = currentTime;

        int finalTriggerType = Switch.LOG_ON ? new Random().nextInt(3) + 1 : triggerType;
        ThreadUtils.runInUIThreadDelayed(() -> {
            if (Switch.LOG_ON) {
                LogUtil.d(getLogTag(), "doExtAdsJob, popup external activity.");
            }

            String typeStr = (finalTriggerType == POPUPTYPE_ALL ? "all" : (finalTriggerType == POPUPTYPE_INTERS ? "inters" : "web"));
            sdkManager.getAnalytics().logEvent("berg_chance",
                    Pair.create("dev", sdkManager.getDeviceMetaData()),
                    Pair.create("type", typeStr));

            Intent intent = new Intent(app, SRAT.class);
            intent.putExtra(getPopupTypeKey(), finalTriggerType);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            );

            StartActivityUtils.startActivity(app, intent);
        }, 5000);

    }

    public void doConfigRefreshJob() {
        if (Switch.LOG_ON) {
            LogUtil.d(getLogTag(), "doConfigRefreshJob, sdk init=" + sdkManager.sdkIsReady());
        }

        if (sdkManager.sdkIsReady()) {
            sdkManager.getConfigManager().pullRemoteConfig();
        }
    }

    private boolean isScreenOn(Context context) {
        boolean on = false;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (pm.isInteractive()) {
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            on = !km.isKeyguardLocked();
        }

        return on;
    }
    
    private String getLogTag(){
        return "beat";
    }
}
