#include <jni.h>
#include "com_yoon_scanner_ndk_Util.h"
//#include <opencv/cv.h>
//#include <opencv2/imgproc/imgproc_c.h>
//#include <opencv2/core.hpp>
//#include <opencv2/highgui.hpp>
//#include <opencv2/imgproc.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <stdio.h>
#include <iostream>
#include <cmath>

using namespace std;
using namespace cv;

extern "C" {
//JNIEXPORT jstring JNICALL Java_com_yoon_scanner_ndk_Util_warp (JNIEnv *env, jobject obj, jstring path);
JNIEXPORT jstring JNICALL Java_com_yoon_scanner_ndk_Util_warp (JNIEnv *env, jobject obj, jstring filepath) {
    //string path = env->GetStringUTFChars(filepath, 0);
    const char *cFilepath = env->GetStringUTFChars(filepath, 0);
    string path = string(cFilepath);
    //string path = "/storage/emulated/0/YoonScanner/ysc_1471604293709.jpg";

    env->ReleaseStringUTFChars(filepath, cFilepath);

    //string path = string(env->GetStringUTFChars(filepath, 0));
    Mat src = imread(path);
//	printf("%s\n", argv[1]);
//	imshow("source", src);
	int largest_area = 0;
	int largest_contour_index = 0;
	Rect bounding_rect;

	Mat thr;
	cvtColor(src, thr, COLOR_BGR2GRAY); //Convert to gray
	//필터한번하자
	threshold(thr, thr, 125, 255, THRESH_BINARY); //Threshold the gray

	vector<vector<Point> > contours; // Vector for storing contours

	findContours(thr, contours, RETR_CCOMP, CHAIN_APPROX_SIMPLE); // Find the contours in the image

	for (size_t i = 0; i < contours.size(); i++) // iterate through each contour.
	{
		double area = contourArea(contours[i]);  //  Find the area of contour

		//src면적이랑 area가 같으면 제외
		//if (src.total != area) {
			if (area > largest_area)
			{
				largest_area = area;
				largest_contour_index = i;               //Store the index of largest contour
				bounding_rect = boundingRect(contours[i]); // Find the bounding rectangle for biggest contour
			}
		//}
	}

	RotatedRect bounding_rect2;
	bounding_rect2 = minAreaRect(Mat(contours[largest_contour_index]));
/*	Point2f rect_points[4]; bounding_rect2.points(rect_points);
	printf("ro1-%f,%f\n", rect_points[0].x, rect_points[0].y);
	printf("ro2-%f,%f\n", rect_points[1].x, rect_points[1].y);
	printf("ro3-%f,%f\n", rect_points[2].x, rect_points[2].y);
	printf("ro4-%f,%f\n", rect_points[3].x, rect_points[3].y);
	printf("ro4-%f,%f\n", rect_points[4].x, rect_points[4].y); */
	//draw
	int largest_distance_index = 0;
	double largest_distance = 0;
	Point	corner[4];
	//첫번째
	for (size_t i = 0; i < contours[largest_contour_index].size(); i++) // iterate through each contour.
	{
		double distance = sqrt(pow(contours[largest_contour_index][0].x - contours[largest_contour_index][i].x, 2) + pow(contours[largest_contour_index][0].y - contours[largest_contour_index][i].y, 2));
		if (distance > largest_distance)
		{
			largest_distance = distance;
			largest_distance_index = i;               //Store the index of largest contour
			corner[0] = contours[largest_contour_index][largest_distance_index];
		}
	}
	largest_distance_index = 0;
	largest_distance = 0;
	//2번째
	for (size_t i = 0; i < contours[largest_contour_index].size(); i++) // iterate through each contour.
	{
		double distance = sqrt(pow(corner[0].x - contours[largest_contour_index][i].x, 2) + pow(corner[0].y - contours[largest_contour_index][i].y, 2));
		if (distance > largest_distance)
		{
			largest_distance = distance;
			largest_distance_index = i;               //Store the index of largest contour
			corner[1] = contours[largest_contour_index][largest_distance_index];
		}
	}
	//3번쨰
	largest_distance_index = 0;
	largest_distance = 0;
	for (size_t i = 0; i < contours[largest_contour_index].size(); i++) // iterate through each contour.
	{
		double distance = sqrt(pow(corner[0].x - contours[largest_contour_index][i].x, 2) + pow(corner[0].y - contours[largest_contour_index][i].y, 2))
						+ sqrt(pow(corner[1].x - contours[largest_contour_index][i].x, 2) + pow(corner[1].y - contours[largest_contour_index][i].y, 2));
		if (distance > largest_distance)
		{
			largest_distance = distance;
			largest_distance_index = i;               //Store the index of largest contour
			corner[2] = contours[largest_contour_index][largest_distance_index];
		}
	}
	// 네 번 째 꼭지점 추출
	// (벡터 내적을 이용하여 좌표평면에서 사각형의 너비의 최대 값을 구한다.)
	//														 thanks to 송성원
	int x1 = corner[0].x;	int y1 = corner[0].y;
	int x2 = corner[1].x;	int y2 = corner[1].y;
	int x3 = corner[2].x;	int y3 = corner[2].y;
	int nMaxDim = 0;

	for (size_t i = 0; i < contours[largest_contour_index].size(); i++) // iterate through each contour.
	{
		int nDim = abs((x1 * y2 + x2 * contours[largest_contour_index][i].y + contours[largest_contour_index][i].x  * y1) - (x2 * y1 + contours[largest_contour_index][i].x  * y2 + x1 * contours[largest_contour_index][i].y))
					+ abs((x1 * contours[largest_contour_index][i].y + contours[largest_contour_index][i].x  * y3 + x3 * y1) - (contours[largest_contour_index][i].x  * y1 + x3 * contours[largest_contour_index][i].y + x1 * y3))
					+ abs((contours[largest_contour_index][i].x  * y2 + x2 * y3 + x3 * contours[largest_contour_index][i].y) - (x2 * contours[largest_contour_index][i].y + x3 * y2 + contours[largest_contour_index][i].x  * y3));

		if (nDim > nMaxDim)
		{
			nMaxDim = nDim;
			int max = i;
			corner[3] = contours[largest_contour_index][max];
		}
	}

/*	printf("0-%d,%d\n", corner[0].x, corner[0].y);
	printf("1-%d,%d\n", corner[1].x, corner[1].y);
	printf("2-%d,%d\n", corner[2].x, corner[2].y);
	printf("3-%d,%d\n\n", corner[3].x, corner[3].y);
	*/
	Mat src2 = src.clone();
	drawContours(src2, contours, largest_contour_index, Scalar(0, 255, 0), 2); // Draw the largest contour using previously stored index.
//	imshow("result", src2);

	int cornersum[4] = {};

	for (int i = 0; i < 4; i++) {
		cornersum[i]= corner[i].x + corner[i].y;
		//printf("%d\n", cornersum[i]);
	}

	int max = cornersum[0];
	int min = cornersum[0];
	int imax = 0;
	int imin = 0;

	for (int i = 1; i < 4; i++) {

		if (cornersum[i] > max) {
			max = cornersum[i];
			imax = i;
		}
		if (cornersum[i] < min) {
			min = cornersum[i];
			imin = i;
		}
	}
	Point TopLeft = corner[imin];
	Point BottomRight = corner[imax];

	Point corner2[2];
	int j = 0;
	for (int i = 0; i < 4; i++) {
		if (i != imax&& i != imin) {
			corner2[j] = corner[i];
			j++;
		}
	}

	Point BottomLeft = (corner2[0].x < corner2[1].x) ? corner2[0] : corner2[1];
	Point TopRight = (corner2[0].x > corner2[1].x) ? corner2[0] : corner2[1];

	//와핑

	/*Point TopLeft = corner[imin];
	Point TopRight = corner[3];
	Point BottomLeft = corner[2];
	Point BottomRight = corner[imax];*/

	vector<Point> rect;
	rect.push_back(TopLeft);    // 왼쪽 위
	rect.push_back(TopRight);    // 오른쪽 위
	rect.push_back(BottomRight);    // 오른쪽 아래
	rect.push_back(BottomLeft);    // 왼쪽 아래

	double w1 = sqrt(pow(BottomRight.x - BottomLeft.x, 2)
		+ pow(BottomRight.x - BottomLeft.x, 2));
	double w2 = sqrt(pow(TopRight.x - TopLeft.x, 2)
		+ pow(TopRight.x - TopLeft.x, 2));

	double h1 = sqrt(pow(TopRight.y - BottomRight.y, 2)
		+ pow(TopRight.y - BottomRight.y, 2));
	double h2 = sqrt(pow(TopLeft.y - BottomLeft.y, 2)
		+ pow(TopLeft.y - BottomLeft.y, 2));

	double maxWidth = (w1 < w2) ? w1 : w2;
	double maxHeight = (h1 < h2) ? h1 : h2;
	//a4용지 비율
	maxWidth = (maxWidth < maxHeight) ? maxWidth : maxHeight*sqrt(2.0);
	maxHeight = (maxWidth < maxHeight) ? maxWidth*sqrt(2.0) : maxHeight;
	Point2f src1[4], dst[4];
	src1[0] = Point2f(TopLeft.x, TopLeft.y);
	src1[1] = Point2f(TopRight.x, TopRight.y);
	src1[2] = Point2f(BottomRight.x, BottomRight.y);
	src1[3] = Point2f(BottomLeft.x, BottomLeft.y);

	dst[0] = Point2f(0, 0);
	dst[1] = Point2f(maxWidth - 1, 0);
	dst[2] = Point2f(maxWidth - 1, maxHeight - 1);
	dst[3] = Point2f(0, maxHeight - 1);

	//Transformation Matrix 구하기

	Mat transformMatrix = getPerspectiveTransform(src1, dst);

	//Warping
	Size warpSize(maxWidth, maxHeight);

	Mat dstFrame(Size(maxWidth, maxHeight), src.type());

	warpPerspective(src, dstFrame, transformMatrix, Size(maxWidth, maxHeight));

	//
	//cout << maxWidth << ", " << maxHeight << ", " << w1 << ", " << w2 << ", " << h1 << ", " << h2 << endl;
	//선명하게
	Mat shrFrame;
	Mat kern = (Mat_<char>(3, 3) << 0, -1, 0, -1, 5, -1, 0, -1, 0);

	filter2D(dstFrame, shrFrame, dstFrame.depth(), kern);

//	imshow("sharpImg", shrFrame);

//	imshow("warpImg", dstFrame);

	//imshow("Image", src);
	string str = path;
//	cout << str << endl;//파일명
	int pos = str.rfind(".", str.length()-1);//뒤에서부터 . 검색
//	printf("%d\n", pos);
	//string str1 = 0;
	//string str2=0;
	string str1 = str.substr(0, pos);
	string str2 = str.substr(pos, str.length() - 1);
	string last = str1 + "_warp" + str2;

	imwrite(last,dstFrame);
//	waitKey(0);
	//return last;

    //jstring last2 =  env->NewStringUTF(last);
    //jstring last2 =  (*env)->NewStringUTF(env,last);
    //const char *cFilepathLast = last.c_str()
    //return (env)->NewStringUTF(cFilepathLast);
    //const char *cFilepathLast = "aaaaa";
    //return (env)->NewStringUTF(cFilepathLast);
    return (env)->NewStringUTF(last.c_str());
  }
}