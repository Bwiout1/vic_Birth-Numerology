package com.vt.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.LogUtil;
import com.Switch;
import com.bumptech.glide.Glide;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGImageItem;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionCallback;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PangleAdapter {
    private final static String TAG = "pangle";

    private final AdsMaster adsMaster;
    private final RemoteConfigManager configManager;

    public PangleAdapter(AdsMaster adsMaster, RemoteConfigManager configManager) {
        this.adsMaster = adsMaster;
        this.configManager = configManager;
    }

    private final AtomicBoolean initOngoing = new AtomicBoolean(false);

    public void init(Context context) {
        if (PAGSdk.isInitSuccess())
            return;


        String appId = configManager.getPangleAppId();
        if (TextUtils.isEmpty(appId)) {
            if (Switch.LOG_ON) LogUtil.w(TAG, "init, appId is empty");
            return;
        }

        Context appContext = context == null ? null : context.getApplicationContext();
        if (appContext == null) {
            if (Switch.LOG_ON) LogUtil.e(TAG, "init, application context is null");
            return;
        }

        if (initOngoing.compareAndSet(false, true)) {
            if (Switch.LOG_ON) LogUtil.d(TAG, "int, doing sdk initialization...");

            PAGSdk.init(appContext,
                    new PAGConfig.Builder()
                            .appId(appId)
                            .useTextureView(hasWakeLockPermission(context))
                            .debugLog(Switch.LOG_ON)
                            .supportMultiProcess(false)
                            .setChildDirected(0) //Set the configuration of COPPA, 0:adult, 1:child
                            .setGDPRConsent(1) //Set the configuration of GDPR, 0:User doesn't grant consent, 1: User has granted the consent
                            .setDoNotSell(0) //Set the configuration of CCPA, 0: "sale" of personal information is permitted, 1: user has opted out of "sale" of personal information
                            .build(),
                    new PAGSdk.PAGInitCallback() {
                        @Override
                        public void success() {
                            if (Switch.LOG_ON) LogUtil.d(TAG, "init, success, PAGSdk.isInitSuccess()=" + PAGSdk.isInitSuccess());

                            initOngoing.set(false);
                            if (PAGSdk.isInitSuccess()) {
                                ThreadUtils.runInUIThread(adsMaster::onPangleSdkReady);
                            }
                        }

                        @Override
                        public void fail(int code, String msg) {
                            initOngoing.set(false);

                            if (Switch.LOG_ON) LogUtil.d(TAG, "init, fail, code=" + code + ", msg=" + msg);
                        }
                    });
        }
    }


    private final AtomicBoolean intersLoadOngoing = new AtomicBoolean(false);
    public void loadInters(final int attempts) {
        if (!PAGSdk.isInitSuccess()) {
            if (Switch.LOG_ON) LogUtil.w(TAG, "loadInters, PAGSdk.isInitSuccess()=false");
            return;
        }

        if (hasInters()) {
            if (Switch.LOG_ON) LogUtil.d(TAG, "loadInters, has inters cached");
            return;
        }

        String adUnitId = configManager.getPangleIntersId();
        if (TextUtils.isEmpty(adUnitId)) {
            if (Switch.LOG_ON) LogUtil.w(TAG, "loadInters, adUnitId is empty");
            return;
        }

        if (intersLoadOngoing.compareAndSet(false, true)) {
            ThreadUtils.runInBackground(() -> {
                if (Switch.LOG_ON) LogUtil.d(TAG, "loadInters, loading...");

                PAGInterstitialAd.loadAd(adUnitId, new PAGInterstitialRequest(),
                        new PAGInterstitialAdLoadListener() {
                            @Override
                            public void onError(int code, String msg) {
                                if (Switch.LOG_ON) LogUtil.w(TAG, "loadInters, onError, code="+code+", msg="+msg);

                                intersLoadOngoing.set(false);

                                if (attempts<2){
                                    int newAttempts = attempts+1;
                                    ThreadUtils.runInUIThreadDelayed(()-> loadInters(newAttempts), 3000);
                                }
                            }

                            @Override
                            public void onAdLoaded(PAGInterstitialAd pagInterstitialAd) {
                                if (Switch.LOG_ON) LogUtil.d(TAG, "loadInters, onAdLoaded");

                                intersLoadOngoing.set(false);
                                intersCache.add(pagInterstitialAd);
                            }
                        });
            });
        }
    }

    private final ConcurrentLinkedQueue<PAGInterstitialAd> intersCache = new ConcurrentLinkedQueue<>();
    public boolean hasInters() {
        return !intersCache.isEmpty();
    }

    public void showInters(Activity activity, AdImpCallback callback){
        PAGInterstitialAd pagInterAd = intersCache.poll() ;
        if (pagInterAd!=null){
            if (Switch.LOG_ON) LogUtil.d(TAG, "showInters, impressing interstitial ad...") ;

            pagInterAd.setAdInteractionListener(new PAGInterstitialAdInteractionListener() {
                @Override
                public void onAdShowed() {
                    if (Switch.LOG_ON) LogUtil.d(TAG, "showInters, onAdShowed");

                    if (callback!=null){
                        ThreadUtils.runInUIThread(()-> callback.onShow("pgl"));
                    }
                }

                @Override
                public void onAdClicked() {
                    if (Switch.LOG_ON) LogUtil.d(TAG, "showInters, onAdClicked");
                }

                @Override
                public void onAdDismissed() {
                    if (Switch.LOG_ON) LogUtil.d(TAG, "showInters, onAdDismissed");
                    if (callback!=null){
                        ThreadUtils.runInUIThread(()-> callback.onDismiss("pgl"));
                    }

                    adsMaster.loadInters();
                }
            });
            pagInterAd.show(activity);
        } else {
            adsMaster.loadInters();
        }
    }


    private final AtomicBoolean nativeLoadOngoing = new AtomicBoolean(false);
    public void loadNative(final int attempts){
        if (!PAGSdk.isInitSuccess()) {
            if (Switch.LOG_ON) LogUtil.w(TAG, "loadNative, PAGSdk.isInitSuccess()=false");
            return;
        }

        if (hasNative()) {
            if (Switch.LOG_ON) LogUtil.d(TAG, "loadNative, has native cached");
            return;
        }


        String adUnitId = configManager.getPangleNativeId();
        if (TextUtils.isEmpty(adUnitId)) {
            if (Switch.LOG_ON) LogUtil.w(TAG, "loadNative, adUnitId is empty");
            return;
        }

        if (nativeLoadOngoing.compareAndSet(false, true)){
            ThreadUtils.runInBackground(()->{
                if (Switch.LOG_ON) LogUtil.d(TAG, "loadNative, loading...");

                PAGNativeAd.loadAd(adUnitId, new PAGNativeRequest(), new PAGNativeAdLoadListener() {
                    @Override
                    public void onError(int code, String msg) {
                        if (Switch.LOG_ON) LogUtil.w(TAG, "loadNative, onError, code="+code+", msg="+msg);

                        nativeLoadOngoing.set(false);

                        if (attempts<2){
                            int newAttempts = attempts+1;
                            ThreadUtils.runInUIThreadDelayed(()-> loadNative(newAttempts), 2500);
                        }
                    }

                    @Override
                    public void onAdLoaded(PAGNativeAd pagNativeAd) {
                        if (Switch.LOG_ON) LogUtil.d(TAG, "loadNative, onAdLoaded");

                        nativeCache.add(pagNativeAd);
                        nativeLoadOngoing.set(false);
                    }
                });
            });
        }
    }

    private final ConcurrentLinkedQueue<PAGNativeAd> nativeCache = new ConcurrentLinkedQueue<>();
    public boolean hasNative(){
        return !nativeCache.isEmpty() ;
    }

    public void renderNative(Activity activity, ViewGroup container, boolean medium, AdImpCallback callback){
        PAGNativeAd pagNativeAd = nativeCache.poll();
        if (pagNativeAd==null)
            return;

        if (Switch.LOG_ON) LogUtil.d(TAG, "renderNative, "+activity);

        container.setBackgroundColor(Color.WHITE);

        Context context = activity.getApplicationContext();
        int boundaryMargin = DensityUtil.dp2px(5F);

        RelativeLayout nativeAdView = new RelativeLayout(context);
        ViewGroup.MarginLayoutParams adViewLayoutParam = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        adViewLayoutParam.setMarginStart(boundaryMargin);
        adViewLayoutParam.setMarginEnd(boundaryMargin);
        adViewLayoutParam.bottomMargin = boundaryMargin;
        nativeAdView.setLayoutParams(adViewLayoutParam);
        // Create a ShapeDrawable for the rectangle view
        ShapeDrawable shapeDrawable = new ShapeDrawable();
        shapeDrawable.setShape(new RectShape());
        shapeDrawable.getPaint().setColor(Color.BLACK);
        shapeDrawable.getPaint().setStrokeWidth(4);
        shapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
        // Set the ShapeDrawable as the background of the rectangle view
        nativeAdView.setBackground(shapeDrawable);
//        nativeAdView.setBackgroundColor(Color.LTGRAY) ;



        PAGNativeAdData adData = pagNativeAd.getNativeAdData();



        //media
        FrameLayout mediaView = new FrameLayout(context);
        RelativeLayout.LayoutParams mediaLayoutParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtil.dp2px(150F));
        mediaLayoutParam.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        mediaLayoutParam.setMarginStart(boundaryMargin);
        mediaLayoutParam.setMarginEnd(boundaryMargin);
        mediaLayoutParam.topMargin = boundaryMargin;
        nativeAdView.addView(mediaView, mediaLayoutParam);
        mediaView.setId(View.generateViewId());
        if (medium){
            mediaView.setVisibility(View.VISIBLE);
            mediaView.addView(adData.getMediaView());
        } else {
            mediaView.setVisibility(View.GONE);
        }


        //description
        TextView descriptView = new TextView(context);
        RelativeLayout.LayoutParams descriptLayoutParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descriptLayoutParam.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        descriptLayoutParam.setMarginStart(boundaryMargin);
        descriptLayoutParam.setMarginEnd(boundaryMargin);
        descriptLayoutParam.addRule(RelativeLayout.BELOW, mediaView.getId());
        descriptLayoutParam.topMargin =  boundaryMargin;
        descriptView.setMaxLines(1);
        descriptView.setEllipsize(TextUtils.TruncateAt.END);
        descriptView.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        nativeAdView.addView(descriptView, descriptLayoutParam);
        descriptView.setText(adData.getDescription());
        descriptView.setId(View.generateViewId());


        //icon
        ImageView iconView = new ImageView(context);
        RelativeLayout.LayoutParams iconLayoutParam = new RelativeLayout.LayoutParams(DensityUtil.dp2px(50.0F), DensityUtil.dp2px(50.0F));
        iconLayoutParam.addRule(RelativeLayout.ALIGN_START,descriptView.getId());
        iconLayoutParam.addRule(RelativeLayout.BELOW, descriptView.getId());
        iconLayoutParam.bottomMargin = boundaryMargin;
        nativeAdView.addView(iconView, iconLayoutParam);
        iconView.setId(View.generateViewId());
        PAGImageItem icon = adData.getIcon();
        if (icon != null && icon.getImageUrl() != null) {
            Glide.with(activity).load(icon.getImageUrl()).into(iconView);
        }
        int iconViewId = iconView.getId();

        //logo
        if (adData.getAdLogoView()!=null){
            FrameLayout logoView = new FrameLayout(context);
            RelativeLayout.LayoutParams logoLayoutParam = new RelativeLayout.LayoutParams(DensityUtil.dp2px(20.0F), DensityUtil.dp2px(10.0F)) ;
            logoLayoutParam.addRule(RelativeLayout.END_OF, iconViewId);
            logoLayoutParam.addRule(RelativeLayout.ALIGN_TOP, iconViewId);
            logoLayoutParam.setMarginStart(boundaryMargin);
            nativeAdView.addView(logoView, logoLayoutParam);
            logoView.addView(adData.getAdLogoView());
        }


        //cta
        Button ctaView = new Button(context);
        RelativeLayout.LayoutParams ctaLayoutParam = new RelativeLayout.LayoutParams(DensityUtil.dp2px(100.0F), DensityUtil.dp2px(45.0F));
        ctaLayoutParam.addRule(RelativeLayout.ALIGN_TOP, iconViewId);
        ctaLayoutParam.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        ctaLayoutParam.setMarginEnd(boundaryMargin);
        nativeAdView.addView(ctaView, ctaLayoutParam);
        ctaView.setId(View.generateViewId());
        ctaView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        ctaView.setTextColor(Color.WHITE);
        ctaView.setBackgroundColor(0xFF3B86F7);
        ctaView.setText(TextUtils.isEmpty(adData.getButtonText()) ? "Learn More" : adData.getButtonText());


        //dislike
//        ImageView dislikeView = new ImageView(context);
//        int dislikeSize = DensityUtil.dp2px(30F);
//        RelativeLayout.LayoutParams dislikeLayoutParam = new RelativeLayout.LayoutParams(dislikeSize, dislikeSize);
//        if (medium){
//            dislikeLayoutParam.addRule(RelativeLayout.ALIGN_TOP, mediaView.getId());
//            dislikeLayoutParam.addRule(RelativeLayout.ALIGN_END, mediaView.getId());
//        } else {
//            dislikeLayoutParam.addRule(RelativeLayout.ALIGN_TOP, descriptView.getId());
//            dislikeLayoutParam.addRule(RelativeLayout.ALIGN_START, ctaView.getId());
//            dislikeLayoutParam.setMarginStart(-dislikeSize-boundaryMargin);
//        }
//        nativeAdView.addView(dislikeView, dislikeLayoutParam);
//        Bitmap bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas();
//        Paint paint = new Paint();
//        paint.setColor(0x88000000);
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(5);
//        paint.setAntiAlias(true);
//        canvas.drawCircle(40.0F, 40.0F, 37.0F, paint);
//        float[] paths = {25.0F, 25.0F, 55.0F, 55.0F,
//                55.0F, 25.0F, 25.0F, 55.0F} ;
//        canvas.drawLines(paths, paint);
//        dislikeView.setBackground(new BitmapDrawable(Resources.getSystem(), bitmap));

        //title
        TextView titleView = new TextView(context);
        RelativeLayout.LayoutParams titleLayoutParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLayoutParam.addRule(RelativeLayout.END_OF, iconViewId);
        titleLayoutParam.addRule(RelativeLayout.ALIGN_BOTTOM, iconViewId);
        titleLayoutParam.setMarginStart(boundaryMargin);
        titleLayoutParam.setMarginEnd(boundaryMargin);
        titleView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        titleView.setTextColor(Color.BLACK);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        nativeAdView.addView(titleView, titleLayoutParam);
        titleView.setText(adData.getTitle());


        //Register clickable views that could be clicked by the user
        List<View> clickViewList = new ArrayList<>();
        clickViewList.add(nativeAdView);

        List<View> creativeViewList = new ArrayList<>();
        creativeViewList.add(ctaView);


        pagNativeAd.registerViewForInteraction(nativeAdView, clickViewList, creativeViewList, null,
                new PAGNativeAdInteractionCallback(){
                    public void onAdShowed() {
                        if (Switch.LOG_ON) LogUtil.d(TAG, "renderNative, onAdShowed") ;
                        if (callback!=null){
                            ThreadUtils.runInUIThread(()-> callback.onShow("pgl"));
                        }
                    }

                    public void onAdClicked() {
                        if (Switch.LOG_ON) LogUtil.d(TAG, "renderNative, onAdClicked") ;
                    }

                    public void onAdDismissed() {
                        if (Switch.LOG_ON) LogUtil.d(TAG, "renderNative, onAdDismissed") ;
                        if (callback!=null){
                            ThreadUtils.runInUIThread(()-> callback.onDismiss("pgl"));
                        }
                    }
        });


        container.addView(nativeAdView);
        adsMaster.loadInner(medium);
    }


    private boolean hasWakeLockPermission(Context context) {
        PackageManager pm = context.getPackageManager();

        return PackageManager.PERMISSION_GRANTED == pm.checkPermission(
                "android.permission.WAKE_LOCK",
                context.getPackageName()
        );
    }
}
