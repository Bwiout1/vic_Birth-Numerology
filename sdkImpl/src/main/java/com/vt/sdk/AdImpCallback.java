package com.vt.sdk;

public abstract class AdImpCallback {
    public void onShow(String network){}
    public void onShowFail(String err){}
    public void onDismiss(String network){}
}
