package com.github.dlx4.convolution;

public class PyrDown {

    public static void xFpyrDownKernel(Mat<Byte> src, Mat<Byte> dst, int rows, int cols) {
        StreamFIFO<Byte> filterIn = new StreamFIFO<>();
        StreamFIFO<Byte> filterOut = new StreamFIFO<>();

        int readPointer = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                filterIn.write(src.read(readPointer));
                readPointer++;
            }
        }

        int writePointer = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Byte b = filterOut.read();
                if (i % 2 == 0 && j % 2 == 0) {
                    dst.write(writePointer, b);
                    readPointer++;
                }
            }
        }
    }

    private static void xFPyrDownGaussianBlur(StreamFIFO<Byte> src, StreamFIFO<Byte> dst,
                                              int winSize,
                                              int borderType,
                                              int rows,
                                              int cols) {
        int[] rowIndex = new int[winSize];
        int bufSize = winSize;
        Integer shiftX = 0;
        // int row, col;

        Byte[] outputValues = new Byte[1];
        Byte[][] srcBuf = new Byte[winSize][winSize];

        Byte p0;
        Byte[][] buf = new Byte[winSize][cols];

        // 如果winSize是5 这里就是初始化为 0 1 2 3 4
        for (int initRowIndex = 0; initRowIndex < winSize; initRowIndex++) {
            rowIndex[initRowIndex] = initRowIndex;
        }

        // 如果winSize是5 这里循环2 3 4
        for (int initBuf = rowIndex[winSize >> 1]; initBuf < rowIndex[winSize - 1]; initBuf++) {
            for (int col = 0; col < cols; col++) {
                buf[initBuf][col] = src.read();
            }
        }

        // 处理边界 如果winSize是5 这里循环 0 1
        for (int col = 0; col < cols; col++) {
            for (int initBuf = 0; initBuf < winSize >> 1; initBuf++) {
                buf[initBuf][col] = buf[rowIndex[winSize >> 1]][col];
            }
        }

        // 遍历所有行
        for (int row = (winSize >> 1); row < rows + (winSize >> 1); row++) {
            p0 = 0;
            xFPyrDownprocessgaussian(src, dst, buf, srcBuf, outputValues, p0, rows, cols, shiftX, rowIndex, row, winSize);
            // 下面实现的效果就是从0 1 2 3 4 变成 1 2 3 4 0
            int zeroIndex = rowIndex[0];
            // 如果winSize是5 这里循环0 1 2 3
            for (int initRowIndex = 0; initRowIndex < winSize - 1; initRowIndex++) {
                rowIndex[initRowIndex] = rowIndex[initRowIndex + 1];
            }
            rowIndex[winSize - 1] = zeroIndex;
        }

    }

    private static void xFPyrDownprocessgaussian(
            StreamFIFO<Byte> src,
            StreamFIFO<Byte> out,
            Byte[][] buf,
            Byte[][] srcBuf,
            Byte[] outputValues,
            Byte p0, int rows, int cols,
            Integer shiftX,
            int[] rowIndex,
            int row,
            int winSize

    ) {
        // buf的buf（一列）
        Byte[] bufCop = new Byte[winSize];
        for (int col = 0; col < cols + (winSize >> 1); col++) {
            if (row < rows && col < cols) {
                // 读到buf的首行
                buf[rowIndex[winSize - 1]][col] = src.read();
            } else {
                buf[rowIndex[winSize - 1]][col] = 0;
            }

            // 如果winSize为5 copyBufVar这里循环0 1 2 3 4
            for (int copyBufVar = 0; copyBufVar < winSize; copyBufVar++) {
                // row到最后一行的距离（超过）
                int distanceToLastRow = (row - (rows - 1));

                if ((row > rows - 1) && (copyBufVar > (winSize - 1 - distanceToLastRow))) {
                    // row超过最后一行
                    bufCop[copyBufVar] = buf[(rowIndex[winSize - 1 - distanceToLastRow])][col];
                } else {
                    bufCop[copyBufVar] = buf[(rowIndex[copyBufVar])][col];
                }
            }

            // 如果winSize为5 这里循环0 1 2 3 4
            // 把srcBuf(5*5)的最后一列给填上了（用copyBuf填的）
            for (int extractPx = 0; extractPx < winSize; extractPx++) {
                if (col < cols) {
                    srcBuf[extractPx][winSize - 1] = bufCop[extractPx];
                } else {
                    srcBuf[extractPx][winSize - 1] = srcBuf[extractPx][winSize - 2];
                }
            }

            xFPyrDownApplykernel(outputValues[0], srcBuf, winSize);
            if (col >= winSize >> 1) {
                out.write(outputValues[0]);
            }

            // 如果winSize为5 这里循环0 1 2 3 4
            for (int wrapBuf = 0; wrapBuf < winSize; wrapBuf++) {
                // 如果winSize为5 这里循环0 1 2 3
                for (int colWrap = 0; colWrap < winSize - 1; colWrap++) {
                    if (col == 0) {
                        srcBuf[wrapBuf][colWrap] = srcBuf[wrapBuf][winSize - 1];
                    } else {
                        srcBuf[wrapBuf][colWrap] = srcBuf[wrapBuf][colWrap + 1];
                    }
                }
            }
        }

    }

    private static void xFPyrDownApplykernel (Byte outputValues, Byte[][] srcBuf, int winSize){
        outputValues = 0;
    }
}
