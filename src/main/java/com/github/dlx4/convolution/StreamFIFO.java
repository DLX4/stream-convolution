package com.github.dlx4.convolution;

import java.util.LinkedList;

public class StreamFIFO<T> {

    private LinkedList<T> data = new LinkedList<>();

    public void write(T o) {
        data.addLast(o);
    }

    public T read() {
        return data.removeFirst();
    }
}
