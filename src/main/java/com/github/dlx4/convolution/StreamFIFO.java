package com.github.dlx4.convolution;

import java.util.LinkedList;

public class StreamFIFO<T> {

    private LinkedList<T> data;

    private int readCount;
    private int writeCount;

    public StreamFIFO() {
        data = new LinkedList<>();
        readCount = 0;
        writeCount = 0;
    }

    public void write(T o) {
        writeCount++;
        data.addLast(o);
    }

    public T read() {
        readCount++;
        return data.removeFirst();
    }

    @Override
    public String toString() {
        return "StreamFIFO{" +
                "readCount=" + readCount +
                ", writeCount=" + writeCount +
                '}';
    }
}
