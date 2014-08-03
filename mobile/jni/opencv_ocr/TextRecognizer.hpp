#ifndef TextRecognizer_hpp
#define TextRecognizer_hpp

#include <opencv2/core/utility.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/text.hpp>
#include <baseapi.h>
#include <allheaders.h>
#include "common.hpp"

class TextRecognizer
{
public:
    TextRecognizer(std::string nm1File, std::string nm2File);
    ~TextRecognizer();
    void processFrame(const cv::Mat& bgraMat, float scale = 1.0F);

private:
    cv::Ptr<cv::text::ERFilter> er_filter1;
    cv::Ptr<cv::text::ERFilter> er_filter2;
    tesseract::TessBaseAPI *tess;
};

#endif
