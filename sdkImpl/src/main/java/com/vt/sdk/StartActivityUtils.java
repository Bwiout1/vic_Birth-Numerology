package com.vt.sdk;

import android.app.Application;
import android.app.PendingIntent;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;

import com.Switch;

public class StartActivityUtils {
    public static void startActivity(Context context, Intent intent){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            context.startActivity(intent);
        } else {
            virtualDisplaySolution(context, intent);
        }
    }

    private static void virtualDisplaySolution(Context context, Intent intent) {//29,30, 31，32 works
        try {
            DisplayManager  displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            VirtualDisplay virtualDisplay = displayManager.createVirtualDisplay("virtual_display_other",
                    500,500, context.getResources().getConfiguration().densityDpi,
                    null, 0);
            new Presentation(context, virtualDisplay.getDisplay()).show();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE).send();
            } else {
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT).send();
            }
        } catch (Exception ignored){
            if (Switch.LOG_ON) ignored.printStackTrace();
        }
    }
}
