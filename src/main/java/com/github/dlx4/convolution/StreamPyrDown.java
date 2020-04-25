package com.github.dlx4.convolution;

import java.util.Random;

/**
 * 卷积流处理应用  下采样
 *
 * @author dinglingxiang
 */
public class StreamPyrDown extends StreamConvolution {

    public StreamPyrDown(StreamFIFO<Integer> src, StreamFIFO<Integer> dst, int winSize, int rows, int cols) {
        super(src, dst, winSize, rows, cols);
    }

    public static void main(String[] args) {
        int rows = 20;
        int cols = 16;
        Random random = new Random();

        Integer[][] srcData = new Integer[rows][cols];
        Integer[][] dstData = new Integer[rows / 2][cols / 2];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                srcData[i][j] = cols * i + j;
            }
        }

        for (int i = 0; i < rows / 2; i++) {
            for (int j = 0; j < cols / 2; j++) {
                dstData[i][j] = 0;
            }
        }

        Mat<Integer> src = new Mat<>(rows, cols, srcData);
        Mat<Integer> dst = new Mat<>(rows / 2, cols / 2, dstData);

        System.out.println(src);
        streamPyrDown(src, dst, rows, cols);
        System.out.println(dst);
    }

    /**
     * @param src
     * @param dst
     * @param rows
     * @param cols
     * @return void
     */
    public static void streamPyrDown(Mat<Integer> src, Mat<Integer> dst, int rows, int cols) {
        StreamFIFO<Integer> filterIn = new StreamFIFO<>();
        StreamFIFO<Integer> filterOut = new StreamFIFO<>();

        int readPointer = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                filterIn.write(src.read(readPointer));
                readPointer++;
            }
        }

        StreamPyrDown streamPyrDown = new StreamPyrDown(filterIn, filterOut, 5, rows, cols);
        streamPyrDown.doConvolution();

        int writePointer = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Integer b = filterOut.read();
                if (i % 2 == 0 && j % 2 == 0) {
                    dst.write(writePointer, b);
                    writePointer++;
                }
            }
        }

        System.out.println(filterIn.toString());
        System.out.println(filterOut.toString());
    }

    /**
     * 计算窗口数据
     *
     * @param window
     * @param winSize
     * @return java.lang.Integer
     */
    @Override
    Integer windowCalculate(Integer[][] window, int winSize) {
        int result = 0;
        for (int i = 0; i < winSize; i++) {
            for (int j = 0; j < winSize; j++) {
                result += window[i][j];
            }
        }
        return result / (winSize * winSize);
    }


}
