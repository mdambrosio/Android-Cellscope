package edu.berkeley.cellscope.cscore.celltracker;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;

public class PanTrackActivity extends OpenCVCameraActivity {
	private Mat mRgba;
	private Mat lastImg;
	private Mat currImg;
	private Mat template;
	private Rect roi;
	private Point roiCorner1, roiCorner2;
	private volatile Point result, corner;
	private int frame;
	private boolean allocate;
	private int width, height;
	private PositionCalculator currentTask;
	private PositionCalculation calculation;
	private ExecutorService calcThread;
	private volatile int activeThreads;
	private static final String TAG = "Pan Tracker";
	private static Scalar RED = new Scalar(255, 0, 0, 255);
	private static Scalar GREEN = new Scalar(0, 255, 0, 255);
	private static Scalar BLUE = new Scalar(0, 0, 255, 255);
	private static final int TRACK_INTERVAL = 1; //Minimum number of frames between every update
	//A larger sample size will give greater accuracy for slow pans, but cannot detect fast pans
	private static final double SAMPLE_SIZE_Y = 0.4; 
	private static final double SAMPLE_SIZE_X = 0.6;
	private static final int THREAD_CAP = 1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		allocate = true;
		activeThreads = 0;
	}
	
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        frame ++;

        if (allocate) {
        	currImg = new Mat();
        	lastImg = new Mat();
        	template = new Mat();
            width = mRgba.cols();
            height = mRgba.rows();
            roiCorner1 = new Point((int) (width / 2 - width * SAMPLE_SIZE_X / 2), (int) (height / 2 - height * SAMPLE_SIZE_Y / 2));
            roiCorner2 = new Point(roiCorner1.x + (int)(width * SAMPLE_SIZE_X), roiCorner1.y + (int)(height * SAMPLE_SIZE_Y));
            roi = new Rect(roiCorner1, roiCorner2);
            result = new Point();
            corner = new Point();
            calculation = new PositionCalculation(currImg, lastImg);
            calcThread = Executors.newSingleThreadExecutor();
        }
        
        if (frame == TRACK_INTERVAL) {
        	frame = 0;
        	
        	//currImg = mRgba;

    		synchronized(calculation) {
    			mRgba.copyTo(currImg);
    		}
            if (!allocate) {
            	/*if (currentTask == null || currentTask.isDone()) {
            		currentTask = new PositionCalculator(calculation);
            		currentTask.run();
            		System.out.println(currentTask.isDone());
            	}*/
            	if (!threadCapReached()) {
            		calcThread.execute(calculation);
            		updateThreadCount(1);
            	}
            }
            else {
            	allocate = false;
            	mRgba.copyTo(lastImg);
            }
            //lastImg = currImg;
        }
        

    	Core.rectangle(mRgba, roiCorner1, roiCorner2, GREEN);
    	Core.rectangle(mRgba, result, corner, BLUE);
        return mRgba;
    }
	
	private synchronized boolean threadCapReached() {
		return activeThreads == THREAD_CAP;
	}
	
	private synchronized void updateThreadCount(int i) {
		activeThreads += i;
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
	
	private class PositionCalculator extends FutureTask<Point> {
		public PositionCalculator(Callable<Point> callable) {
			super(callable);
		}
	}
	
	private class PositionCalculation implements Callable<Point>, Runnable {
		Mat curr, last;
		public PositionCalculation(Mat currImg, Mat lastImg) {
			curr = new Mat();
			last = new Mat();
		}
		public Point call() {
			synchronized (calculation) {
				currImg.copyTo(curr);
				lastImg.copyTo(last);
                currImg.copyTo(lastImg);
			}
			curr.submat(roi).copyTo(template);
        	Point location = locate(last, template);
        	result.x = location.x;
        	result.y = location.y;
            corner.x = result.x + (int)(width * SAMPLE_SIZE_X);
            corner.y = result.y + (int)(height * SAMPLE_SIZE_Y);
        	System.out.println("Panned " + (result.x - roiCorner1.x) + " along x and " + (result.y - roiCorner1.y)+ " along y" );
    		updateThreadCount(-1);
            return result;
		}
		public void run() {
			call();
		}
		
		
		
	}

}
