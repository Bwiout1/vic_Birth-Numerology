package com.vt.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.Switch;


public class StorageFactory {
    public static SharedPreferences getStorage(Context context, String name){
        try {
            MasterKey key = new MasterKey.Builder(context, name)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    name,
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Throwable ignored){
            if (Switch.LOG_ON) ignored.printStackTrace();

            return context.getSharedPreferences(name, Context.MODE_PRIVATE);
        }
    }
}
