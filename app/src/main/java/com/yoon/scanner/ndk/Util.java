package com.yoon.scanner.ndk;

/**
 * Created by Administrator on 2016-08-19.
 */
public class Util {
    public native String warp(String path);

    static {
        System.loadLibrary("opencv_java3");
    }
    static {
        System.loadLibrary("warp");
    }
}
