package io.fand.api.map;

public interface MapCanvas {
    int WIDTH = 128;
    int HEIGHT = 128;

    void pixel(int x, int y, byte color);
}
