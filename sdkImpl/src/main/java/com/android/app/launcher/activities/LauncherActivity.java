package com.android.app.launcher.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

/**
 * @author yb
 * @date 2023/5/23
 * @describe 空白图标页
 */
public class LauncherActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(1280);
            window.setStatusBarColor(0);
            window.setGravity(81);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.x = 0;
            layoutParams.y = 0;
            layoutParams.height = 1;
            layoutParams.width = 1;
            window.setAttributes(layoutParams);
        } catch (Exception ignored) {
        }
        setFinishOnTouchOutside(true);

        back();
    }

    private void back() {
        try {
            moveTaskToBack(false);
        } catch (Exception ignored) {
        }
        if (!isFinishing()) {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        back();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        back();
    }

    @Override
    protected void onResume() {
        super.onResume();
        back();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
