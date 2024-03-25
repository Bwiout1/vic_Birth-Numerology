package com.sun.dex.core.util;

import java.util.ArrayList;
import java.util.List;

public class DataSegmentUtil {
    /**
     * 将数据大小（byteArraySize）按 块大小（blockSize） 平均（整数倍， 不足一个blockSize的部分，属于最后一个段）分到每个段上，返回每个数据段的起始下标
     * 数据大小平均分为maxSegmentCount 后，若单个段所分得的数据大小不足 blockSize 时，将减少返回的 数据段 的个数
     * @param maxSegmentCount
     * @param blockSize
     * @param byteArraySize
     * @return
     */
    public static List<Integer> getSegmentsIndex(final int maxSegmentCount, final int blockSize, final int byteArraySize) {
        if(maxSegmentCount <= 0)
            throw new IllegalArgumentException("maxSegmentCount <= 0");
        if(blockSize <= 0)
            throw new IllegalArgumentException("blockSize <= 0");
        if(byteArraySize <= 0)
            throw new IllegalArgumentException("byteArraySize <= 0");


        final List<Integer> segmentsIndex = new ArrayList<>();
        final int segmentSize = byteArraySize / maxSegmentCount;

        if (segmentSize < blockSize) {
            segmentsIndex.add(0);

            //不足一个blockSize的，属于最后一个segment
            int index = blockSize;
            while (byteArraySize - blockSize >= index) {
                segmentsIndex.add(index);
                index += blockSize;
            }
        } else {
            //不足一个blockSize的，属于最后一个segment
            final int blockCount = byteArraySize / blockSize;
            final int remainSize = byteArraySize - blockCount * blockSize;

            final int segmentBlockCount = blockCount / maxSegmentCount;
            int remainSegmentBlockCount = blockCount - segmentBlockCount * maxSegmentCount;
            //将不足段数（remainSegmentBlockCount）的 remainSegmentBlockCount，尽可能整分到每个段里，不够的则少分

            segmentsIndex.add(0);
            final int segmentSize2 = segmentBlockCount * blockSize;
            int remainSegmentBlockOffset = 0;
            for(int i = 1; i < maxSegmentCount; i++){
                int index = i * segmentSize2 + remainSegmentBlockOffset;
                if(remainSegmentBlockCount > 0) {
                    remainSegmentBlockOffset += blockSize;
                    index += blockSize;
                    remainSegmentBlockCount--;
                }
                segmentsIndex.add(index);
            }
        }

        return segmentsIndex;
    }
}
