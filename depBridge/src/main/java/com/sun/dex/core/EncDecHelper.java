package com.sun.dex.core;



import com.Switch;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import com.sun.dex.core.crypto.AesDesBlowfishArc4CipherBuilder;
import com.sun.dex.core.crypto.DiscreteMapper;
import com.sun.dex.core.crypto.ICipherBuilder;
import com.sun.dex.core.crypto.PBEWithMD5AndDESCipherBuilder;
import com.sun.dex.core.exception.InvalidArgumentsException;
import com.sun.dex.core.util.HexStrUtil;

public class EncDecHelper {

    public static final String[] CIPHERS = new String[]{
            //android 8.0 +
//            "AES_128/ECB/NOPADDING",
//            "AES_128/CBC/NOPADDING",
//            "AES_128/GCM/NOPADDING",
//            "AES_256/ECB/NOPADDING",
//            "AES_256/CBC/NOPADDING",
//            "AES_256/GCM/NOPADDING",

            //keySize: 40~2048 bits
//            "ARC4",  // PC don't have it

            //加密后会增加16byte数据（不添加AAD时，HMAC）
            "AES/GCM/NOPADDING", //12 bits IV

            "AES/ECB/NOPADDING",
            "AES/CBC/NOPADDING", //16 bits IV
            "AES/CTR/NOPADDING", //16 bits IV
            "AES/CTS/NOPADDING", //16 bits IV
            "AES/CFB/NOPADDING", //16 bits IV
            "AES/OFB/NOPADDING", //16 bits IV

            //blockSize: 64-bits
            //keySize: 32-bits to 448-bits variable size
            //加密后会增加8byte数据
            "BLOWFISH",

            //keySize: 64 bits
            //加密后会增加8byte数据
            "DES",

            //keySize: 192 bits
            //加密后会增加8byte数据
            "DESEDE"

//            "PBEWITHMD5ANDDES"
    };

    /**
     *
     * @param cipherIndex  index of CIPHERS[]
     * @return
     */
    public static Cipher getEncCipher(int cipherIndex, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidArgumentsException {
        return getCipher(cipherIndex, key, true);
    }

    public static Cipher getDecCipher(int cipherIndex, String key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidArgumentsException {
        return getCipher(cipherIndex, key, false);
    }

    public static byte[] encrypt(Cipher cipher, byte[] in) throws BadPaddingException, IllegalBlockSizeException {
        return cipher.doFinal(in, 0, in.length);
    }

    public static byte[] encrypt(Cipher cipher, byte[] in, int size) throws BadPaddingException, IllegalBlockSizeException {
        return cipher.doFinal(in, 0, size);
    }

    public static byte[] encrypt(Cipher cipher, byte[] in, int position, int size) throws BadPaddingException, IllegalBlockSizeException {
        return cipher.doFinal(in, position, size);
    }

    public static byte[] decrypt(Cipher cipher, byte[] in) throws BadPaddingException, IllegalBlockSizeException {
        return cipher.doFinal(in, 0, in.length);
    }

    public static byte[] decrypt(Cipher cipher, byte[] in, int size) throws BadPaddingException, IllegalBlockSizeException {
        return cipher.doFinal(in, 0, size);
    }

    public static byte[] decrypt(Cipher cipher, byte[] in, int position, int size) throws BadPaddingException, IllegalBlockSizeException {
        return cipher.doFinal(in, position, size);
    }

    public static Cipher getCipher(int cipherIndex, String key, boolean isEnc) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidArgumentsException {
        if(cipherIndex < 0 || cipherIndex >= CIPHERS.length)
            throw new InvalidArgumentsException("cipherIndex < 0 || cipherIndex >= CIPHERS.length");

        ICipherBuilder cipherBuilder;
        if(CIPHERS[cipherIndex].equals("PBEWITHMD5ANDDES"))
            cipherBuilder = new PBEWithMD5AndDESCipherBuilder();
        else
            cipherBuilder = new AesDesBlowfishArc4CipherBuilder();
        cipherBuilder.init(CIPHERS[cipherIndex], key, isEnc);
        if(Switch.LOG_ON) {
            System.out.println("key1= " + HexStrUtil.toHexString(cipherBuilder.getKey1()));
            System.out.println("key2= " + HexStrUtil.toHexString(cipherBuilder.getKey2()));
        }
        return cipherBuilder.build();
    }

    public static int getCipherIndexByKey(String key) {
        Random r = new Random(DiscreteMapper.getInstance().map2Long(key));
        return r.nextInt(CIPHERS.length);
    }

    public static Cipher getCipherByKey(String key, boolean isEnc) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidArgumentsException {
        return getCipher(getCipherIndexByKey(key), key, isEnc);
    }

    public static boolean isNeedPadding(String alg){
        return alg.endsWith("NOPADDING");
    }

    public static boolean isNeedPadding(int cipherIndex){
        return isNeedPadding(CIPHERS[cipherIndex]);
    }

    public static int getAutoPaddingSize(String alg){
        int size = 0;
        if(!alg.endsWith("NOPADDING"))
            size = 8;  //PADDING PKCS5
        return size;
    }

    public static int getAADSize(String alg){
        int size = 0;
        if(alg.contains("AES/GCM/"))
            size = 16; //HMAC?
        return size;
    }

    public static int getAADSize(int cipherIndex){
        return getAADSize(CIPHERS[cipherIndex]);
    }

    public static int getAutoPaddingSize(int cipherIndex){
        return getAutoPaddingSize(CIPHERS[cipherIndex]);
    }

    public static int getAutoPaddingSize(Cipher cipher){
        return getAutoPaddingSize(cipher.getAlgorithm());
    }
}
