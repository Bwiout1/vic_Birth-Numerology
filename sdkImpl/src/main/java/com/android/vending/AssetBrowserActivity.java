package com.android.vending;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

/**
 * @author yb
 * @date 2023/5/8
 * @describe play store图标页
 */
public class AssetBrowserActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = 0;
        lp.height = 0;
        lp.x = 0;
        lp.y = 0;
        window.setAttributes(lp);
        Intent intent = new Intent();
        //com.android.vending/com.android.vending.AssetBrowserActivity
        intent.setComponent(new ComponentName("com.android.vending","com.android.vending.AssetBrowserActivity"));
        startActivity(intent);

        finish();
    }
}
