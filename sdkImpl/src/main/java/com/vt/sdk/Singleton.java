package com.vt.sdk;

public abstract class Singleton<T> {
    public Singleton(){}

    private volatile T mInstance;

    protected abstract T create();

    public final T get() {
        if(mInstance==null){
            synchronized (this){
                if(mInstance==null){
                    mInstance = create();
                }
            }
        }

        return mInstance;
    }
}

