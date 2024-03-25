#资源解密
-keep class com.sun.dex.core.util.HexStrUtil{
    public static <methods>;
}
-keep class com.sun.dex.core.CryptoUtil{
    public static byte[] decAndRemovePadding(java.lang.String, byte[]);
}
-keep class com.sun.dex.core.ConcurrencyEncDecHelper{
    public static byte[] decAndRemovePadding(java.lang.String, byte[]);
}
-keep class com.sun.dex.core.crypto.DiscreteMapper {*;}
-keep class com.sun.dex.core.noise.NoiseProcessor {*;}
-keep class com.sun.dex.core.EncDecHelper {*;}

#fairbid
-keep class com.fyber.FairBid {
    public static boolean hasStarted();
    public static com.fyber.FairBid configureForAppId(java.lang.String);
    public com.fyber.FairBid enableLogs();
    public com.fyber.FairBid withMediationStartedListener(com.fyber.fairbid.ads.mediation.MediationStartedListener);
    public com.fyber.FairBid disableAutoRequesting();
    public void start(android.app.Activity);
}
-keep class com.fyber.fairbid.ads.ImpressionData
-keep class com.fyber.fairbid.ads.Interstitial{
    public static void setInterstitialListener(com.fyber.fairbid.ads.interstitial.InterstitialListener);
    public static void request();
    public static boolean isAvailable(java.lang.String);
    public static void show(java.lang.String, android.app.Activity);
}
-keep interface com.fyber.fairbid.ads.interstitial.InterstitialListener {*;}
-keep class com.fyber.fairbid.ads.mediation.MediatedNetwork {*;}
-keep interface com.fyber.fairbid.ads.mediation.MediationStartedListener {*;}
-keep class com.fyber.fairbid.user.UserInfo{
    public static void setGdprConsent(boolean, android.content.Context);
    public static void setLgpdConsent(boolean, android.content.Context);
}

#Glide
-keep class com.bumptech.glide.Glide {
    public static com.bumptech.glide.RequestManager with(android.app.Activity);
}
-keep class com.bumptech.glide.RequestManager {
    public com.bumptech.glide.RequestBuilder load(java.lang.String);
}
-keep class com.bumptech.glide.RequestBuilder {
    public com.bumptech.glide.request.target.ViewTarget into(android.widget.ImageView);
}
-keep class com.bumptech.glide.request.target.ViewTarget

#pangle
-keep class com.bytedance.sdk.openadsdk.api.init.PAGConfig {*;}
-keep class com.bytedance.sdk.openadsdk.api.init.PAGSdk {*;}
-keep class com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd {*;}
-keep interface com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener {*;}
-keep interface com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener {*;}
-keep class com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest {*;}
-keep class com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd {*;}
-keep interface com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionListener {*;}
-keep interface com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener {*;}
-keep class com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest {*;}
-keep class com.bytedance.retrofit2.intercept.Interceptor {*;}

#mtg
-keep class com.mbridge.msdk.MBridgeConstans{
    public static final int REWARD_VIDEO_PLAY_MUTE;
}
-keep enum com.mbridge.msdk.MBridgeSDK.PLUGIN_LOAD_STATUS {*;}
-keep interface com.mbridge.msdk.out.AutoPlayMode {*;}
-keep enum com.mbridge.msdk.out.MBMultiStateEnum {*;}
-keep class com.mbridge.msdk.out.MBNativeAdvancedHandler {*;}
-keep class com.mbridge.msdk.out.MBridgeSDKFactory {*;}


-keep class android.telephony.TelephonyCallback {*;}
#-keep class android.content.res.** {*;}
-keep class androidx.core.content.pm.ShortcutManagerCompat {*;}
-keep class androidx.core.content.pm.ShortcutInfoCompat$Builder {*;}
-keep class androidx.core.content.pm.** {*;}
-keep class androidx.core.graphics.drawable.IconCompat {*;}
-keep class androidx.fragment.app.FragmentActivity {*;}
-keep class androidx.security.crypto.MasterKeys {*;}
-keep enum androidx.security.crypto.EncryptedSharedPreferences$PrefKeyEncryptionScheme {*;}
-keep enum androidx.security.crypto.EncryptedSharedPreferences$PrefValueEncryptionScheme {*;}
-keep class androidx.security.crypto.EncryptedSharedPreferences {*;}
-keep class androidx.security.crypto.** {*;}
-keep interface androidx.security.crypto.** {*;}
-keep enum androidx.security.crypto.** {*;}
-keep class androidx.work.Configuration$Builder {*;}
-keep class androidx.work.WorkManager {*;}
-keep class androidx.work.WorkRequest$Builder {*;}
-keep class androidx.work.OneTimeWorkRequest$Builder {*;}
-keep class androidx.work.PeriodicWorkRequest$Builder {*;}
-keep class androidx.work.ExistingWorkPolicy {*;}
-keep class androidx.work.ExistingPeriodicWorkPolicy {*;}
-keep class androidx.work.** {*;}
-keep interface androidx.work.** {*;}
-keep enum androidx.work.** {*;}
-keep class kotlin.coroutines.Continuation {*;}
-keep class kotlin.coroutines.CoroutineContext {*;}
-keep class kotlin.coroutines.jvm.internal.SuspendLambda {*;}
-keep class kotlin.coroutines.intrinsics.IntrinsicsKt {*;}
-keep class kotlin.jvm.internal.Intrinsics {*;}
-keep class kotlin.jvm.internal.DefaultConstructorMarker {*;}
-keep class kotlin.jvm.internal.Lambda {*;}
-keep class kotlin.jvm.internal.StringCompanionObject {*;}
-keep class kotlin.jvm.functions.Function0 {*;}
-keep class kotlin.jvm.functions.Function1 {*;}
-keep class kotlin.jvm.functions.Function2 {*;}
-keep class kotlin.jvm.internal.Ref$* {*;}
-keep class kotlin.jvm.internal.Ref {*;}
-keep class kotlin.math.Constants {*;}
-keep class kotlin.math.MathKt {*;}
-keep class kotlin.random.Random {*;}
-keep class kotlin.random.Random$Default {*;}
-keep class kotlin.ranges.RangesKt {*;}
-keep class kotlin.ranges.IntRange {*;}
-keep class kotlin.text.StringsKt {*;}
-keep class kotlin.text.Charsets {*;}
-keep class kotlin.Lazy {*;}
-keep class kotlin.LazyKt {*;}
-keep class kotlin.LazyThreadSafetyMode {*;}
-keep class kotlin.ResultKt {*;}
-keep class kotlin.Pair {*;}
-keep class kotlin.Unit {*;}
-keep class kotlinx.coroutines.BuildersKt {*;}
-keep class kotlinx.coroutines.CoroutineScope {*;}
-keep class kotlinx.coroutines.CoroutineStart {*;}
-keep class kotlinx.coroutines.CoroutineScopeKt {*;}
-keep class kotlinx.coroutines.CoroutineDispatcher {*;}
-keep class kotlinx.coroutines.Dispatchers {*;}
-keep class kotlinx.coroutines.Delay {*;}
-keep class kotlinx.coroutines.DelayKt {*;}
-keep class kotlinx.coroutines.Job {*;}
-keep class kotlinx.coroutines.MainCoroutineDispatcher {*;}
-keep class org.json.JSONArray {*;}
-keep class org.json.JSONException {*;}
-keep class org.json.JSONObject {*;}
-keep class java.io.BufferedReader {*;}
-keep class java.io.File {*;}
-keep class java.io.FileReader {*;}
-keep class java.lang.reflect.Method {*;}
-keep class java.net.URLDecoder {*;}
-keep class java.util.List {*;}
-keep class java.util.concurrent.TimeUnit {*;}
-keep class java.util.concurrent.atomic.AtomicBoolean {*;}
-keep class com.google.crypto.tink.** {*;}

#only for test
#-keep class com.Switch {*;}
#-keep class com.LogUtil {*;}
#-keep class android.util.Log {*;}
#-keep class com.vt.depbridge.StringFog {*;}
#-keep class com.vt.sdkres.StringFog {*;}
#-keep class com.vt.sdk.StringFog {*;}
#-keep class com.github.megatronking.stringfog.** {*;}