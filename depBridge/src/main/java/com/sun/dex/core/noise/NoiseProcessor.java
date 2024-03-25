package com.sun.dex.core.noise;


import java.util.Arrays;
import java.util.Random;

import com.sun.dex.core.exception.NoSpaceException;

public class NoiseProcessor {
    private long key;
    public final int TAIL_SIZE = 4; //for storage noiseSize in noisedData[]

    public NoiseProcessor(long key) {
        this.key = key;
    }

    public void init(long seed) {
        this.key = seed;
    }

    /**
     * 将dataSize膨胀到 [dataSize * (1+minSizeProbability) ~ dataSize * (1+maxSizeProbability)] 之间
     *
     * @param data
     * @param minSizeProbability
     * @param maxSizeProbability
     * @return
     */
    public byte[] addNoise(byte[] data, int minSizeProbability, int maxSizeProbability) throws NoSpaceException {
        if (maxSizeProbability < minSizeProbability || minSizeProbability < 0) {
            throw new IllegalArgumentException("maxSizeProbability < minSizeProbability || minSizeProbability < 0");
        }

        Random r = new Random(key);
        int noiseSize = (int) (((float) (r.nextInt(maxSizeProbability - minSizeProbability) + minSizeProbability) / 100) * data.length);
        if (noiseSize < 0 || data.length + noiseSize < 0)
            noiseSize = Integer.MAX_VALUE - data.length - TAIL_SIZE;
        if (noiseSize == 0)
            noiseSize = 1;
        return addNoise(data, noiseSize);
    }

    /**
     * TODO 用C实现
     *
     * @param data      原始数据
     * @param noiseSize 辣鸡字节大小, 至少 TAIL_SIZE + 1
     * @return
     * @throws NoSpaceException data.length + noiseSize > Integer.MAX_VALUE 时表示空间不够
     */
    public byte[] addNoise(byte[] data, int noiseSize) throws NoSpaceException {
        //TODO 1. RM TAIL_SIZE -> (data.len + noiseSize) find noiseSize position
        //TODO 2. add some  MAGIC + key(seed, hash, xor, ... 签名)  to check this is a valid noisedData
        //3. keep insert
        if (noiseSize <= TAIL_SIZE || null == data) {
            throw new IllegalArgumentException("noiseSize <= " + TAIL_SIZE + " || null == data");
        }
        if (data.length + noiseSize <= 0) {
            throw new NoSpaceException("data.length + noiseSize > Integer.MAX_VALUE");
        }

        //for out-of-order execute
        final Random r1 = new Random(key);
        final Random r2 = new Random(r1.nextInt());

        final Random sr1 = new Random();
        final Random sr2 = new Random();

        final int noiseSizeMask = r1.nextInt();
        final int dataLen = data.length;
        final int halfNoiseSize = noiseSize >> 1;

        byte[] noisedData = new byte[dataLen + noiseSize + TAIL_SIZE]; //+4 = noiseSize
        final int[] noisePositions = new int[noiseSize];//0~remaining data size

        //add noise in data[] position (maybe have repeat position,become cluster)
        for (int i = 0; i < halfNoiseSize; i++) {
            noisePositions[i * 2] = r1.nextInt(dataLen); //todo dataLen+1
            noisePositions[i * 2 + 1] = r2.nextInt(dataLen);
        }
        if ((noiseSize & 1) == 1) {
            noisePositions[noiseSize - 1] = r1.nextInt(dataLen);
        }

        Arrays.sort(noisePositions);

        //add noise and copy org data
        int lastP = 0;
        int lastPNoise = 0;
        int i = 0;
        for (i = 0; i < halfNoiseSize; i++) {
            int p1 = noisePositions[i * 2];
            int p2 = noisePositions[i * 2 + 1];

            //position in noisedData[]
            int p1Noise = p1 + i * 2;
            int p2Noise = p2 + i * 2 + 1;

            //add noise
            noisedData[p1Noise] = (byte) sr1.nextInt(256);
            noisedData[p2Noise] = (byte) sr2.nextInt(256);

            //(p1 - lastP2) maybe equal 0
            //copy data[lastP2 ~ p1] -> noisedData[lastP2Noise ~ (p1-lastP2)]
            System.arraycopy(data, lastP, noisedData, lastPNoise + 1, p1 - lastP);
            System.arraycopy(data, p1, noisedData, p1Noise + 1, p2 - p1);

            lastP = p2;
            lastPNoise = p2Noise;
        }
        if ((noiseSize & 1) == 1) {
            int p1 = noisePositions[i * 2]; //noiseSize-1

            //position in noisedData[]
            int p1Noise = p1 + i * 2;

            //add noise
            noisedData[p1Noise] = (byte) sr1.nextInt(256);

            //(p1 - lastP) maybe equal 0
            //copy data[lastP ~ p1] -> noisedData[lastPNoise ~ (p1-lastP)]
            System.arraycopy(data, lastP, noisedData, lastPNoise + 1, p1 - lastP);

            lastP = p1;
            lastPNoise = p1Noise;
        }
        System.arraycopy(data, lastP, noisedData, lastPNoise + 1, dataLen - lastP);

        //put ~noiseSize to tail
        noiseSize ^= noiseSizeMask;
        noisedData[noisedData.length - 4] = (byte) (noiseSize & 0xFF);
        noisedData[noisedData.length - 3] = (byte) ((noiseSize >> 8) & 0xFF);
        noisedData[noisedData.length - 2] = (byte) ((noiseSize >> 16) & 0xFF);
        noisedData[noisedData.length - 1] = (byte) ((noiseSize >> 24) & 0xFF);

        return noisedData;
    }

    public byte[] removeNoise(byte[] noisedData) throws InvalidNoiseDataException {
        if (null == noisedData || noisedData.length <= TAIL_SIZE)
            throw new InvalidNoiseDataException("null == noisedData || noisedData.length <= " + TAIL_SIZE);

        //for out-of-order execute
        final Random r1 = new Random(key);
        final Random r2 = new Random(r1.nextInt());

        final int noiseSizeMask = r1.nextInt();

        try {
            //1. get noiseSize from tail
            int noiseSize = 0;
            noiseSize = noisedData[noisedData.length - 4] & 0xFF;
            noiseSize |= (noisedData[noisedData.length - 3] & 0xFF) << 8;
            noiseSize |= (noisedData[noisedData.length - 2] & 0xFF) << 16;
            noiseSize |= (noisedData[noisedData.length - 1] & 0xFF) << 24;
            noiseSize ^= noiseSizeMask;
            if (noiseSize <= 0) //invalid data
                throw new InvalidNoiseDataException("invalid noiseSize");

            final int noisedDataLen = noisedData.length - TAIL_SIZE;
            final int halfNoiseSize = noiseSize >> 1;
            final int orgDataLen = noisedDataLen - noiseSize;

            final byte[] orgData = new byte[orgDataLen];

            final int[] noisePositions = new int[noiseSize];//0~remaining data size
            //2. noise in orgData[] position (maybe have repeat position,become cluster)
            for (int i = 0; i < halfNoiseSize; i++) {
                noisePositions[i * 2] = r1.nextInt(orgDataLen);
                noisePositions[i * 2 + 1] = r2.nextInt(orgDataLen);
            }
            if ((noiseSize & 1) == 1) {
                noisePositions[noiseSize - 1] = r1.nextInt(orgDataLen);
            }

            Arrays.sort(noisePositions);

            //3. add/remove/skip noise and copy valid data
            int lastP = 0;
            int lastPNoise = 0;
            int i = 0;
            for (i = 0; i < halfNoiseSize; i++) {
                int p1 = noisePositions[i * 2];
                int p2 = noisePositions[i * 2 + 1];

                //position in noisedData[]
                int p1Noise = p1 + i * 2;
                int p2Noise = p2 + i * 2 + 1;

                //skip noise

                //(p1 - lastP2) maybe equal 0
                //copy noisedData[lastPNoise ~ (p1-lastP)] -> data[lastP ~ p1]
                int p1Len = p1 - lastP;
                System.arraycopy(noisedData, lastPNoise + 1, orgData, lastP, p1Len);
                int p2Len = p2 - p1;
                System.arraycopy(noisedData, p1Noise + 1, orgData, p1, p2Len);

                lastP = p2;
                lastPNoise = p2Noise;
            }
            if ((noiseSize & 1) == 1) {
                int p1 = noisePositions[i * 2];

                //position in noisedData[]
                int p1Noise = p1 + i * 2;

                //skip noise

                //(p1 - lastP2) maybe equal 0
                //copy noisedData[lastPNoise ~ (p1-lastP)] -> data[lastP ~ p1]
                System.arraycopy(noisedData, lastPNoise + 1, orgData, lastP, p1 - lastP);

                lastP = p1;
                lastPNoise = p1Noise;
            }
            System.arraycopy(noisedData, lastPNoise + 1, orgData, lastP, orgDataLen - lastP);

            return orgData;
        } catch (Exception e) {
            throw new InvalidNoiseDataException(e);
        }
    }

    private void arrayCopy(byte[] src, int srcOffset, byte[] dst, int dstOffset, int len) {
        if(len < 200){
            final int hlen = len>>1;
            for(int j = 0; j < hlen; j++){
                dst[srcOffset+j] = src[dstOffset + j];
                dst[srcOffset+j+1] = src[dstOffset + j+1];
            }
            if((len & 1) == 1){
                dst[srcOffset + len - 1] = src[dstOffset + len - 1];
            }
        }else{
            System.arraycopy(src, srcOffset, dst, dstOffset, len);
        }
    }

}
