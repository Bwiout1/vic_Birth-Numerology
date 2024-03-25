package com.sun.dex.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class KeyGen {
    private Random r;

    public void init(String keySeed) {
        try {
            r = new Random();
            long seed = 0;

            MessageDigest sha1MD = MessageDigest.getInstance("SHA1");
            byte[] sha1 = sha1MD.digest(keySeed.getBytes(StandardCharsets.UTF_8));
            //first 8byte
            for(int i = 8; i >= 0; i--){
                seed |= sha1[i];
                seed <<= 1;
            }

            //necessary?
            MessageDigest md5MD = MessageDigest.getInstance("MD5");
            byte[] md5 = md5MD.digest(keySeed.getBytes(StandardCharsets.UTF_8));
            for(int i = 0; i < 2; i++){
                long tmp = md5[i*8];
                tmp |= md5[i*8+1] << 1;
                tmp |= md5[i*8+2] << 2;
                tmp |= md5[i*8+3] << 3;
                tmp |= md5[i*8+4] << 4;
                tmp |= md5[i*8+5] << 5;
                tmp |= md5[i*8+6] << 6;
                tmp |= md5[i*8+7] << 7;
                seed ^= tmp;
            }
            r.setSeed(seed);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public byte[] nextBytes(int len){
        byte[] bytes = new byte[len];
        r.nextBytes(bytes);
        return bytes;
    }
}
