package com.sun.dex.core.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class BaseCipherBuilder implements ICipherBuilder {
    protected String cipherName;
    protected String algName;
    protected boolean isEnc;

    protected KeyGen keyGen;
    protected byte[] key1; //encrypt key
    protected byte[] key2; //IV or salt
    protected Key key1Obj;
    protected AlgorithmParameterSpec key2Obj; //IV or salt

    protected Cipher cipher;

    @Override
    public void init(String cipherName, String keySeed, boolean isEnc) {
        this.isEnc = isEnc;
        if(cipherName == null)
            throw new IllegalArgumentException("cipherName == null");
        this.cipherName = cipherName;
        this.algName = cipherName;
        if(keySeed == null)
            throw new IllegalArgumentException("keySeed == null");
        keyGen = new KeyGen();
        keyGen.init(keySeed);
    }

    @Override
    public byte[] getKey1() {
        return key1;
    }

    @Override
    public byte[] getKey2() {
        return key2;
    }

    @Override
    public Cipher build() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        cipher = Cipher.getInstance(cipherName);
        if(key2Obj == null)
            cipher.init(isEnc ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key1Obj);
        else
            cipher.init(isEnc ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key1Obj, key2Obj);
        return cipher;
    }
}
