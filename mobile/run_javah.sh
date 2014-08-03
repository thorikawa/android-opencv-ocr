#!/bin/sh

javah -classpath build/intermediates/classes/armv7/debug:libs/opencv-sdk.jar -o jni/ocr_jni.h com.polysfactory.opencvocr.jni.NativeMarkerDetector
