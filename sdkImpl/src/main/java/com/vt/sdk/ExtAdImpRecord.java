package com.vt.sdk;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.LogUtil;
import com.Switch;

import java.util.Calendar;

public class ExtAdImpRecord {

    private static final String TAG = "record";
    private static final String UPDATE_TIME_KEY = "updateTime";
    private static final int MILLIS_OF_DAY = 86400000;
    public static final String POP_PANGLE = "pangle";
    public static final String POP_DT = "dt";
    public static final String POP_WEB = "web";

    private final SdkManager sdkManager;
    private final SharedPreferences storage;

    public ExtAdImpRecord(SdkManager sdkManager) {
        this.sdkManager = sdkManager;
        storage = sdkManager.getEncryptSP();
    }

    public boolean hasPangleQuota() {
        int maxTimes = sdkManager.getConfigManager().getPangleDailyTime();
        int usedTimes = getUsedTimes(POP_PANGLE);
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "hasPangleQuota, uesd/limit:" + usedTimes + "/" + maxTimes);
        }
        return usedTimes < maxTimes;
    }

    public boolean hasDtQuota() {
        int maxTimes = sdkManager.getConfigManager().getDtDailyTime();
        int usedTimes = getUsedTimes(POP_DT);
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "hasDtQuota, uesd/limit:" + usedTimes + "/" + maxTimes);
        }
        return usedTimes < maxTimes;
    }

    public boolean hasWebOfferQuota() {
        int maxTimes = sdkManager.getConfigManager().getWebDailyTime();
        int usedTimes = getUsedTimes(POP_WEB);
        if (Switch.LOG_ON) {
            LogUtil.d(TAG, "hasWebOfferQuota, uesd/limit:" + usedTimes + "/" + maxTimes);
        }
        return usedTimes < maxTimes;
    }

    boolean hasAdsQuota() {
        return hasPangleQuota() || hasDtQuota();
    }

    public void updateUsedTimes(String type) {
        if (TextUtils.isEmpty(type)) {
            if (Switch.LOG_ON) {
                LogUtil.e(TAG, "update times:unknown type");
            }
            return;
        }
        if (isToday()) {
            String key = "pupop_" + type;
            int num = storage.getInt(key, 0) + 1;
            storage.edit().putInt(key, num).apply();
            if (Switch.LOG_ON) {
                LogUtil.d(TAG, "update " + type + " times:" + num);
            }
        } else {
            storage.edit().putInt("pupop_" + POP_PANGLE, 0)
                    .putInt("pupop_" + POP_DT, 0)
                    .putInt("pupop_" + POP_WEB, 0)
                    .putLong(UPDATE_TIME_KEY, System.currentTimeMillis())
                    .apply();
            if (Switch.LOG_ON) {
                LogUtil.d(TAG, "reset time");
            }
        }
    }

    private int getUsedTimes(String popupType) {
        return isToday() ? storage.getInt("pupop_" + popupType, 0) : 0;
    }

    private boolean isToday() {
        long lastTime = storage.getLong(UPDATE_TIME_KEY, 0);
        long wee = getWeeOfToday();
        return lastTime >= wee && lastTime < wee + MILLIS_OF_DAY;
    }

    private long getWeeOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
