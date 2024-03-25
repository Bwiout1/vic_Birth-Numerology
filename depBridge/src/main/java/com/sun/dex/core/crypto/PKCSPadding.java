package com.sun.dex.core.crypto;

import javax.crypto.Cipher;

public class PKCSPadding {

    public static int calcAddPaddingSize(int dataSize, int blockByteSize) {
        int newDataLen = (dataSize / blockByteSize + 1) * blockByteSize;
        int paddingByte = (newDataLen - dataSize);
        return paddingByte;
    }

    public static int calcAddPaddingSize(int dataSize, Cipher cipher) {
        return calcAddPaddingSize(dataSize, getPaddingBlockSize(cipher));
    }

    public static byte[] addPadding(byte[] data, int position, int size, int blockByteSize) throws IllegalArgumentException {
        if (blockByteSize <= 1)
            return data;

        if (data == null || blockByteSize > 0xFF || Integer.MAX_VALUE - size < blockByteSize)
            throw new IllegalArgumentException("data == null || blockByteSize > 0xFF || size + blockByteSize > Integer.MAX_VALUE");

        int newDataLen = (size / blockByteSize + 1) * blockByteSize;
        byte paddingByte = (byte) (newDataLen - size);
        byte[] newData = new byte[newDataLen];
        System.arraycopy(data, position, newData, 0, size);
        for (int i = size; i < newDataLen; i++) {
            newData[i] = paddingByte;
        }
        return newData;
    }

    //类似PKCS5Padding, 但块大小为自定义大小，不超过255字节
    //单次最大支持2G多点的数据
    public static byte[] addPadding(byte[] data, int blockByteSize) throws IllegalArgumentException {
        return addPadding(data, 0, data.length, blockByteSize);
    }

    public static byte[] addPadding(byte[] data, Cipher cipher) throws IllegalArgumentException {
        return addPadding(data, 0, data.length,
                getPaddingBlockSize(cipher));
    }

    public static byte calcRemovePaddingSize(byte lastByte) {
        return lastByte;
    }

    public static byte[] removePadding(byte[] data, int position, int size, int blockSize) {
        if (blockSize <= 1)
            return data;

        if (data == null || blockSize > 0xFF || size < blockSize)
            throw new IllegalArgumentException("data == null || blockSize > 0xFF || size < blockSize");

        byte paddingByte = data[size - 1];
        int newDataLen = size - paddingByte;
        byte[] newData = new byte[newDataLen];
        System.arraycopy(data, position, newData, 0, newDataLen);
        return newData;
    }

    public static byte[] removePadding(byte[] data, int blockSize) {
        return removePadding(data, 0, data.length, blockSize);
    }

    public static byte[] removePadding(byte[] data, Cipher cipher) {
        return removePadding(data, 0, data.length,
                getPaddingBlockSize(cipher));
    }

    //for AES cipher.getBlockSize() return 8. but AES need 16 paddingBlockSize;
    public static int getPaddingBlockSize(Cipher cipher){
//        return Math.max(PaddingUtil.getAlgPaddingSize(cipher.getAlgorithm()), cipher.getBlockSize());
        return cipher.getBlockSize();
    }
}
