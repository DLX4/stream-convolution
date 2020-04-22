package com.github.dlx4.convolution;

import java.util.ArrayList;
import java.util.List;

public class Mat<T> {

    private final List<T> data;

    public Mat(int size) {
        data = new ArrayList<>(size);
    }

    public T read(int readPointer) {
        return data.get(readPointer);
    }

    public void write(int writePointer, T o) {
        data.set(writePointer, o);
    }
}
