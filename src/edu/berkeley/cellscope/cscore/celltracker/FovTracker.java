package edu.berkeley.cellscope.cscore.celltracker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/*
 * When enabled, will asynchronously calculate how much the contents of the screen move by.
 * This is done by sampling a small area on the screen and cross correlating its position several
 * frames later.
 * Note that if the screen moves too quickly and the sampled region moves off the field of view
 * before cross correlation can be run, the result will be false. Motion blur will also introduce error.
 * If the sample area being tracked lacks features, cross correlation will likely produce an invalid
 * result. It is advisable to 
 */
public class FovTracker implements RealtimeImageProcessor {
	private Mat currImg, lastImg;
	private int width, height;
	
	private Rect roi; //area in center of screen used as cross-correlation template
	private Point roiCorner1, roiCorner2; //defines corners of Rect roi
	private Point translation; //gives the position of the current frame, relative to the last frame
	private Point panCorner1, panCorner2;
	private Point result;
	private boolean busy;
	private boolean tracking, paused;
	private int waitDuration;
	private int wait;

	private PositionCalculation calculation;
	private ExecutorService calcThread; //Calculations are executed in this thread.
	
	protected int frameCounter;
	protected boolean firstFrame;
	
	MotionCallback callback;
	
	protected static final int TRACK_INTERVAL = 1; //Minimum number of frames between every update
	//A larger sample size will give greater accuracy for slow pans, but cannot detect fast pans
	private static final double SAMPLE_SIZE = 0.2;
	private static final int WAIT_AFTER_PAUSE = 2; //After resuming from pause, wait this many frames for the camera preview to catch up.
	
	protected static Scalar RED = new Scalar(255, 0, 0, 255);
	protected static Scalar GREEN = new Scalar(0, 255, 0, 255);
	protected static Scalar BLUE = new Scalar(0, 0, 255, 255);
	
	public FovTracker(int w, int h, Rect r) {
		init(w, h, r);
	}
	
	public FovTracker(int w, int h) {
        int sampleDimen = (w < h) ? (int)(w * SAMPLE_SIZE) : (int)(h * SAMPLE_SIZE);
        Point corner = new Point(w / 2 - sampleDimen / 2, h / 2 - sampleDimen / 2);
        Rect r = new Rect(corner, new Size(sampleDimen, sampleDimen));
        init(w, h, r);
	}
	
	private void init(int w, int h, Rect r) {
    	currImg = new Mat();
    	lastImg = new Mat();
        width = w;
        height = h;
        roi = r;
        roiCorner1 = roi.tl();
        roiCorner2 = roi.br();
        translation = new Point();
        result = new Point();
        panCorner1 = new Point();
        panCorner2 = new Point();
        waitDuration = WAIT_AFTER_PAUSE;
        calculation = new PositionCalculation();
	}
	
	public void setPause(int i) {
		waitDuration = i;
	}
	
	public boolean isRunning() {
		return tracking;
	}
	
	public void displayFrame(Mat mRgba) {
		MathUtils.set(panCorner1, roiCorner1);
		MathUtils.add(panCorner1, translation);
		MathUtils.set(panCorner2, roiCorner2);
		MathUtils.add(panCorner2, translation);
		
    	Core.rectangle(mRgba, roiCorner1, roiCorner2, GREEN);
    	Core.rectangle(mRgba, panCorner1, panCorner2, BLUE);
	}
	
	public void continueRunning() {
		
	}
	
	public synchronized void start() {
		MathUtils.set(translation, 0, 0);
		tracking = true;
		paused = false;
		firstFrame = true;
		frameCounter = TRACK_INTERVAL;
        calcThread = Executors.newSingleThreadExecutor();
	}
	
	public void pause() {
		synchronized(this) {
			paused = true;
		}
	}
	
	public void resume() {

		synchronized(this) {
			paused = false;
			wait = waitDuration;
		}
	}
	
	public void stop() {
		synchronized(this) {
			System.out.println("stopped");
			tracking = false;
			if (calcThread != null)
				calcThread.shutdown();
			calcThread = null;
		}
	}
	
	public void processFrame(Mat mRgba) {
		synchronized(this) {
			if (!tracking || paused)
				return;
			if (wait > 0) {
				wait --;
	    	//	currImg.copyTo(lastImg);
				return;
			}
			frameCounter ++;
	        if (frameCounter >= TRACK_INTERVAL) {
	        	frameCounter = 0;
				updateCurrentFrame(mRgba);
		        if (firstFrame) {
		        	firstFrame = false;
	        		currImg.copyTo(lastImg);
		        }
		        if (!isBusy()){
		        	//calculation.run();
		        	setBusy(true);
		    		calcThread.execute(calculation);
		        }
	        }
		}
	}
	
	public void updateCurrentFrame(Mat mRgba) {
		synchronized(this) {
			mRgba.copyTo(currImg);
		}
	}
	
	public synchronized boolean isBusy() {
		return busy;
		//return queuedCalcs == CALC_CAP;
	}
	
	public synchronized void setBusy(boolean b) {
		busy = b;
	}
	
	//public synchronized void updateCalcQueueCount(int i) {
	//	queuedCalcs += i;
	//}
	
	public void setCallback(MotionCallback c) {
		callback = c;
	}

	private class PositionCalculation implements Runnable {
		Mat curr, last, template;
		
		public PositionCalculation() {
			curr = new Mat();
			last = new Mat();
			template = new Mat();
		}
		
		public void run() {
			//Compare the newest frame to the frame when the last calculation was run.
			synchronized (FovTracker.this) {
				currImg.copyTo(curr);
				lastImg.copyTo(last);
                currImg.copyTo(lastImg);
			}
			curr.submat(roi).copyTo(template);
        	Point location = locate(last, template);
        	if (location == null)
        		MathUtils.set(translation, 0, 0);
        	else {
	        	MathUtils.set(translation, location);
	        	MathUtils.subtract(translation, roiCorner1);
	        	setBusy(false);
	    	//	updateCalcQueueCount(-1);
        	}
        	if (callback != null)
        		callback.onMotionResult(MathUtils.set(result, translation));
		}
	}
	
	public interface MotionCallback {
		public void onMotionResult(Point result);
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
