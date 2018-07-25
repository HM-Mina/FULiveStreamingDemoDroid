package com.sensetime.stmobile.model;

/**
 * Created by sensetime on 17-3-1.
 */

public class STImage {
    public byte[] imageData;
    public int pixelFormat;
    public int width;
    public int height;
    public int stride;
    public STTime timeStamp;

    public class STTime{
        long second;
        long microSeconds;
    }
}
