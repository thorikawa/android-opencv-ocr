#include "ocr_jni.h"
#include <opencv2/core/core.hpp>
#include <android/log.h>

#include <string>
#include <vector>
#include "common.hpp"
#include "TextRecognizer.hpp"

using namespace std;
using namespace cv;

JNIEXPORT jlong JNICALL Java_com_polysfactory_opencvocr_jni_NativeMarkerDetector_nativeCreateObject(
		JNIEnv * jenv, jobject, jstring jnm1, jstring jnm2) {
	jlong detector;
	try {
	    const char* nm1 = jenv->GetStringUTFChars(jnm1, NULL);
	    const char* nm2 = jenv->GetStringUTFChars(jnm2, NULL);
		detector = (jlong) new TextRecognizer(string(nm1), string(nm2));
	} catch (...) {
		LOGD("nativeCreateObject caught unknown exception");
		jclass je = jenv->FindClass("java/lang/Exception");
		jenv->ThrowNew(je,
				"Unknown exception in JNI code {highgui::VideoCapture_n_1VideoCapture__()}");
		return 0;
	}

	return detector;
}

JNIEXPORT void JNICALL Java_com_polysfactory_opencvocr_jni_NativeMarkerDetector_nativeFindMarkers(
		JNIEnv * jenv, jobject, jlong thiz, jlong imageBgra, jlong transMat, jfloat scale) {
	try {
        TextRecognizer* markerDetector = (TextRecognizer*) thiz;
        Mat* image = (Mat*) imageBgra;
        // Mat* transMatObj = (Mat*) transMat;
        markerDetector->processFrame(*image, (float) scale);
	} catch (...) {
		LOGD("nativeDetect caught unknown exception");
		jclass je = jenv->FindClass("java/lang/Exception");
		jenv->ThrowNew(je,
				"Unknown exception in JNI code {highgui::VideoCapture_n_1VideoCapture__()}");
	}
}

JNIEXPORT void JNICALL Java_com_polysfactory_opencvocr_jni_NativeMarkerDetector_nativeDestroyObject(
		JNIEnv * jenv, jobject, jlong thiz) {
	try {
	} catch (...) {
		LOGD("nativeDestroyObject caught unknown exception");
		jclass je = jenv->FindClass("java/lang/Exception");
		jenv->ThrowNew(je,
				"Unknown exception in JNI code {highgui::VideoCapture_n_1VideoCapture__()}");
	}
}
