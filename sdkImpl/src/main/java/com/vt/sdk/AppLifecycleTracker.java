package com.vt.sdk;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.LogUtil;
import com.Switch;

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AppLifecycleTracker {
    private final AdsMaster adsMaster;

    public AppLifecycleTracker(SdkManager sdkManager) {
        adsMaster = sdkManager.getAdsMaster();
    }

    public void track(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
                ThreadUtils.runInUIThread(() -> {
                    if (Switch.LOG_ON)
                        LogUtil.d(getLogTag(), "onActivityCreated, act=" + activity);

                    adsMaster.init(activity);

                    int actType = classifyActivity(activity);
                    if (actType != ACT_TYPE_ADN) {
                        adsMaster.loadInters();
                        adsMaster.loadInner(false);
                    }
                    if (actType != ACT_TYPE_NORMAL) {
                        shadow(activity);
                    }
                });
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (activityNum.incrementAndGet() < 1) {
                    //todo logerr
//                    System.exit(0);
                }

                ThreadUtils.runInUIThread(() -> {
                    if (Switch.LOG_ON)
                        LogUtil.d(getLogTag(), "onActivityStarted, act=" + activity);
                    int actType = classifyActivity(activity);
                    if (Switch.LOG_ON)
                        LogUtil.d(getLogTag(), "onActivityStarted, lastActType=" + lastActType + ", act=" + activity + ", type=" + actType);

                    int actHash = activity.hashCode();

                    //normalAct_a ---> normalAct_b ---> adnAct -> normalAct_b
                    if (lastActType.get() == ACT_TYPE_NORMAL && actType == ACT_TYPE_NORMAL
                            && actHash != lastActHash.get()) {
                        adsMaster.showInters(activity);
                    }
                    lastActHash.set(actHash);
                    lastActType.set(actType);

                    if (actType == ACT_TYPE_NORMAL) {
                        ViewGroup container = catchInnerAdContainer(activity);
                        adsMaster.addInner(activity, container);
                    }
                });
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (Switch.LOG_ON)
                    LogUtil.d(getLogTag(), "onActivityPaused, act=" + activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                if (Switch.LOG_ON)
                    LogUtil.d(getLogTag(), "onActivityPaused, act=" + activity);
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (Switch.LOG_ON)
                    LogUtil.d(getLogTag(), "onActivityStopped, act=" + activity);
                if (activityNum.decrementAndGet() < 0) {
                    //todo logerr
//                    System.exit(0);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (Switch.LOG_ON)
                    LogUtil.d(getLogTag(), "onActivityDestroyed, act=" + activity);
            }
        });
    }

    private AtomicInteger activityNum = new AtomicInteger(0);

    public boolean isAppForeground() {
        return activityNum.get() > 0;
    }


    private AtomicInteger lastActType = new AtomicInteger(-1);
    private AtomicInteger lastActHash = new AtomicInteger(-1);


    private final static int ACT_TYPE_NORMAL = 0;
    private final static int ACT_TYPE_POPUP = 1;
    private final static int ACT_TYPE_ADN = 2;

    private int classifyActivity(Activity activity) {
        int ret = ACT_TYPE_NORMAL;

        if (activity instanceof PopupCoreActivity) {
            ret = ACT_TYPE_POPUP;
        } else {
            String clsName = activity.getClass().getName();
            if (clsName.startsWith("com.bytedance")
                    || clsName.startsWith("com.fyber")
                    || clsName.startsWith("com.tapjoy")
                    || clsName.startsWith("com.mbridge")) {
                ret = ACT_TYPE_ADN;
            }
        }

        return ret;
    }


    private final WeakHashMap<Activity, ViewGroup> act2AdContainerMap = new WeakHashMap<>();

    private ViewGroup catchInnerAdContainer(Activity activity) {
        if (act2AdContainerMap.containsKey(activity)) {
            return act2AdContainerMap.get(activity);
        }

        View decorView = activity.getWindow().getDecorView();
        View container = decorView.findViewWithTag("small");

        if (container == null) {
            container = decorView.findViewWithTag("big");
        }

        if (container != null) {
            act2AdContainerMap.put(activity, (ViewGroup) container);
        }

        return (ViewGroup) container;
    }

    /**
     * 隐藏RecentList中图标
     * 广告页和外展页
     *
     * @param activity
     */
    private void shadow(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        try {
            ActivityManager.TaskDescription description;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {//android 13
                description = new ActivityManager.TaskDescription.Builder()
                        .setLabel(" ")
//                        .setIcon()
                        .build();
            } else {
                Bitmap bitmap = Bitmap.createBitmap(66, 66, Bitmap.Config.ARGB_8888);
                ColorDrawable drawable = new ColorDrawable(0x00000000);
                drawable.setBounds(0, 0, 66, 66);
                drawable.draw(new Canvas(bitmap));
                description = new ActivityManager.TaskDescription(" ", bitmap);
            }
            activity.setTaskDescription(description);
            if (Switch.LOG_ON) {
                LogUtil.d(getLogTag(), "activity shadow");
            }
        } catch (Exception ignored) {
        }
    }

    private String getLogTag(){
        return "lifecycle";
    }
}
