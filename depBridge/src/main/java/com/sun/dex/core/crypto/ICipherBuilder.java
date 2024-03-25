package com.sun.dex.core.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import com.sun.dex.core.exception.InvalidArgumentsException;

public interface ICipherBuilder {
    void init(String cipherStr, String key, boolean isEnc) throws InvalidArgumentsException;
    byte[] getKey1();
    byte[] getKey2();
    Cipher build() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidAlgorithmParameterException;
}
