package edu.berkeley.cellscope.cscore.celltracker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

public class Autofocus {
	private TouchSwipeControl stage;
	private boolean busy;
	private boolean saveFrame;
	private int stepSize;
	private int currentPosition, wait;
	private int bestPosition, bestScore;
	private ExecutorService calcThread;
	
	private final Object lockStatus, lockData;
	private static final int INITIAL_STEP = 32;
	private static final int PAUSE = 3;
	private static final double THRESHOLD_RATIO = 1;
	private static final double LOWER_THRESHOLD = 64;
	
	public Autofocus(TouchSwipeControl s) {
		stage = s;
		busy = false;
		saveFrame = false;
		lockStatus = new Object();
		lockData = new Object();
	}
	
	public void focus() {
		if (busy || !stage.bluetoothConnected())
			return;
		System.out.println("begin focus");
		busy = true;
		currentPosition = bestPosition = bestScore = 0;
		stepSize = INITIAL_STEP;
		motionComplete();
	}
	
	public void queueFrame(Mat mat) {
		synchronized(lockStatus) {
			if (!saveFrame)
				return;
			if (wait > 0) {
				wait --;
				return;
			}
			saveFrame = false;
		}
		Mat data = new Mat(mat.size(), mat.type());
		mat.copyTo(data);
		if (calcThread == null)
			startCalculationThread();
		calcThread.submit(new FocusCalculation(data, currentPosition));
		if (currentPosition <= 512) {
			currentPosition += stepSize;
			stage.swipe(TouchSwipeControl.zDownMotor, stepSize);
		}
		else {
			focusComplete();
		}
	}
	
	public void motionComplete() {
		synchronized(lockStatus) {
			saveFrame = true;
			wait = PAUSE; //Wait several frames for blur to subside
		}
	}
	
	public boolean isFocusing() {
		return busy;
	}
	
	public void focusFailed() {
		
	}
	
	public void focusComplete() {
		busy = false;
	}
	
	private void startCalculationThread() {
		stopCalculationThread();
		calcThread = Executors.newSingleThreadExecutor();
	}
	
	private void stopCalculationThread() {
		if (calcThread != null)
			calcThread.shutdown();
		calcThread = null;
	}
	
	private class FocusCalculation implements Runnable {
		private Mat img;
		private int pos;
		public FocusCalculation(Mat m, int i) {
			img = m;
			pos = i;
		}
		public void run() {
			Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
			Imgproc.Canny(img, img, LOWER_THRESHOLD, LOWER_THRESHOLD * THRESHOLD_RATIO);
			int score = Core.countNonZero(img);
			updateBest(pos, score);
		}
	}
	
	private synchronized void updateBest(int position, int score) {
		System.out.println("result: " + position + " " + score);
		if (score > bestScore) {
			bestPosition = position;
			bestScore = score;
		}
	}
	
	public static interface Autofocusable {
		public void queueAutofocusFrame(Mat m);
		public void notifyAutofocus(int message);
	}
}
