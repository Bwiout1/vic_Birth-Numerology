package com.vt.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.LogUtil;
import com.Switch;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PopupCoreActivity extends Activity {

    SdkManager sdkManager;
    AdsMaster adsMaster;
    Analytics analytics;
    ExtAdImpRecord record;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sdkManager = SdkManager.sdkManager.get();
        analytics = sdkManager.getAnalytics();

        if (!validatePopupType()) {
            finish();
            return;
        }

        adsMaster = sdkManager.getAdsMaster();
        record = sdkManager.getExtAdImpRecord();
        if (popupType == BeatCenter.POPUPTYPE_WEB || popupType == BeatCenter.POPUPTYPE_ALL) {
            doInnerStuff();
        } else {
            onInnerFinished();
        }
    }

    private int popupType = BeatCenter.POPUPTYPE_UNKNOWN;

    private boolean validatePopupType() {
        boolean ret;
        popupType = getIntent().getIntExtra(BeatCenter.getPopupTypeKey(), BeatCenter.POPUPTYPE_UNKNOWN);

        String typeStr;
        if (popupType == BeatCenter.POPUPTYPE_WEB) {
            ret = true;
            typeStr = "web";
        } else if (popupType == BeatCenter.POPUPTYPE_INTERS) {
            ret = true;
            typeStr = "inters";
        } else if (popupType == BeatCenter.POPUPTYPE_ALL) {
            ret = true;
            typeStr = "all";
        } else {
            ret = false;
            typeStr = "unknown";
        }

        if (Switch.LOG_ON)
            LogUtil.d(getLogTag(), "validatePopupType, type=" + typeStr);

        analytics.logEvent("berg_popup_act",
                Pair.create("dev", sdkManager.getDeviceMetaData()),
                Pair.create("type", typeStr));

        return ret;
    }

    public void onInnerFinished() {
        if (Switch.LOG_ON) {
            LogUtil.d(getLogTag(), "onEvent, EBEvents.P_INNER_FINISHED");
        }


        if (popupType == BeatCenter.POPUPTYPE_INTERS || popupType == BeatCenter.POPUPTYPE_ALL) {
            doIntersStuff();

        } else if (innerAdsShown) {
            ImageView xBtn = new ImageView(this);
            xBtn.setImageResource(com.fyber.inneractive.sdk.R.drawable.ia_ib_close);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    DensityUtil.dp2px(20), DensityUtil.dp2px(20));
            ViewGroup container = findViewById(containerId);
            int gravity = ((FrameLayout.LayoutParams) container.getLayoutParams()).gravity;
            if (gravity == Gravity.BOTTOM) {
                params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
                params.bottomMargin = container.getHeight() + DensityUtil.dp2px(20);
            } else if (gravity == Gravity.CENTER_VERTICAL) {
                params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                params.topMargin = -((container.getHeight()) / 2) - DensityUtil.dp2px(10);
            } else {
                params.gravity = Gravity.RIGHT | Gravity.TOP;
            }
            addContentView(xBtn, params);

            xBtn.setOnClickListener((v)->{
                if(adsMaster.hasInters() && new Random().nextInt(100)<30){//即使本次不该展示inters，关闭native后，仍然有30%机会展示inters
                    adsMaster.showInters(this, new AdImpCallback() {
                        @Override
                        public void onShow(String network) {
                            if (Switch.LOG_ON)
                                LogUtil.d(getLogTag(), "onInnerFinished, onShow");

                            analytics.logEvent("berg_inters",
                                    Pair.create("succ_extra", network + " ｜ " + (popupType == BeatCenter.POPUPTYPE_INTERS ? "inters" : "all")));

                            String popType;
                            if (network.equals("pgl")) {
                                popType = ExtAdImpRecord.POP_PANGLE;
                            } else if (network.equals(ExtAdImpRecord.POP_DT)) {
                                popType = ExtAdImpRecord.POP_DT;
                            } else {
                                popType = "";
                            }
                            record.updateUsedTimes(popType);
                        }

                        @Override
                        public void onShowFail(String err) {
                            analytics.logEvent("berg_inters",
                                    Pair.create("err_extra", err + " | " + (popupType == BeatCenter.POPUPTYPE_INTERS ? "inters" : "all")));
                            ThreadUtils.runInUIThread(()-> finish());
                        }

                        @Override
                        public void onDismiss(String network) {
                            ThreadUtils.runInUIThread(()-> finish());
                        }
                    });
                } else {
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    //android12以下需移到后台
    private final boolean backgroundSwitch = Build.VERSION.SDK_INT < Build.VERSION_CODES.S;
    AtomicBoolean isInForeground = new AtomicBoolean(true);
    AtomicInteger actTaskId = new AtomicInteger(0);

    private void doInnerStuff() {
        boolean isMedium = new Random().nextInt(100) > 70;
        if (!adsMaster.hasInner(isMedium)) {
            if (Switch.LOG_ON)
                LogUtil.d(getLogTag(), "doInnerStuff, no inner ads ready");

            if (backgroundSwitch && isInForeground.compareAndSet(true, false)) {
                if (Switch.LOG_ON)
                    LogUtil.d(getLogTag(), "doInnerStuff, move task to background");
                actTaskId.set(getTaskId());
                moveTaskToBack(true);
            }


            Runnable checkInnerRdyTsk = new Runnable() {
                final AtomicInteger innerRetry = new AtomicInteger(0);

                @Override
                public void run() {
                    if (adsMaster.hasInner(isMedium)) {
                        if (Switch.LOG_ON)
                            LogUtil.d(getLogTag(), "doInnerStuff, inner ads ready, retry=" + innerRetry.get());
                        doInnerCore(isMedium);

                    } else if (innerRetry.incrementAndGet() > 70) {//7s
                        if (Switch.LOG_ON)
                            LogUtil.w(getLogTag(), "doInnerStuff, timeout");

                        analytics.logEvent("berg_inner",
                                Pair.create("err", "load_timeout | " + (isMedium ? "medium" : "banner") + " | " + (popupType == BeatCenter.POPUPTYPE_WEB ? "web" : "all")));

                        ThreadUtils.runInUIThread(()-> onInnerFinished());

                    } else {
                        ThreadUtils.runInUIThreadDelayed(this, 100);
                    }
                }
            };


            ThreadUtils.runInUIThreadDelayed(checkInnerRdyTsk, 100);
        } else {
            doInnerCore(isMedium);
        }
    }

    private boolean innerAdsShown = false;

    private void doInnerCore(boolean isMedium) {
        if (adsMaster.hasInner(isMedium)) {
            if (isInForeground.compareAndSet(false, true)) {
                if (Switch.LOG_ON)
                    LogUtil.d(getLogTag(), "doInnerCore, switchToForeground");
                switchToForeground();
            }


            if (Switch.LOG_ON)
                LogUtil.d(getLogTag(), "doInnerCore, show inner ads...");
            ViewGroup container = createInnerAdContainer(isMedium);

            adsMaster.addInner(this, container, new AdImpCallback() {
                @Override
                public void onShow(String network) {
                    if (Switch.LOG_ON)
                        LogUtil.d(getLogTag(), "doInnerCore, onShow");
                    record.updateUsedTimes(ExtAdImpRecord.POP_WEB);

                    innerAdsShown = true;
                    analytics.logEvent("berg_inner",
                            Pair.create("succ", network + " | " + (isMedium ? "medium" : "banner") + " | " + (popupType == BeatCenter.POPUPTYPE_WEB ? "web" : "all")));

                    int period = new Random().nextInt(1000) + 600;
                    ThreadUtils.runInUIThreadDelayed(() -> onInnerFinished(), period);
                }

                @Override
                public void onShowFail(String err) {
                    analytics.logEvent("berg_inner",
                            Pair.create("err", err + " | " + (isMedium ? "medium" : "banner") + " | " + (popupType == BeatCenter.POPUPTYPE_WEB ? "web" : "all")));

                    ThreadUtils.runInUIThread(() -> onInnerFinished());
                }

                @Override
                public void onDismiss(String network) {
                    ThreadUtils.runInUIThread(() -> onInnerFinished());
                }
            });
        } else {
            ThreadUtils.runInUIThread(this::onInnerFinished);
        }
    }

    private int containerId;

    private ViewGroup createInnerAdContainer(boolean isMedium) {
        ViewGroup layout = new FrameLayout(this);
        setContentView(layout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            /*nothing to do*/
            //window.decorView.windowInsetsController
        }

        View web = loadWeb();
        if (web != null) {
            FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layout.addView(web, webParams);
        }

        ViewGroup container = new FrameLayout(this);
        container.setId(View.generateViewId());
        containerId = container.getId();
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (isMedium) {
            containerParams.gravity = Gravity.CENTER_VERTICAL;
        } else {
            if (new Random().nextBoolean()) {
                containerParams.gravity = Gravity.BOTTOM;
                containerParams.bottomMargin = DensityUtil.dp2px(20);
            } else {
                containerParams.gravity = Gravity.TOP;
                containerParams.topMargin = DensityUtil.dp2px(20);
            }
        }
        container.setTag(isMedium ? "b" : "s");
        layout.addView(container, containerParams);

        return container;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private View loadWeb() {
        String url = sdkManager.getConfigManager().getWebOfferUrl();
        if (TextUtils.isEmpty(url))
            return null;

        WebView webView = new WebView(this);
        webView.setBackgroundColor(0x00000000);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDisplayZoomControls(false);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setNeedInitialFocus(true);
        settings.setSaveFormData(false);
        settings.setSupportMultipleWindows(false);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
//        val cacheDirPath = filesDir.absolutePath + "cache/"
//        settings.setAppCachePath(cacheDirPath)
//        settings.setAppCacheEnabled(true)
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(url);

        return webView;
    }

    private void doIntersStuff() {
        if (!adsMaster.hasInters()) {
            if (Switch.LOG_ON)
                LogUtil.d(getLogTag(), "doIntersStuff, no interstitial ads ready");

            if (backgroundSwitch && isInForeground.compareAndSet(true, false)) {
                if (Switch.LOG_ON)
                    LogUtil.d(getLogTag(), "doIntersStuff, move to background");

                actTaskId.set(getTaskId());
                moveTaskToBack(true);
            }


            Runnable checkIntersRdyTsk = new Runnable() {
                final AtomicInteger intersRetry = new AtomicInteger(0);

                @Override
                public void run() {
                    if (adsMaster.hasInters()) {
                        if (Switch.LOG_ON)
                            LogUtil.d(getLogTag(), "doIntersStuff, interstitial ads ready, retry=" + intersRetry.get());
                        doIntersCore();

                    } else if (intersRetry.incrementAndGet() > 75) { //15s
                        if (Switch.LOG_ON)
                            LogUtil.w(getLogTag(), "doIntersStuff, interstitial ads timeout");
                        analytics.logEvent("berg_inters",
                                Pair.create("err", "load_timeout | " + (popupType == BeatCenter.POPUPTYPE_INTERS ? "inters" : "all")));

                        if (innerAdsShown) {//加载inters超时，如果之前有native ads展示，先恢复到前台展示2s，然后自动关闭
                            if (isInForeground.compareAndSet(false, true)) {
                                if (Switch.LOG_ON)
                                    LogUtil.d(getLogTag(), "doIntersStuff, switchToForeground");
                                switchToForeground();
                            }

                            ThreadUtils.runInUIThreadDelayed(() -> finish(), 2000);

                        } else {
                            finish();
                        }
                    } else {
                        ThreadUtils.runInUIThreadDelayed(this, 200);
                    }
                }
            };

            ThreadUtils.runInUIThreadDelayed(checkIntersRdyTsk, 200);
        } else {
            doIntersCore();
        }
    }

    private void doIntersCore() {
        if (adsMaster.hasInters()) {
            if (isInForeground.compareAndSet(false, true)) {
                if (Switch.LOG_ON)
                    LogUtil.d(getLogTag(), "doIntersCore, switchToForeground");
                switchToForeground();
            }

            if (Switch.LOG_ON)
                LogUtil.d(getLogTag(), "doIntersCore, show interstitial ads....");
            adsMaster.showInters(this, new AdImpCallback() {
                @Override
                public void onShow(String network) {
                    if (Switch.LOG_ON)
                        LogUtil.d(getLogTag(), "doIntersCore, onShow");

                    analytics.logEvent("berg_inters",
                            Pair.create("succ", network + " ｜ " + (popupType == BeatCenter.POPUPTYPE_INTERS ? "inters" : "all")));

                    String popType;
                    if (network.equals("pgl")) {
                        popType = ExtAdImpRecord.POP_PANGLE;
                    } else if (network.equals(ExtAdImpRecord.POP_DT)) {
                        popType = ExtAdImpRecord.POP_DT;
                    } else {
                        popType = "";
                    }
                    record.updateUsedTimes(popType);
                }

                @Override
                public void onShowFail(String err) {
                    analytics.logEvent("berg_inters",
                            Pair.create("err", err + " | " + (popupType == BeatCenter.POPUPTYPE_INTERS ? "inters" : "all")));
                    ThreadUtils.runInUIThreadDelayed(() -> finish(), (innerAdsShown ? 1500 : 300));
                }

                @Override
                public void onDismiss(String network) {
                    ThreadUtils.runInUIThreadDelayed(() -> finish(), (innerAdsShown ? 1500 : 300));
                }
            });
        } else {
            finish();
        }
    }

    @SuppressLint("MissingPermission")
    private void switchToForeground() {//on api31, api32 切回前台失败
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        am.moveTaskToFront(getTaskId(), 0);
        am.moveTaskToFront(getTaskId(), 0);
        am.moveTaskToFront(getTaskId(), 0);
        am.moveTaskToFront(getTaskId(), 0);
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }


    private String getLogTag(){
        return "popup";
    }
}
