package com.polysfactory.opencvocr;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private static final int MAX_MERKERS = 10;
    private JavaCameraViewEx mCameraView;
    private GLSurfaceView mGLView;
    private NativeMarkerDetector mMarkerDetector;
    private float[][] mTransformation = new float[MAX_MERKERS][16];
    private int mMarkerCount;
    private int mWidth;
    private int mHeight;
    private float mScale;
    private Cube cube = new Cube();

    // camera parameter array corresponding to [fx, fy, cx, cy]
    // camera matrix will be [fx, 0, cx], [0, fy, cy], [0, 0, 1]

    // Google Glass
    // private static final float[] mCameraParameters = {357.658935546875f, 357.658935546875f, 319.5f, 179.5f};
    // Galaxy S5
    private static final float[] mCameraParameters = {1617.964244716695f, 1617.964244716695f, 959.5f, 539.5f};

    // OpenGL constants
    private static final FloatBuffer squareVertices = allocateFloatBufferDirect(new float[]{-0.5f, -0.5f, 0.5f,
            -0.5f, -0.5f, 0.5f, 0.5f, 0.5f});
    private static final ByteBuffer squareColors = allocateByteBufferDirect(new byte[]{(byte) 255, (byte) 255, 0,
            (byte) 255, 0, (byte) 255, (byte) 255, (byte) 255, 0, 0, 0, 0, (byte) 255, 0, (byte) 255, (byte) 255});
    private static final FloatBuffer lineX = allocateFloatBufferDirect(new float[]{0f, 0f, 0f, 1f, 0f, 0f});
    private static final FloatBuffer lineY = allocateFloatBufferDirect(new float[]{0f, 0f, 0f, 0f, 1f, 0f});
    private static final FloatBuffer lineZ = allocateFloatBufferDirect(new float[]{0f, 0f, 0f, 0f, 0f, 1f});
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

        mGLView = (GLSurfaceView) findViewById(R.id.gl_view);
        mGLView.setZOrderOnTop(true);
        mGLView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLView.setRenderer(new GLRenderer());
        mGLView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

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
        mMarkerDetector = new NativeMarkerDetector(nm1File.getAbsolutePath(), nm2File.getAbsolutePath());
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
        if (mMarkerDetector != null) {
            List<Mat> transformations = new ArrayList<Mat>();
            float scale = mScale == 0.0f ? 1.0f : mScale;
            mMarkerDetector.findMarkers(frame, transformations, scale);
            mMarkerCount = transformations.size();
            if (mMarkerCount > 0) {
                for (int i = 0; i < Math.min(MAX_MERKERS, mMarkerCount); i++) {
                    Mat mat = transformations.get(i);
                    mat.get(0, 0, mTransformation[i]);
                }
            }
        }

        return frame;
    }

    private static FloatBuffer allocateFloatBufferDirect(float[] data) {
        FloatBuffer result;
        ByteBuffer vbb = ByteBuffer.allocateDirect(data.length * Float.SIZE / 8);
        vbb.order(ByteOrder.nativeOrder());
        result = vbb.asFloatBuffer();
        result.put(data);
        result.flip();
        return result;
    }

    private static ByteBuffer allocateByteBufferDirect(byte[] data) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(data.length * Byte.SIZE / 8);
        vbb.order(ByteOrder.nativeOrder());
        vbb.put(data);
        vbb.flip();
        return vbb;
    }

    private class GLRenderer implements GLSurfaceView.Renderer {

        private float[] projectionMatrix;

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            projectionMatrix = buildProjectionMatrix(width, height);
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadMatrixf(allocateFloatBufferDirect(projectionMatrix));
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

            if (mMarkerCount > 0) {
                drawAR(gl);
            }
        }

        private void drawAR(GL10 gl) {

            gl.glDepthMask(true);
            gl.glEnable(GL10.GL_DEPTH_TEST);

            gl.glPushMatrix();
            gl.glLineWidth(5.0f);

            gl.glMatrixMode(GL10.GL_MODELVIEW);

            for (int i = 0; i < mMarkerCount; i++) {
                gl.glLoadIdentity();
                gl.glLoadMatrixf(allocateFloatBufferDirect(mTransformation[i]));
                gl.glScalef(0.5f, 0.5f, 0.5f);
                cube.draw(gl);
            }

            gl.glPopMatrix();
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        }
    }

    private static float[] buildProjectionMatrix(int w, int h) {
        float near = 0.01f; // Near clipping distance
        float far = 100f; // Far clipping distance

        // Camera parameters
        float f_x = mCameraParameters[0]; // Focal length in x axis
        float f_y = mCameraParameters[1]; // Focal length in y axis (usually the same?)
        float c_x = mCameraParameters[2]; // Camera primary point x
        float c_y = mCameraParameters[3]; // Camera primary point y

        float projectionMatrix[] = new float[16];
        float size = 2.0f;
        projectionMatrix[0] = -2.0f * f_x / w;
        projectionMatrix[1] = 0.0f;
        projectionMatrix[2] = 0.0f;
        projectionMatrix[3] = 0.0f;

        projectionMatrix[4] = 0.0f;
        projectionMatrix[5] = 2.0f * f_y / h;
        projectionMatrix[6] = 0.0f;
        projectionMatrix[7] = 0.0f;

        projectionMatrix[8] = size * c_x / w - 1.0f;
        projectionMatrix[9] = size * c_y / h - 1.0f;
        projectionMatrix[10] = -(far + near) / (far - near);
        projectionMatrix[11] = -1.0f;

        projectionMatrix[12] = 0.0f;
        projectionMatrix[13] = 0.0f;
        projectionMatrix[14] = -2.0f * far * near / (far - near);
        projectionMatrix[15] = 0.0f;

        return projectionMatrix;
    }

}
