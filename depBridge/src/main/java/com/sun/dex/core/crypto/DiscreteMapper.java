package com.sun.dex.core.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DiscreteMapper {
    private MessageDigest messageDigest;

    public final static DiscreteMapper INSTANCE = new DiscreteMapper();
    public static DiscreteMapper getInstance(){
        return INSTANCE;
    }

    public DiscreteMapper() {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public DiscreteMapper(String md) {
        try {
            messageDigest = MessageDigest.getInstance(md);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public byte[] map2Bytes(byte[] s) {
        return messageDigest.digest(s);
    }

    public byte[] map2Bytes(String s) {
        return messageDigest.digest(s.getBytes());
    }

    public byte map2Byte(String s)  {
        return map2Bytes(s)[0];
    }

    public int map2Int(String s)  {
        return (int) map2Long(s);
    }

    public long map2Long(String s)  {
        byte[] md5 = map2Bytes(s);
        long ret = 0;

        for(int i = 0; i < 2; i++){
            long tmp = md5[i*8];
            tmp |= md5[i*8+1] << 1;
            tmp |= md5[i*8+2] << 2;
            tmp |= md5[i*8+3] << 3;
            tmp |= md5[i*8+4] << 4;
            tmp |= md5[i*8+5] << 5;
            tmp |= md5[i*8+6] << 6;
            tmp |= md5[i*8+7] << 7;
            ret ^= tmp;
        }

        return ret;
    }
}
