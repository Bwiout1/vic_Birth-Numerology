package com.vt.sdk;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.LogUtil;
import com.Switch;
import com.fyber.FairBid;
import com.fyber.fairbid.ads.ImpressionData;
import com.fyber.fairbid.ads.Interstitial;
import com.fyber.fairbid.ads.interstitial.InterstitialListener;
import com.fyber.fairbid.user.UserInfo;


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FairbidAdapter {
    private final static String TAG = "dt";

    private final AdsMaster adsMaster;
    private final RemoteConfigManager configManager;

    public FairbidAdapter(AdsMaster adsMaster, RemoteConfigManager configManager) {
        this.adsMaster = adsMaster;
        this.configManager = configManager;
    }

    private final AtomicBoolean initOngoing = new AtomicBoolean(false);
    public void init(Activity activity){
        if (FairBid.hasStarted())
            return;

        String appId = configManager.getDtAppId();
        if (TextUtils.isEmpty(appId)) {
            if (Switch.LOG_ON) LogUtil.w(TAG, "init, appId is empty");
            return;
        }

        if (initOngoing.compareAndSet(false, true)) {
            if (Switch.LOG_ON) LogUtil.d(TAG, "int, doing sdk initialization...");

            UserInfo.setGdprConsent(true, activity.getApplicationContext());
            UserInfo.setLgpdConsent(true, activity.getApplicationContext());

            FairBid dtBid = FairBid.configureForAppId(appId);
            if (Switch.LOG_ON){
//                dtBid.enableLogs();
            }
            dtBid
//                    .disableAutoRequesting()
                    .start(activity);

            Interstitial.setInterstitialListener(new InterstitialListener() {
                @Override public void onShow(@NonNull String s, @NonNull ImpressionData impressionData) {
                    if (Switch.LOG_ON) LogUtil.w(TAG, "inters, onShow,"+impressionData.getDemandSource());

                    AdImpCallback callback = impCallbackRef.get();
                    if (callback!=null){
                        ThreadUtils.runInUIThread(()->callback.onShow(ExtAdImpRecord.POP_DT));
                    }
                }

                @Override public void onClick(@NonNull String s) {
                    if (Switch.LOG_ON) LogUtil.d(TAG, "inters, onClick");
                }

                @Override public void onHide(@NonNull String s) {
                    if (Switch.LOG_ON) LogUtil.w(TAG, "inters, onHide");

                    AdImpCallback callback = impCallbackRef.getAndSet(null);
                    if (callback!=null){
                        ThreadUtils.runInUIThread(()->callback.onDismiss(ExtAdImpRecord.POP_DT));
                    }
                }

                @Override public void onShowFailure(@NonNull String plc, @NonNull ImpressionData impressionData) {
                    if (Switch.LOG_ON) LogUtil.w(TAG, "inters, onShowFailure, "+impressionData.getDemandSource());

                    AdImpCallback callback = impCallbackRef.getAndSet(null);
                    if (callback!=null){
                        ThreadUtils.runInUIThread(()-> callback.onShowFail(ExtAdImpRecord.POP_DT+"-"+impressionData.getDemandSource()));
                    }
                }

                @Override public void onAvailable(@NonNull String plc) {
                    if (Switch.LOG_ON) LogUtil.d(TAG, "inters, onAvailable");
                    intersLoadOngoing.set(false);
                }

                @Override public void onUnavailable(@NonNull String plc) {
                    if (Switch.LOG_ON) LogUtil.d(TAG, "inters, onUnavailable");
                    intersLoadOngoing.set(false);
                }

                @Override public void onRequestStart(@NonNull String plc) {
                    if (Switch.LOG_ON) LogUtil.d(TAG, "inters, onRequestStart");
                }
            });

            initOngoing.set(false);
            if (FairBid.hasStarted()){
                ThreadUtils.runInUIThread(adsMaster::onDtSdkReady);
            }
        }
    }

    private final AtomicBoolean intersLoadOngoing = new AtomicBoolean(false);
    public void loadInters(){
        if (!FairBid.hasStarted()) {
            if (Switch.LOG_ON) LogUtil.w(TAG, "loadInters, FairBid.hasStarted()=false");
            return;
        }

        if (hasInters()){
            if (Switch.LOG_ON) LogUtil.d(TAG, "loadInters, has inters cached");
            return;
        }

        String unitId = configManager.getDtIntersId();
        if (TextUtils.isEmpty(unitId)){
            if (Switch.LOG_ON) LogUtil.w(TAG, "loadInters, adUnitId is empty");
            return;
        }

        if (intersLoadOngoing.compareAndSet(false, true)){
            if (Switch.LOG_ON) LogUtil.w(TAG, "loadInters....");
            Interstitial.request(unitId);
        }
    }

    public boolean hasInters(){
        String unitId = configManager.getDtIntersId();
        return !TextUtils.isEmpty(unitId) && Interstitial.isAvailable(unitId);
    }

    private final AtomicReference<AdImpCallback> impCallbackRef = new AtomicReference<>();
    public void showInters(Activity activity, AdImpCallback callback){
        if (activity==null || activity.isFinishing() || activity.isDestroyed())
            return;

        String unitId = configManager.getDtIntersId();
        if (Interstitial.isAvailable(unitId)){
            if (Switch.LOG_ON) LogUtil.d(TAG, "showInters...");

            impCallbackRef.set(callback);
            Interstitial.show(unitId, activity);
        } else {
            adsMaster.loadInters();
        }
    }
}
