package com.vt.sdk;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class RefreshConfigTask extends Worker {
    public RefreshConfigTask(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SdkManager.sdkManager.get().getBeatCenter().doConfigRefreshJob();

        return Result.success();
    }
}
