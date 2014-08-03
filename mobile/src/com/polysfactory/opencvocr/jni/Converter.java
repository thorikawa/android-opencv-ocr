package com.polysfactory.opencvocr.jni;

import java.util.List;

import org.opencv.core.Mat;

public class Converter {

    public static void transformationsMatToList(Mat input, List<Mat> output) {
        output.clear();
        int count = input.rows();
        for (int i = 0; i < count; i++) {
            Mat row = input.row(i);
            row.reshape(1, 4);
            output.add(row);
        }
        return;
    }

}
