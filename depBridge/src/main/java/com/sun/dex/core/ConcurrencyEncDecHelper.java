package com.sun.dex.core;



import com.Switch;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import com.sun.dex.core.crypto.DiscreteMapper;
import com.sun.dex.core.crypto.PKCSPadding;
import com.sun.dex.core.exception.InvalidArgumentsException;
import com.sun.dex.core.exception.NoSpaceException;
import com.sun.dex.core.util.DataSegmentUtil;
import com.sun.dex.core.util.HexStrUtil;

/**
 * 使用该类加密的数据，也需要使用该类解密，除非 concurrentCont = 1 时，可以使用 CryptoUtil 来加解密
 * 对同一个加密的数据 data，解密时这些参数需要使用相同的:key, dataSegmentCount. 可以根据机器调整: concurrentCont
 */
public class ConcurrencyEncDecHelper {
    public static byte[] addPaddingAndEnc(String key, byte[] data) throws InterruptedException, ExecutionException, InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidArgumentsException, IllegalBlockSizeException, BadPaddingException, NoSpaceException {
        return encDecAndPadding(key, data, 32, Runtime.getRuntime().availableProcessors() + 1, true);
    }

    public static byte[] decAndRemovePadding(String key, byte[] data) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, ExecutionException, InvalidKeyException, InterruptedException, IllegalBlockSizeException, BadPaddingException, InvalidArgumentsException, NoSpaceException {
        return encDecAndPadding(key, data, 32, Runtime.getRuntime().availableProcessors() + 1, false);
    }

    public static byte[] encDecAndPadding(String key, byte[] data, int dataSegmentCount, int concurrentCont, boolean isEnc) throws InterruptedException, ExecutionException, InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidArgumentsException, NoSpaceException {
        if (null == key)
            throw new InvalidArgumentsException("key == null");

        int cipherIndex = EncDecHelper.getCipherIndexByKey(key);
//        int cipherIndex = 1;
        if (Switch.LOG_ON)
            System.out.println("CIPHERS = " + EncDecHelper.CIPHERS[cipherIndex]);
        return encDecAndPadding(key, cipherIndex, data, dataSegmentCount, concurrentCont, isEnc);
    }

    //TODO 数组的操作（复制，增减大小）用C实现

    /**
     * 异常: 加密后的数据会因加入对齐、签名等其他数据，增加大小。当超过 byte[] array的最大大小 Integer.MAX_VALUE 时，会造成加密失败
     * 若要对临近超过2G的数据进行加密，可以将原始数据按每段1.9G 分割后，逐个加密的方式解决。
     *
     * @param key
     * @param cipherIndex
     * @param data
     * @param dataSegmentCount 加密必填，解密可以不填
     * @param concurrentCont
     * @param isEnc
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws ShortBufferException
     * @throws BadPaddingException
     * @throws InvalidArgumentsException
     * @throws NoSpaceException
     */
    public static byte[] encDecAndPadding(String key, int cipherIndex, byte[] data, int dataSegmentCount, int concurrentCont, boolean isEnc) throws InterruptedException, ExecutionException, InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidArgumentsException, NoSpaceException {
        //check exception
        if (isEnc && dataSegmentCount <= 0)
            throw new InvalidArgumentsException("dataSegmentCount <= 0");
        if (data == null)
            throw new InvalidArgumentsException("data == null || data.length == 0");
        if (data.length == 0)
            return data;

        //1.根据key选择加密算法和cipher
        //2.根据分段数生成header，并设置分段数到header，然后将header加padding，然后加密，然后放入resultData
        //3.对整个data根据 cipher block 加padding
        //4.将data根据分段数分段
        //5.并发加解密
        //6.合并分段的加解密结果并返回

        //1.根据key选择加密算法和cipher
        Cipher cipher = EncDecHelper.getCipher(cipherIndex, key, isEnc);
        int segSizeAdj = EncDecHelper.getAutoPaddingSize(cipherIndex)
                + EncDecHelper.getAADSize(cipherIndex);

        Header header = new Header();
        int headerSize = Header.getHeaderSize(cipher);
        int resultDataSize;

        byte[] resultData = new byte[0];
        byte[] decEncInputData;
        List<Integer> segIndexList;

        if (isEnc) {
            int addPaddingDataSize = PKCSPadding.calcAddPaddingSize(data.length, cipher);
            addPaddingDataSize += data.length;
            if (addPaddingDataSize < 0)
                throw new NoSpaceException("no enough array size for add padding.");
//5242896
            decEncInputData = PKCSPadding.addPadding(data, cipher);//此时decEncInputData有整数个block
            segIndexList = DataSegmentUtil.getSegmentsIndex(dataSegmentCount, cipher.getBlockSize(), decEncInputData.length);
            header.setSegCount(segIndexList.size());
            byte[] headerBytes = header.enc(cipher);
            resultDataSize = headerSize + decEncInputData.length + segIndexList.size() * segSizeAdj;
            if (resultDataSize < 0)
                throw new NoSpaceException("no enough array size for merge encrypted all data segment.");

            resultData = new byte[resultDataSize];
            System.arraycopy(headerBytes, 0, resultData, 0, headerSize);
        } else {
            //TODO 下个版本:去掉头部，从加密后数据得到正确的段索引
            header.dec(cipher, data, 0);
            //this size contain self-padding size
            dataSegmentCount = header.getSegCount();
            resultDataSize = data.length - headerSize - dataSegmentCount * segSizeAdj;
            segIndexList = DataSegmentUtil.getSegmentsIndex(dataSegmentCount, cipher.getBlockSize(), resultDataSize);
            decEncInputData = data;
        }

        //dataSegmentCount
        int tailSegIndex = segIndexList.get(segIndexList.size() - 1);
        int paddingDataLen = isEnc ? decEncInputData.length : resultDataSize;

        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(segIndexList.size(), Math.max(1, concurrentCont)));
        List<Future<TaskResultData>> taskList = new LinkedList<>();
        long startWaitTime = System.currentTimeMillis();
        //从最后一个段开始
        for (int i = segIndexList.size() - 1; i >= 0; i--) { //逆序：解密padding
            final int finalSegIndex = i;
            final int byteIndex = segIndexList.get(i);
            final int endByteIndex = (i < segIndexList.size() - 1) ? segIndexList.get(i + 1) : paddingDataLen;
            final int segmentDataSize = endByteIndex - byteIndex;
            final Cipher cipherForSeg = EncDecHelper.getCipher(cipherIndex/*i*/, key, isEnc);

            Future<TaskResultData> future = executorService.submit(() -> {
                long startTime = System.currentTimeMillis();
                byte[] encDecSegData;
                if (isEnc) {
                    if(Switch.LOG_ON){
                        byte[] tmp = new byte[segmentDataSize];
                        System.arraycopy(decEncInputData, byteIndex, tmp, 0, tmp.length);
                        System.out.println("1 dec segment:     " + (byteIndex) + " + "+tmp.length+"  ,MD5: " + HexStrUtil.toHexString(DiscreteMapper.getInstance().map2Bytes(tmp)) + ", decDataLen: " + segmentDataSize);
                    }

                    encDecSegData = EncDecHelper.encrypt(cipherForSeg, decEncInputData, byteIndex, segmentDataSize);

                    if(Switch.LOG_ON){
                        System.out.println("1 enc segment:     " + (byteIndex) + " + " + segmentDataSize+"  ,MD5: " + HexStrUtil.toHexString(DiscreteMapper.getInstance().map2Bytes(encDecSegData)) + ", encDataLen: " + encDecSegData.length);
                        //For GCM
                        Cipher testCipherForSeg = EncDecHelper.getCipher(cipherIndex/*i*/, key, false);
                        byte[] testDecArr = EncDecHelper.decrypt(testCipherForSeg, encDecSegData);
                        System.out.println("1 testDec segment: " + (byteIndex) + " + "+segmentDataSize+"  ,MD5: " + HexStrUtil.toHexString(DiscreteMapper.getInstance().map2Bytes(testDecArr)));
                    }
                } else {
                    int segsSizeOffset = finalSegIndex * segSizeAdj;
                    int srcOffsetIndex = headerSize + segsSizeOffset + byteIndex;
                    int srcSegmentLen = segmentDataSize + segSizeAdj;
                    if(Switch.LOG_ON){
                        byte[] tmp = new byte[srcSegmentLen];
                        System.arraycopy(decEncInputData, srcOffsetIndex, tmp, 0, tmp.length);
                        System.out.println("2 enc segment: " + srcOffsetIndex + " + "+tmp.length+"  ,MD5: " + HexStrUtil.toHexString(DiscreteMapper.getInstance().map2Bytes(tmp)) + ", encDataLen: " + srcSegmentLen);
                    }

                    encDecSegData = EncDecHelper.decrypt(cipherForSeg, decEncInputData, srcOffsetIndex, srcSegmentLen);

                    if(Switch.LOG_ON)
                        System.out.println("2 dec segment: " + srcOffsetIndex + " + "+srcSegmentLen+"  ,MD5: " + HexStrUtil.toHexString(DiscreteMapper.getInstance().map2Bytes(encDecSegData)) + ", decDataLen: " + encDecSegData.length);
                }
                //TODO thread safe? in difference address write only
                //need volatile in last output (merge all thread cache)
//                System.arraycopy(encDecSegData, 0, decEncOutputData, index, afterPaddingSize);
                long endTime = System.currentTimeMillis();

                TaskResultData taskResultData = new TaskResultData();
                taskResultData.segIndex = finalSegIndex;
                taskResultData.segSize = segmentDataSize;
                taskResultData.byteIndex = byteIndex;
                taskResultData.useTime = (endTime - startTime);
                taskResultData.resultData = encDecSegData;

                if (false)
                    System.out.println("ConcurrencyEncDecHelper - " +
                            (isEnc ? "Enc " : "Dec ") +
                            "I: " + finalSegIndex + "/" + segIndexList.size() +
                            ", dataRange: " + byteIndex + " ~ " + endByteIndex + " = " + segmentDataSize +
//                            ", copyArrToSegDataTimeUse: " + (copy1EndTime - startTime) +
//                            ", encDecTimeUse: " + (endEncDecTime - copy1EndTime) +
//                            ", encDecTimeUse: " + (endTime - startTime) +
//                            ", copyArrFromSegDataTimeUse: " + (endTime - endEncDecTime) +
                            ", allTimeUse: " + (endTime - startTime));
                return taskResultData;
            });

            taskList.add(future);
        }


        //merge all enc/decrypted segment data to resultData[]
        //List<TaskResultData> resultDataList = new ArrayList<>();
        //int allSegSize = 0;
        int decRemovePaddingDataSize = -1;
        long addTime = 0;
        //get tailSegment task first
        int processTaskCounter = 0;
        while (taskList.size() > 0) {
            Future<TaskResultData> task = taskList.get(0);
            try {
                TaskResultData taskResultData = task.get();
                addTime += taskResultData.useTime;
                //allSegSize += taskResultData.resultData.length;
                // resultDataList.add(taskResultData);

                if (isEnc) {
                    //taskResultData.resultData.length - taskResultData.segSize
                    //AES/GCM = 16 ,BLOWFISH = 8, DES = 8, DESEDE = 8
                    int segsSizeOffset = taskResultData.segIndex * segSizeAdj;
                    System.arraycopy(taskResultData.resultData, 0, resultData, headerSize + segsSizeOffset + taskResultData.byteIndex, taskResultData.resultData.length);
                    taskList.remove(task);
                } else {
                    if (decRemovePaddingDataSize > -1) {
                        System.arraycopy(taskResultData.resultData, 0, resultData, taskResultData.byteIndex, taskResultData.resultData.length);
                        taskResultData.resultData = null;
                        taskList.remove(task);
                    } else if (taskResultData.byteIndex == tailSegIndex) {
                        //last segment have padding, remove paddingSize after get correct realOutputDataArraySize
                        //process last segment
                        byte removePaddingBytes = PKCSPadding.calcRemovePaddingSize(taskResultData.resultData[taskResultData.resultData.length - 1]);
                        decRemovePaddingDataSize = resultDataSize - removePaddingBytes;
                        resultData = new byte[decRemovePaddingDataSize];

                        System.arraycopy(taskResultData.resultData, 0, resultData, taskResultData.byteIndex, taskResultData.resultData.length - removePaddingBytes);
                        taskResultData.resultData = null;
                        taskList.remove(task);

                        //合并在此之前其他已完成的segment
                        for (int ti = 0; ti < taskList.size(); ) {
                            Future<TaskResultData> otherTask = taskList.get(ti);
                            //并发?
                            if(!otherTask.isDone()) {
                                ti++;
                                continue;
                            }
                            TaskResultData otherTaskResultData = otherTask.get();
                            System.arraycopy(otherTaskResultData.resultData, 0, resultData, otherTaskResultData.byteIndex, otherTaskResultData.resultData.length);
                            otherTaskResultData.resultData = null;
                            taskList.remove(otherTask);
                        }
                    }
                }

                if((processTaskCounter & 0x3) == 0)
                    System.gc();
                processTaskCounter++;
            } catch (InterruptedException | ExecutionException e) {
                //e.printStackTrace();
                executorService.shutdown();
                throw e;
            }
        }

        try {
            executorService.shutdown();
        } catch (Exception ignore) {
        }

        long waitTime = System.currentTimeMillis() - startWaitTime;
        if (Switch.LOG_ON)
            System.out.println("END - ConcurrencyEncDecHelper : waitTime = " + waitTime + ", addTime = " + addTime);

        return resultData;
    }

    private static class TaskResultData {
        int segIndex;
        int segSize;
        int byteIndex;
        long useTime;
        byte[] resultData;

    }

    private static class Header {
        private int segCount;

        public static int getHeaderSize(Cipher cipher) {
            int dataSize = 4;
            int blockSize = cipher.getBlockSize();

            int selfPaddingSize = PKCSPadding.calcAddPaddingSize(dataSize, blockSize);
            dataSize += selfPaddingSize;

            int autoPaddingSize = EncDecHelper.getAutoPaddingSize(cipher.getAlgorithm());
            int aadSize = EncDecHelper.getAADSize(cipher.getAlgorithm());
            return dataSize + autoPaddingSize + aadSize;
        }

        private byte[] seq() {
            return new byte[]{(byte) (segCount & 0xFF), (byte) ((segCount >> 8) & 0xFF),
                    (byte) ((segCount >> 16) & 0xFF), (byte) ((segCount >> 24) & 0xFF)};
        }

        private void inSeq(byte[] seq) {
            segCount = seq[0] | (seq[1] << 8) | (seq[1] << 16) | (seq[1] << 24);
        }

        public byte[] enc(Cipher cipher) throws IllegalBlockSizeException, BadPaddingException {
            byte[] data = seq();
            byte[] pData = PKCSPadding.addPadding(data, cipher);
            byte[] encData = EncDecHelper.encrypt(cipher, pData);
            if (encData.length != getHeaderSize(cipher) && Switch.LOG_ON)
                System.err.println("encData.length != getHeaderSize(cipher) : " + encData.length + " != " + getHeaderSize(cipher));
            return encData;
        }

        public void dec(Cipher cipher, byte[] in, int position) throws IllegalBlockSizeException, BadPaddingException {
            byte[] decData = EncDecHelper.decrypt(cipher, in, position, getHeaderSize(cipher));
            byte[] dpData = PKCSPadding.removePadding(decData, cipher);
            inSeq(dpData);
        }

        public int getSegCount() {
            return segCount;
        }

        public void setSegCount(int segCount) {
            this.segCount = segCount;
        }
    }
}
