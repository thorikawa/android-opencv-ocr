package com.polysfactory.opencvocr;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.polysfactory.cv.util.IOUtil;
import com.polysfactory.opencvocr.jni.NativeMarkerDetector;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraViewEx;
import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private JavaCameraViewEx mCameraView;
    private NativeMarkerDetector mNativeOcr;
    private int mWidth;
    private int mHeight;
    private float mScale;

    private File nm1File;
    private File nm2File;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.marker_tracking);

        mWidth = getResources().getDimensionPixelSize(R.dimen.view_width);
        mHeight = getResources().getDimensionPixelSize(R.dimen.view_height);

        mCameraView = (JavaCameraViewEx) findViewById(R.id.camera_view);
        mCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY);
        mCameraView.setCvCameraViewListener(this);
        mCameraView.setMaxFrameSize(mWidth, mHeight);
        mCameraView.disableView();

        nm1File = IOUtil.getFilePath(this, "ocr", "nm1.xml");
        nm2File = IOUtil.getFilePath(this, "ocr", "nm2.xml");
        IOUtil.copy(this, R.raw.trained_classifier_nm1, nm1File);
        IOUtil.copy(this, R.raw.trained_classifier_nm2, nm2File);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mNativeOcr = new NativeMarkerDetector(nm1File.getAbsolutePath(), nm2File.getAbsolutePath());
        mCameraView.enableView();
    }

    public void onDestroy() {
        super.onDestroy();
        mCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(L.TAG, "framesize:" + width + "," + height);
        mScale = mCameraView.getScale();
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        Mat frame = inputFrame.rgba();
        if (mNativeOcr != null) {
            float scale = mScale == 0.0f ? 1.0f : mScale;
            mNativeOcr.doOcr(frame, scale);
        }

        return frame;
    }
}
