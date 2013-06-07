package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.util.Log;

public class PanTrackActivity extends OpenCVCameraActivity {
	private Mat mRgba;
	private Mat lastImg;
	private Mat currImg;
	private Mat template;
	private Rect roi;
	private Point roiCorner1, roiCorner2;
	private boolean allocate;
	private int width, height;
	private static final String TAG = "Pan Tracker";
	private static Scalar RED = new Scalar(255, 0, 0, 255);
	private static Scalar GREEN = new Scalar(0, 255, 0, 255);
	private static Scalar BLUE = new Scalar(0, 0, 255, 255);
	private static final double SAMPLE_SIZE = 0.4;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		allocate = true;
	}
	
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        if (allocate) {
        	currImg = new Mat();
        	lastImg = new Mat();
        	template = new Mat();
            width = mRgba.cols();
            height = mRgba.rows();
            roiCorner1 = new Point((int) (width / 2 - width * SAMPLE_SIZE / 2), (int) (height / 2 - height * SAMPLE_SIZE / 2));
            roiCorner2 = new Point(roiCorner1.x + (int)(width * SAMPLE_SIZE), roiCorner1.y + (int)(height * SAMPLE_SIZE));
            roi = new Rect(roiCorner1, roiCorner2);
        }
        
        mRgba.copyTo(currImg);
        if (!allocate) {
        	currImg.submat(roi).copyTo(template);
        	Point result = locate(lastImg, template);
            Point corner = new Point(result.x + (int)(width * SAMPLE_SIZE), result.y + (int)(height * SAMPLE_SIZE));
        	Core.rectangle(mRgba, roiCorner1, roiCorner2, GREEN);
        	Core.rectangle(mRgba, result, corner, BLUE);
        	Log.e(TAG, "Panned " + (result.x - roiCorner1.x) + " along x and " + (result.y - roiCorner1.y)+ " along y" );
        }
        else
        	allocate = false;
        currImg.copyTo(lastImg);
        return mRgba;
    }
	
	private Point locate(Mat img, Mat templ) {
		int result_cols =  img.cols() - templ.cols() + 1;
		int result_rows = img.rows() - templ.rows() + 1;
		Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
		Imgproc.matchTemplate( img, templ, result, Imgproc.TM_CCORR_NORMED);
		Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1);
		Core.MinMaxLocResult minMax = Core.minMaxLoc(result);
		return minMax.maxLoc;
	}

}
