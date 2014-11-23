package com.polysfactory.opencvocr.jni;

import org.opencv.core.Mat;

import java.util.List;

public class NativeMarkerDetector {

    private long mNativeObj;

    public NativeMarkerDetector(String nm1File, String nm2File) {
        mNativeObj = nativeCreateObject(nm1File, nm2File);
    }

    public void doOcr(Mat imageBgra, float scale) {
        Mat transformationsMat = new Mat();
        nativeFindMarkers(mNativeObj, imageBgra.nativeObj, transformationsMat.nativeObj, scale);
    }

    public void release() {
        nativeDestroyObject(mNativeObj);
        mNativeObj = 0;
    }

    private native long nativeCreateObject(String nm1File, String nm2File);

    private native void nativeFindMarkers(long thiz, long imageBgra, long transformations, float scale);

    private native void nativeDestroyObject(long thiz);
}
