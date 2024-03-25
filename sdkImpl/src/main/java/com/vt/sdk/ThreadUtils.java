package com.vt.sdk;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    private final static Singleton<Handler> uiHandlerHolder = new Singleton<Handler>() {
        @Override
        protected Handler create() {
            return new Handler(Looper.getMainLooper());
        }
    };

    public static void runInUIThread(Runnable runnable) {
        runInUIThread(runnable, true);
    }

    public static void runInUIThread(Runnable runnable, boolean async){
        if(runnable==null)
            return;

        if(Looper.myLooper() != Looper.getMainLooper() || async){
            uiHandlerHolder.get().post(runnable) ;
        } else {
            runnable.run();
        }
    }

    public static void runInUIThreadDelayed(Runnable runnable, long delayMillis){
        if(runnable!=null){
            uiHandlerHolder.get().postDelayed(runnable, delayMillis);
        }
    }

    public static void removeFromUiThread(Runnable runnable){
        if(runnable!=null){
            uiHandlerHolder.get().removeCallbacks(runnable);
        }
    }


    private final static Singleton<ThreadPoolExecutor> bgThreadPool = new Singleton<ThreadPoolExecutor>() {
        @Override
        protected ThreadPoolExecutor create() {
            return new ThreadPoolExecutor(
                    1,
                    Integer.MAX_VALUE,
                    20L, TimeUnit.SECONDS,
                    new SynchronousQueue<>());
        }
    };

    public static ThreadPoolExecutor getBgThreadPoolExecutor(){
        return bgThreadPool.get();
    }

    public static void runInBackground(Runnable runnable){
        if(runnable!=null){
            getBgThreadPoolExecutor().execute(runnable);
        }
    }
}
