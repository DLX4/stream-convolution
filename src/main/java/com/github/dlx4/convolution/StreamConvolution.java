package com.github.dlx4.convolution;

/**
 * 卷积算法流处理实现
 *
 * @author dinglingxiang
 */
public abstract class StreamConvolution {

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
    /*  rowBufIndex 会按照以下的逻辑变化 0 1 2 3 4 ; 1 2 3 4 0; 2 3 4 0 1 ...
        rowBuf[rowBufIndex[winsize - 1]]用来放新行的数据
        避免了拷贝数据
        行缓存的数据： rowBuf[rowBufIndex[0]]  rowBuf[rowBufIndex[1]] ... rowBuf[rowBufIndex[winsize - 1]]
    */
    private int[] rowBufIndex;

    private int curRow;
    private int curCol;

    StreamConvolution(StreamFIFO<Integer> src,
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

        this.rowBufIndex = new int[winSize];
    }

    /**
     * 流处理卷积算法
     */
    void doConvolution() {
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
            // 末尾行处理
            // 如果winSize为5 copyBufVar这里循环0 1 2 3 4
            for (int i = 0; i < winSize; i++) {
                // 当前行已经越过末尾行
                int distanceToLastRow = (curRow - (rows - 1));
                // 当前行的index是winSize-1， 那么末尾行的index是 winSize - 1 - distanceToLastRow
                int lastRowIndex = winSize - 1 - distanceToLastRow;

                if (i > lastRowIndex) {
                    // 复制的最后一行的数据
                    nextStep[i] = rowBuf[(rowBufIndex[lastRowIndex])][curCol];
                } else {
                    // 复制的rowBuf原有的数据
                    nextStep[i] = rowBuf[(rowBufIndex[i])][curCol];
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

    void showWindowBuff() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < winSize; i++) {
            for (int j = 0; j < winSize; j++) {
                sb.append(String.format("%-5s", window[i][j]));
            }
            sb.append("\r\n");
        }
        System.out.println("当前窗口缓存：");
        System.out.println(sb.toString());
    }

    void showRowBuff() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < winSize - 1; i++) {
            for (int j = 0; j < cols + halfWinSize; j++) {
                sb.append(String.format("%-5s", rowBuf[rowBufIndex[i]][j]));
            }
            sb.append("\r\n");
        }

        for (int i = winSize - 1; i < winSize; i++) {
            for (int j = 0; j < cols + halfWinSize; j++) {
                sb.append(String.format("%-5s", rowBuf[rowBufIndex[i]][j]));
            }
            sb.append("\r\n");
        }
        System.out.println("当前行缓存：");
        System.out.println(sb.toString());
    }

    /**
     * 计算窗口数据
     *
     * @return java.lang.Integer
     */
    abstract Integer windowCalculate(Integer[][] window, int winSize);

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

        showRowBuff();
        showWindowBuff();

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

                showRowBuff();
                showWindowBuff();

                // 窗口吃一格
                windowSwallow();

                showRowBuff();
                showWindowBuff();

                // 输出结果到流
                if (curCol >= halfWinSize) {
                    // 计算
                    dst.write(windowCalculate(this.window, winSize));
                }

                // 窗口吐一格
                windowSpit();
            }

            // 滚动
            this.rollNextRow();
        }
    }

}
