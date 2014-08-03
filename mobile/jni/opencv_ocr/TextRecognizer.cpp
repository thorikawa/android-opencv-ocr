////////////////////////////////////////////////////////////////////
// Standard includes:
#include <iostream>
#include <sstream>

////////////////////////////////////////////////////////////////////
// File includes:
#include "TextRecognizer.hpp"

using namespace std;
using namespace cv;
using namespace cv::text;

TextRecognizer::TextRecognizer(string nm1File, string nm2File)
{
    er_filter1 = createERFilterNM1(loadClassifierNM1(nm1File), 16, 0.00015f, 0.13f, 0.2f, true, 0.1f);
    er_filter2 = createERFilterNM2(loadClassifierNM2(nm2File), 0.5);
    tess = new tesseract::TessBaseAPI();
    if (tess->Init("/sdcard/tessdata/", "eng")) {
        LOGD("OCRTesseract: Could not initialize tesseract.");
        throw 1;
    }
    tess->SetPageSegMode(tesseract::PSM_AUTO);
    tess->SetVariable("tessedit_char_whitelist", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
    tess->SetVariable("save_best_choices", "T");
}

TextRecognizer::~TextRecognizer()
{
    er_filter1.release();
    er_filter2.release();
    tess->End();
}

void groups_draw(const Mat &src, vector<Rect> &groups)
{
    for (int i = (int)groups.size()-1; i >= 0; i--) {
        if (src.type() == CV_8UC3) {
            rectangle(src,groups.at(i).tl(),groups.at(i).br(),Scalar( 0, 255, 255 ), 3, 8 );
        } else {
            rectangle(src,groups.at(i).tl(),groups.at(i).br(),Scalar( 255 ), 3, 8 );
        }
    }
}

void er_show(const cv::Mat& src, vector<Mat> &channels, vector<vector<ERStat> > &regions)
{
    for (int c = 0; c < (int)channels.size(); c++) {
        // Mat dst = Mat::zeros(channels[0].rows+2, channels[0].cols+2, CV_8UC1);
        for (int r = 0; r < (int)regions[c].size(); r++) {
            ERStat er = regions[c][r];
            // deprecate the root region
            if (er.parent != NULL) {
                int newMaskVal = 255;
                int flags = 4 + (newMaskVal << 8) + FLOODFILL_FIXED_RANGE + FLOODFILL_MASK_ONLY;
                //floodFill(channels[c], dst, Point(er.pixel % channels[c].cols, er.pixel / channels[c].cols),
                //          Scalar(255), 0, Scalar(er.level), Scalar(0), flags);
                floodFill(src, src, Point(er.pixel % channels[c].cols, er.pixel / channels[c].cols),
                          Scalar(255), 0, Scalar(er.level), Scalar(0), flags);
            }
        }
    }
}

bool isRepetitive(const string& s)
{
    int count = 0;
    for (int i=0; i<(int)s.size(); i++) {
        if ((s[i] == 'i') ||
                (s[i] == 'l') ||
                (s[i] == 'I'))
            count++;
    }
    if (count > ((int)s.size()+1)/2) {
        return true;
    }
    return false;
}


void er_draw(vector<Mat> &channels, vector<vector<ERStat> > &regions, vector<Vec2i> group, Mat& segmentation)
{
    for (int r=0; r<(int)group.size(); r++) {
        ERStat er = regions[group[r][0]][group[r][1]];
        if (er.parent != NULL) {
            int newMaskVal = 255;
            int flags = 4 + (newMaskVal << 8) + FLOODFILL_FIXED_RANGE + FLOODFILL_MASK_ONLY;
            floodFill(channels[group[r][0]],segmentation,Point(er.pixel%channels[group[r][0]].cols,er.pixel/channels[group[r][0]].cols),
                      Scalar(255),0,Scalar(er.level),Scalar(0),flags);
        }
    }
}

void TextRecognizer::processFrame(const cv::Mat& input, float scale)
{
    Size srcSize = input.size();
    Size smallSize(srcSize.width * 0.25, srcSize.height * 0.25);
    Mat small(smallSize, CV_8UC4);
    resize(input, small, smallSize);
    Mat src(smallSize, CV_8UC3);
    cvtColor(small, src, COLOR_RGBA2RGB);


    // Extract channels to be processed individually
    vector<Mat> channels;
    /*
    computeNMChannels(src, channels);

    int cn = (int)channels.size();
    // Append negative channels to detect ER- (bright regions over dark background)
    for (int c = 0; c < cn-1; c++) {
        channels.push_back(255 - channels[c]);
    }
    */
    // Notice here we are only using grey channel, see textdetection.cpp for example with more channels
    Mat grey;
    cvtColor(src, grey, COLOR_RGBA2GRAY);
    channels.push_back(grey);
    channels.push_back(255 - grey);

    vector<vector<ERStat> > regions(channels.size());
    // Apply the default cascade classifier to each independent channel (could be done in parallel)
    LOGD("Extracting Class Specific Extremal Regions from %d channels ...", (int)channels.size());
    LOGD("    (...) this may take a while (...)");
    for (int c = 0; c < (int)channels.size(); c++) {
        er_filter1->run(channels[c], regions[c]);
        er_filter2->run(channels[c], regions[c]);
    }

    // Detect character groups
    LOGD("Grouping extracted ERs ... ");
    vector< vector<Vec2i> > nm_region_groups;
    vector<Rect> nm_boxes;
    erGrouping(src, channels, regions, nm_region_groups, nm_boxes,ERGROUPING_ORIENTATION_HORIZ);
    //erGrouping(src, channels, regions, region_groups, groups_boxes, ERGROUPING_ORIENTATION_ANY, "./trained_classifier_erGrouping.xml", 0.5);

    // draw groups
    // groups_draw(src, groups_boxes);
    // er_show(channels,regions);

    /*Text Recognition (OCR)*/

    string output;

    // Mat out_img;
    // src.copyTo(out_img);
    // Mat out_img_detection;
    // Mat out_img_segmentation = Mat::zeros(src.rows+2, src.cols+2, CV_8UC1);
    // src.copyTo(out_img_detection);
    float scale_img  = 600.f/src.rows;
    float scale_font = max(0.5f, (float)(2-scale_img)/1.4f);
    LOGD("font scale:%f", scale_font);
    vector<string> words_detection;

    for (int i=0; i<(int)nm_boxes.size(); i++) {

        // rectangle(out_img_detection, nm_boxes[i].tl(), nm_boxes[i].br(), Scalar(0,255,255), 3);

        Mat group_img = Mat::zeros(src.rows+2, src.cols+2, CV_8UC1);
        er_draw(channels, regions, nm_region_groups[i], group_img);
        Mat group_segmentation;
        group_img.copyTo(group_segmentation);
        //image(nm_boxes[i]).copyTo(group_img);
        group_img(nm_boxes[i]).copyTo(group_img);
        copyMakeBorder(group_img,group_img,15,15,15,15,BORDER_CONSTANT,Scalar(0));

        vector<Rect>   boxes;
        vector<string> words;
        vector<float>  confidences;
        // Use tesseract raw API instead of opencv tesseract wrapper.
        // See: https://code.google.com/p/tesseract-ocr/wiki/APIExample
        // ocr->run(group_img, output, &boxes, &words, &confidences, OCR_LEVEL_WORD);
        tess->SetImage((uchar*)group_img.data, group_img.size().width, group_img.size().height, group_img.channels(), group_img.step1());
        tess->Recognize(0);
        output = string(tess->GetUTF8Text());
        tesseract::ResultIterator* ri = tess->GetIterator();
        tesseract::PageIteratorLevel level = tesseract::RIL_WORD;
        if (ri != 0) {
            do {
                const char* word = ri->GetUTF8Text(level);
                if (word == NULL) {
                    LOGD("word is null, skipped");
                    continue;
                }
                float conf = ri->Confidence(level);
                int x1, y1, x2, y2;
                ri->BoundingBox(level, &x1, &y1, &x2, &y2);
                LOGD("word: '%s';  \tconf: %.2f; BoundingBox: %d,%d,%d,%d;", word, conf, x1, y1, x2, y2);

                Rect rect(x1, y1, x2 - x1, y2 - y1);
                boxes.push_back(rect);
                words.push_back(string(word));
                confidences.push_back(conf);

                delete[] word;
            } while (ri->Next(level));
        }
        tess->Clear();

        output.erase(remove(output.begin(), output.end(), '\n'), output.end());
        if (output.size() < 3)
            continue;

        for (int j=0; j<(int)boxes.size(); j++) {
            boxes[j].x += nm_boxes[i].x-15;
            boxes[j].y += nm_boxes[i].y-15;

            // cout << "  word = " << words[j] << "\t confidence = " << confidences[j] << endl;
            if ((words[j].size() < 2) || (confidences[j] < 51) ||
                    ((words[j].size()==2) && (words[j][0] == words[j][1])) ||
                    ((words[j].size()< 4) && (confidences[j] < 60)) ||
                    isRepetitive(words[j]))
                continue;
            words_detection.push_back(words[j]);
            // rectangle(out_img, boxes[j].tl(), boxes[j].br(), Scalar(255,0,255),3);
            rectangle(src, boxes[j].tl(), boxes[j].br(), Scalar(255,0,255),3);
            Size word_size = getTextSize(words[j], FONT_HERSHEY_SIMPLEX, (double)scale_font, (int)(3*scale_font), NULL);
            // rectangle(out_img, boxes[j].tl()-Point(3,word_size.height+3), boxes[j].tl()+Point(word_size.width,0), Scalar(255,0,255),-1);
            rectangle(src, boxes[j].tl()-Point(3,word_size.height+3), boxes[j].tl()+Point(word_size.width,0), Scalar(255,0,255),-1);
            // putText(out_img, words[j], boxes[j].tl()-Point(1,1), FONT_HERSHEY_SIMPLEX, scale_font, Scalar(255,255,255),(int)(3*scale_font));
            putText(src, words[j], boxes[j].tl()-Point(1,1), FONT_HERSHEY_SIMPLEX, scale_font, Scalar(255,255,255),(int)(3*scale_font));
            // out_img_segmentation = out_img_segmentation | group_segmentation;
        }
    }

    LOGD("ocr done");

    cvtColor(src, small, COLOR_RGB2RGBA);
    resize(small, input, srcSize);
}
