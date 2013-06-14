package edu.berkeley.cellscope.cscore.celltracker;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class PanTracker {
	private final Mat currImg, lastImg;
	private final int width, height;
	
	private final Rect roi; //area in center of screen used as cross-correlation template
	private final Point roiCorner1, roiCorner2; //defines corners of Rect roi
	private final Point translation; //gives the position of the current frame, relative to the last frame
	private final Point panCorner1, panCorner2;
	private final Point result;
	
	private boolean tracking;

	private PositionCalculation calculation;
	private ExecutorService calcThread; //Calculations are executed in this thread.
	private volatile int queuedCalcs;
	
	protected int frameCounter;
	protected boolean firstFrame;
	
	TrackerCallback callback;
	
	protected static final int TRACK_INTERVAL = 1; //Minimum number of frames between every update
	private static final int CALC_CAP = 1; //Maximum number of queued calculations. No real point in having this more than 1.
	//A larger sample size will give greater accuracy for slow pans, but cannot detect fast pans
	private static final double SAMPLE_SIZE_Y = 0.3; 
	private static final double SAMPLE_SIZE_X = 0.45;
	
	protected static Scalar RED = new Scalar(255, 0, 0, 255);
	protected static Scalar GREEN = new Scalar(0, 255, 0, 255);
	protected static Scalar BLUE = new Scalar(0, 0, 255, 255);
	
	
	public PanTracker(Mat screen) {
    	currImg = new Mat();
    	lastImg = new Mat();
        width = screen.cols();
        height = screen.rows();
        roiCorner1 = new Point((int) (width / 2 - width * SAMPLE_SIZE_X / 2), (int) (height / 2 - height * SAMPLE_SIZE_Y / 2));
        roiCorner2 = new Point(roiCorner1.x + (int)(width * SAMPLE_SIZE_X), roiCorner1.y + (int)(height * SAMPLE_SIZE_Y));
        roi = new Rect(roiCorner1, roiCorner2);
        translation = new Point();
        result = new Point();
        panCorner1 = new Point();
        panCorner2 = new Point();

        calculation = new PositionCalculation();
	}
	
	public boolean isTracking() {
		return tracking;
	}
	
	public void draw(Mat mRgba) {
		PointUtils.set(panCorner1, roiCorner1);
		PointUtils.add(panCorner1, translation);
		PointUtils.set(panCorner2, roiCorner2);
		PointUtils.add(panCorner2, translation);
		
    	Core.rectangle(mRgba, roiCorner1, roiCorner2, GREEN);
    	Core.rectangle(mRgba, panCorner1, panCorner2, BLUE);
	}
	
	public synchronized void enableTracking() {
		PointUtils.set(translation, 0, 0);
		tracking = true;
		firstFrame = true;
		frameCounter = TRACK_INTERVAL;
        calcThread = Executors.newSingleThreadExecutor();
	}
	
	public synchronized void disableTracking() {
		tracking = false;
		calcThread.shutdown();
		calcThread = null;
	}
	
	public synchronized void track(Mat mRgba) {
		if (!tracking)
			return;
		frameCounter ++;
        if (frameCounter >= TRACK_INTERVAL) {
        	frameCounter = 0;
			updateCurrentFrame(mRgba);
	        if (firstFrame) {
	        	firstFrame = false;
	        	synchronized (this) {
	        		currImg.copyTo(lastImg);
	        	}
	        }
	        if (!isBusy()){
	    		calcThread.execute(calculation);
	    		updateCalcQueueCount(1);
	        }
        }
	}
	
	public void updateCurrentFrame(Mat mRgba) {
		synchronized(this) {
			mRgba.copyTo(currImg);
		}
	}
	
	public synchronized boolean isBusy() {
		return queuedCalcs == CALC_CAP;
	}
	
	public synchronized void updateCalcQueueCount(int i) {
		queuedCalcs += i;
	}
	
	public void setCallback(TrackerCallback c) {
		callback = c;
	}

	private class PositionCalculation implements Callable<Point>, Runnable {
		Mat curr, last, template;
		
		public PositionCalculation() {
			curr = new Mat();
			last = new Mat();
			template = new Mat();
		}
		
		public Point call() {
			//Compare the newest frame to the frame when the last calculation was run.
			synchronized (PanTracker.this) {
				currImg.copyTo(curr);
				lastImg.copyTo(last);
                currImg.copyTo(lastImg);
			}
			curr.submat(roi).copyTo(template);
        	Point location = locate(last, template);
        	if (location == null)
        		PointUtils.set(translation, 0, 0);
        	else {
	        	PointUtils.set(translation, location);
	        	PointUtils.subtract(translation, roiCorner1);
	    		updateCalcQueueCount(-1);
        	}
        	if (callback != null)
        		callback.onTrackResult(PointUtils.set(result, translation));
            return translation;
		}
		
		public void run() {
			call();
		}
	}
	
	public interface TrackerCallback {
		public void onTrackResult(Point result);
	}

	//Performs cross-correlation on two matrixes.
	public static Point locate(Mat img, Mat templ) {
		int result_cols =  img.cols() - templ.cols() + 1;
		int result_rows = img.rows() - templ.rows() + 1;
		Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
		Imgproc.matchTemplate( img, templ, result, Imgproc.TM_CCORR_NORMED);
		Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1);
		Core.MinMaxLocResult minMax = Core.minMaxLoc(result);
		if (minMax.maxVal == 0) //no correlation found, which will happen on the first several frames
			return null;
		return minMax.maxLoc;
	}
}
