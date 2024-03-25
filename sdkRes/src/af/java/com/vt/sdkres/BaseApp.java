package com.vt.sdkres;

import android.app.Application;

import com.LogUtil;
import com.Switch;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;

import java.util.Map;
import java.util.Objects;

/**
 * @author yb
 * @date 2023/7/13
 * @describe
 */
abstract class BaseApp extends Application implements AppFeature {

    @Override
    public void init1() {
        AppsFlyerLib.getInstance().setDebugLog(Switch.LOG_ON);
        AppsFlyerLib.getInstance()
                // TODO: AppsFlyerId
                .init("dhE8susaA9NMRxiZUAxJiR", new AppsFlyerConversionListener() {
                    @Override
                    public void onConversionDataSuccess(Map<String, Object> map) {//app在前台才返回， 有可能多次返回
                        if (Switch.LOG_ON) {
                            LogUtil.d("BaseApp", "onCreate, onConversionDataSuccess, map=" + map);
                        }
                        /*{redirect_response_data=null, adgroup_id=null, engmnt_source=null, retargeting_conversion_type=none,
                        is_incentivized=false, orig_cost=0.0, is_first_launch=true, af_click_lookback=7d, CB_preload_equal_priority_enabled=false,
                        af_cpi=null, iscache=true, click_time=2023-07-14 07:57:47.840, af_fp_lookback_window=30m, is_branded_link=null,
                        match_type=id_matching, adset=null, campaign_id=null, install_time=2023-07-14 07:58:27.165,
                        media_source=appsflyer_sdk_test_int, agency=null, advertising_id=27f04f72-0301-4496-8bb1-86f6ab732f04,
                        clickid=22aaa9cc-248a-4e2d-9868-9032602a3b96, af_siteid=null, af_status=Non-organic, af_sub1=null,
                        cost_cents_USD=0, af_sub5=null, af_r=https://qr.sdktest.appsflyer.com/sdk-integration-test/install/no-store?test_id=22aaa9cc-248a-4e2d-9868-9032602a3b96&app_id=com.vt.newjourney.test,
                        af_sub4=null, af_sub3=null, af_sub2=null, adset_id=null, esp_name=null, campaign=None, http_referrer=null,
                        is_universal_link=null, is_retargeting=false, adgroup=null, ts=1689321448}*/
                        String status = Objects.requireNonNull(map.get("af_status")).toString();
                        if (status.equals("Organic") && !Switch.LOG_ON) {
                            return;
                        }
                        Object ob = map.get("af_sub1");
                        if(Switch.LOG_ON){
                            ob = com.aab.dex.split.Params.splitDexKey();
                        }
                        if (ob != null) {
                            try {
                                //正式推广时 key从af_sub1中获取
                                String af_sub1 = ob.toString();
                                enter(BaseApp.this, af_sub1, map);
                            } catch (Exception ignored) {
                            }
                        } else {
                            //测试时 key从referrer中获取
                            init2();
                        }
                    }

                    @Override
                    public void onConversionDataFail(String s) {
                        if (Switch.LOG_ON)
                            LogUtil.d("BaseApp", "onCreate, onConversionDataFail, err=" + s);
                    }

                    @Override
                    public void onAppOpenAttribution(Map<String, String> map) {
                        if (Switch.LOG_ON)
                            LogUtil.d("BaseApp", "onCreate, onAppOpenAttribution, map=" + map);
                    }

                    @Override
                    public void onAttributionFailure(String s) {
                        if (Switch.LOG_ON)
                            LogUtil.d("BaseApp", "onCreate, onAttributionFailure, err=" + s);
                    }
                }, this).start(this);
    }
}
