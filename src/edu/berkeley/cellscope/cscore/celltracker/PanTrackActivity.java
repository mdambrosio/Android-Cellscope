package edu.berkeley.cellscope.cscore.celltracker;

import java.util.ArrayList;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;

import android.os.Bundle;

public class PanTrackActivity extends OpenCVCameraActivity {
	private Mat mRgba;
	private Mat lastRgba;
	private Point point;
	MatOfKeyPoint keypointMat1, keypointMat2;
	Mat descMat1, descMat2;
	MatOfDMatch matches;
	FeatureDetector features;
	DescriptorExtractor extractor;
	DescriptorMatcher matcher;
	boolean firstrun, allocate;
	double x, y;
	ArrayList<Double> transX;
	ArrayList<Double> transY;
	private static Scalar RED = new Scalar(255, 0, 0, 255);
	private static Scalar GREEN = new Scalar(0, 255, 0, 255);
	private static Scalar BLUE = new Scalar(0, 0, 255, 255);
	private static final double LOWER_TOLERANCE = 0.2;
	private static final double UPPER_TOLERANCE = 1.8;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		point = new Point();
		firstrun = true;
		allocate = true;
		transX = new ArrayList<Double>();
		transY = new ArrayList<Double>();
		x = y = 0;
	}
	
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
	//	System.out.println("FRAME");
        mRgba = inputFrame.rgba();
        if (allocate) {
        	allocate = false;
        	System.out.println("REFRESH");
        	keypointMat1 = new MatOfKeyPoint();
            keypointMat2 = new MatOfKeyPoint();
            features = FeatureDetector.create(FeatureDetector.BRISK);
            extractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);
            matches = new MatOfDMatch();
            descMat1 = new Mat();
            descMat2 = new Mat();
            matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
        }
        

    	features.detect(mRgba, keypointMat1);
        extractor.compute(mRgba, keypointMat1, descMat1);
        if (lastRgba != null) {
	        extractor.compute(lastRgba, keypointMat2, descMat2);
	        if (keypointMat1.total() != 0 && keypointMat2.total() != 0) {
	        	matcher.match(descMat1, descMat2, matches);
	        
	        //Core.circle(mRgba, point, 10, COLOR, 3);
	        /*
	        KeyPoint[] points = keypointMat1.toArray();
	        for (KeyPoint p: points)
	        	Core.circle(mRgba, p.pt, 10, RED, 3);*/
		        DMatch[] matchArr = matches.toArray();
		        KeyPoint[] pts1 = keypointMat1.toArray();
		        KeyPoint[] pts2 = keypointMat2.toArray();
		        double distX = 0, distY = 0;
		        transX.clear();
		        transY.clear();
		        for (DMatch d: matchArr) {
		        	Point pt1 = pts1[d.queryIdx].pt;
		        	Point pt2 = pts2[d.trainIdx].pt;
		        	Core.circle(mRgba, pt1, 4, RED, 2);
		        	Core.circle(mRgba, pt2, 4, BLUE, 2);
		        	Core.line(mRgba, pt1, pt2, GREEN, 2);
		        	double deltaX = (pt1.x - pt2.x);
		        	double deltaY = (pt1.y - pt2.y);
		        	transX.add(deltaX);
		        	transY.add(deltaY);
		        	distX += deltaX;
		        	distY += deltaY;
		        }
		        int len = transX.size();
		        double aveX = distX / matchArr.length;
		        double aveY = distY / matchArr.length;
		        double lowerX = aveX * LOWER_TOLERANCE, upperX = aveX * UPPER_TOLERANCE;
		        double lowerY = aveY * LOWER_TOLERANCE, upperY = aveY * UPPER_TOLERANCE;
		        
		        double sumX = 0, sumY = 0;
		        int countX = 0, countY = 0;
		        for (int i = 0; i < len; i ++) {
		        	double tmpX = transX.get(i);
		        	double tmpY = transY.get(i);
		        	if (tmpX > lowerX && tmpX < upperX) {
		        		sumX += tmpX;
		        		countX ++;
		        	}
		        	if (tmpY > lowerY && tmpY < upperY) {
		        		sumY += tmpY;
		        		countY ++;
		        	}
		        }
		        if (countX > 0)
		        	x += sumX / countX;
		        if (countY > 0)
		        	y += sumY / countY;
		        System.out.println(x + " " + y + " " + distX + " " + distY );
	        	//if (matches.total() > 0)
	        	//Features2d.drawMatches(mRgba, keypointMat1, lastRgba, keypointMat2, matches, mRgba);
	        }
		}
        MatOfKeyPoint tmp = keypointMat1;
        keypointMat1 = keypointMat2;
        keypointMat2 = tmp;
        lastRgba = mRgba;
        return mRgba;
    }

}
