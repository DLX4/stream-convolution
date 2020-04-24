package com.github.dlx4.convolution;

import java.util.Random;

public class PyrDown {

    public static void main(String[] args) {
        int rows = 30;
        int cols = 20;
        Random random = new Random();

        Integer[][] srcData = new Integer[rows][cols];
        Integer[][] dstData = new Integer[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // srcData[i][j] = random.nextInt(255);
                srcData[i][j] = cols * i + j;
                dstData[i][j] = 0;
            }
        }

        Mat<Integer> src = new Mat<>(rows, cols, srcData);
        Mat<Integer> dst = new Mat<>(rows, cols, dstData);

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

        StreamPyrDownHelper helper = new StreamPyrDownHelper(filterIn, filterOut, 5, rows, cols);
        helper.doPyrDown();

        int writePointer = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Integer b = filterOut.read();
                //if (i % 2 == 0 && j % 2 == 0) {
                dst.write(writePointer, b);
                writePointer++;
                //}
            }
        }

        System.out.println(filterIn.toString());
        System.out.println(filterOut.toString());
    }

    private static class StreamPyrDownHelper {

        private final int winSize;
        private final int halfWinSize;
        private final int rows;
        private final int cols;
        // 输入及参数
        private StreamFIFO<Integer> src;
        private StreamFIFO<Integer> dst;
        // 数据结构
        // 窗口缓存
        private Integer[][] window;
        // 行缓存
        private Integer[][] rowBuf;
        // 行缓存滚动索引
        private int[] rowBufIndex;

        private int curRow;
        private int curCol;

        StreamPyrDownHelper(StreamFIFO<Integer> src,
                            StreamFIFO<Integer> dst,
                            int winSize,
                            int rows,
                            int cols) {
            this.src = src;
            this.dst = dst;
            this.winSize = winSize;
            this.halfWinSize = winSize >> 1;
            this.rows = rows;
            this.cols = cols;

            this.window = new Integer[winSize][winSize];
            this.rowBuf = new Integer[winSize][cols + halfWinSize];
            /* rowBufIndex 会按照以下的逻辑变化 0 1 2 3 4 ; 1 2 3 4 0; 2 3 4 0 1 ... */
            this.rowBufIndex = new int[winSize];
        }

        void doPyrDown() {
            initRowIndex();
            initRowBuf();
            traverseWindow();
        }

        /**
         * 初始化行缓存滚动索引
         *
         * @param
         * @return void
         */
        void initRowIndex() {
            // 如果winSize是5 这里就是初始化为 0 1 2 3 4
            for (int initRowIndex = 0; initRowIndex < winSize; initRowIndex++) {
                rowBufIndex[initRowIndex] = initRowIndex;
            }
        }

        /**
         * 初始化RowBuf，读2行数据分别存在RowBuf[2] RowBuf[3]
         * RowBuf[0] RowBuf[1] = RowBuf[2]
         * <p>
         * Mat[0] -- RowBuf[0]
         * Mat[0] -- RowBuf[1]
         * Mat[0] -- RowBuf[2]
         * Mat[1] -- RowBuf[3]
         * -- RowBuf[4]（空的）
         *
         * @return void
         */
        void initRowBuf() {
            // 如果winSize是5 这里循环2 3
            for (int initBuf = rowBufIndex[halfWinSize]; initBuf < rowBufIndex[winSize - 1]; initBuf++) {
                for (int col = 0; col < cols; col++) {
                    rowBuf[initBuf][col] = src.read();
                }
            }

            // 处理边界 如果winSize是5 这里循环 0 1
            for (int initBuf = 0; initBuf < halfWinSize; initBuf++) {
                for (int col = 0; col < cols; col++) {
                    rowBuf[initBuf][col] = rowBuf[rowBufIndex[halfWinSize]][col];
                }
            }
        }


        /**
         * 窗口吃一格
         *
         * @return void
         */
        void windowSwallow() {
            // 窗口移动一格新增的数据
            Integer[] nextStep = new Integer[winSize];

            // 从 RowBuf切一列到bufCop
            if (curRow > rows - 1) {
                // 边界特殊处理
                // 如果winSize为5 copyBufVar这里循环0 1 2 3 4
                for (int i = 0; i < winSize; i++) {
                    // row到最后一行的距离（超过）取值为 1 2
                    int distanceToLastRow = (curRow - (rows - 1));
                    // 真实最后一行的标记 取值为3 2
                    int lastRowMark = winSize - 1 - distanceToLastRow;

                    if (i > lastRowMark) {
                        /*       distanceToLastRow           whichRowToCopy
                        last+1 :        1                         3               bufCop[4] = rowBuf[rowBufIndex[3]]
                        last+2 :        2                         2               bufCop[3] = rowBuf[rowBufIndex[2]]  bufCop[4] = rowBuf[rowBufIndex[2]]
                         */
                        nextStep[i] = rowBuf[(rowBufIndex[lastRowMark])][curCol];
                    }
                }
            } else {
                // 正常处理
                for (int i = 0; i < winSize; i++) {
                    nextStep[i] = rowBuf[(rowBufIndex[i])][curCol];
                }
            }

            // 更新窗口新数据
            for (int i = 0; i < winSize; i++) {
                if (curCol < cols) {
                    window[i][winSize - 1] = nextStep[i];
                } else {
                    window[i][winSize - 1] = window[i][winSize - 2];
                }
            }
        }

        /**
         * 窗口吐出来一格
         *
         * @return void
         */
        void windowSpit() {
            // 如果winSize为5 这里循环0 1 2 3 4
            for (int i = 0; i < winSize; i++) {
                // 如果winSize为5 这里循环0 1 2 3
                for (int j = 0; j < winSize - 1; j++) {
                    if (curCol == 0) {
                        window[i][j] = window[i][winSize - 1];
                    } else {
                        window[i][j] = window[i][j + 1];
                    }
                }
            }
        }

        /**
         * 计算窗口数据
         *
         * @return java.lang.Integer
         */
        Integer windowCalculate() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < winSize; i++) {
                for (int j = 0; j < winSize; j++) {
                    sb.append(String.format("%-4s", window[i][j]));
                }
                sb.append("\r\n");
            }
            System.out.println("计算窗口：");
            System.out.println(sb.toString());
            return 8;
        }

        /**
         * 下滚一行
         *
         * @return void
         */
        void rollNextRow() {
            // 下面实现的效果就是从0 1 2 3 4 变成 1 2 3 4 0
            int zeroIndex = rowBufIndex[0];
            // 如果winSize是5 这里循环0 1 2 3
            for (int initRowIndex = 0; initRowIndex < winSize - 1; initRowIndex++) {
                rowBufIndex[initRowIndex] = rowBufIndex[initRowIndex + 1];
            }
            rowBufIndex[winSize - 1] = zeroIndex;
        }

        /**
         * 窗口遍历（包括边界处理）
         *
         * @return void
         */
        void traverseWindow() {

            // 遍历所有行 2 3 4 ... last last+1 last+2
            for (curRow = halfWinSize; curRow < rows + halfWinSize; curRow++) {

                // 当前遍历行 。该变量变化规律 4 0 1 2 3 4 0 1..
                int curRowIndex = rowBufIndex[winSize - 1];

                // 遍历所有列 0 1 2 3 ... last last+1 last+2
                for (curCol = 0; curCol < cols + halfWinSize; curCol++) {

                    // 从输入流中读取一单位数据
                    if (curRow < rows && curCol < cols) {
                        rowBuf[curRowIndex][curCol] = src.read();
                    } else {
                        rowBuf[curRowIndex][curCol] = 0;
                    }

                    // 窗口吃一格
                    windowSwallow();

                    // 计算新窗口
                    Integer outputValue = windowCalculate();

                    // 输出结果到流
                    if (curCol >= halfWinSize) {
                        dst.write(outputValue);
                    }

                    // 窗口吐一格
                    windowSpit();
                }

                // 滚动
                this.rollNextRow();
            }
        }
    }

}
