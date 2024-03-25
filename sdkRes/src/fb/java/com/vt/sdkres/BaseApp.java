package com.vt.sdkres;

import android.app.Application;
import android.net.Uri;

import com.LogUtil;
import com.Switch;
import com.facebook.FacebookSdk;
import com.facebook.applinks.AppLinkData;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yb
 * @date 2023/7/14
 * @describe
 */
abstract class BaseApp extends Application implements AppFeature {

    @Override
    public void init1() {
        if (!FacebookSdk.isInitialized()) {
            FacebookSdk.sdkInitialize(this);
        }
        AppLinkData.fetchDeferredAppLinkData(this, (AppLinkData appLinkData) -> {
            if (Switch.LOG_ON) {
                LogUtil.d("BaseApp", "onCreate, appLinkData:" + appLinkData);
            }
            if (appLinkData != null) {
                try {
                    Map<String, Object> map = new HashMap<>();
                    map.put("uri", appLinkData.getTargetUri());
                    map.put("ref", appLinkData.getRef());
                    map.put("arg", appLinkData.getArgumentBundle());
                    map.put("data", appLinkData.getAppLinkData());
                    if (Switch.LOG_ON) {
                        LogUtil.d("BaseApp", "onCreate, onDeferredAppLinkDataFetched, map=" + map);
                    }
                    Uri uri = appLinkData.getTargetUri();
                    if (uri != null) {
                        //正式推广时key从deeplink中获取
                        String deeplink = uri.toString();
                        String[] sts = deeplink.split("&");

                        String key = "sub=";
                        for (String st : sts) {
                            if (st.contains(key)) {
                                int start = st.indexOf(key) + key.length();
                                //解密key
                                String value = deeplink.substring(start);

                                enter(BaseApp.this, value, map);
                                return;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            //测试时 key从referrer中获取
            init2();
        });
    }
}
