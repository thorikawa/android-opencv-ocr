package com.polysfactory.opencvocr;

import org.opencv.android.OpenCVLoader;

import android.app.Application;

public class OcrApplication extends Application {
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        } else {
            System.loadLibrary("lept");
            System.loadLibrary("tess");
            System.loadLibrary("opencvocr");
        }
    }

    public void onCreate() {
    };
}
