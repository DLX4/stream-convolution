package com.github.dlx4.convolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mat<T> {

    private final List<T> data;

    private final int rows;
    private final int cols;

    private int readCount;
    private int writeCount;

    public Mat(int rows, int cols, T[][] data) {
        this.rows = rows;
        this.cols = cols;

        this.data = new ArrayList<>(rows * cols);

        for (int i = 0; i < data.length; i++) {
            this.data.addAll(Arrays.asList(data[i]));
        }

        this.readCount = 0;
        this.writeCount = 0;
    }

    public T read(int readPointer) {
        readCount++;
        return data.get(readPointer);
    }

    public void write(int writePointer, T o) {
        writeCount++;
        data.set(writePointer, o);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n");
        sb.append("readCount=").append(readCount).append("\r\n");
        sb.append("writeCount=").append(writeCount).append("\r\n");
        sb.append("data=\r\n");

        for (int i = 0; i < rows; i++) {
            data.subList(cols * i, cols * i + cols).forEach(d -> sb.append(String.format("%-4s", d)));
            sb.append("\r\n");
        }
        return sb.toString();
    }
}
