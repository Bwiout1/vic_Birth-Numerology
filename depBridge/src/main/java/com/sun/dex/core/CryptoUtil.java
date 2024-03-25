package com.sun.dex.core;


import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import com.sun.dex.core.crypto.PKCSPadding;
import com.sun.dex.core.exception.InvalidArgumentsException;

import androidx.annotation.Keep;

public class CryptoUtil {

    @Keep
    public static byte[] decAndRemovePadding(String key, byte[] in) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidArgumentsException {
        int i = EncDecHelper.getCipherIndexByKey(key);
        Cipher cipher = EncDecHelper.getDecCipher(i, key);
        return decAndRemovePadding(cipher, in);
    }

    public static byte[] decAndRemovePadding(Cipher cipher, byte[] in) throws IllegalBlockSizeException, BadPaddingException {
        byte[] decMsg = EncDecHelper.decrypt(cipher, in, in.length);
        return PKCSPadding.removePadding(decMsg, cipher);
    }
}
