package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import edu.berkeley.cellscope.cscore.cameraui.TouchControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

/*
 * Determines the number of motor steps that backlash will consume.
 */
public class StepCalibrator implements RealtimeImageProcessor {
	private boolean busy;
	private boolean calibrated;
	private int xPosBacklash, xNegBacklash, yPosBacklash, yNegBacklash;
	private Point xPosStep, xNegStep, yPosStep, yNegStep;
	public int xBacklash, yBacklash;
	private Point xStep, yStep;
	private int[] backlashResults;
	private Point[] stepResults;
	private int currentState, currentDir;
	private int wait;
	private boolean continueCalibration;
	private int trackerResponses;
	private int moves;
	private FovTracker[] trackers;
	private TrackerResult[] trackerResults;
	private TouchSwipeControl stage;
	private CalibrationCallback callback;
	
	private static final int STATE_RESET = 0;
	private static final int STATE_WAIT = 1;
	private static final int STATE_BACKLASH = 2;
	private static final int STATE_STEP = 3;
	
	private static final int[] MOVE_DIR = new int[]{TouchControl.xPositive, TouchControl.xNegative, TouchControl.yPositive, TouchControl.yNegative};
	private static final int[] RESET_DIR = new int[]{TouchControl.xNegative, TouchControl.xPositive, TouchControl.yNegative, TouchControl.yPositive};
	
	private static final int REQUIRED_BACKLASH_MOVES = 6;
	private static final int STRIDE_SIZE = 3;
	private static final int REQUIRED_STRIDES = 6;
	
	private static final double TRACKER_SIZE = 0.05;
	private static final double TRACKER_SPACING = 0.07;
	private static final int TRACKER_COUNT = 4;
	
	private static final int BACKLASH_LIMIT = 42;
	private static final int WAIT_FRAMES = 2;
	public static final String SUCCESS_MESSAGE = "Calibration successful";
	public static final String FAILURE_MESSAGE = "Calibration failed";

	
	/* StepCalibrator MUST use the same TouchSwipeControl as StepNavigator or anything else that uses these results
	 * to correctly account for backlash.
	 */
	public StepCalibrator(TouchSwipeControl s, int w, int h) {
        calibrated = false;
        busy = false;
        stage = s;
        trackers = new FovTracker[TRACKER_COUNT];
        int dimen = w < h ? w: h;
        int spacing = (int)(dimen * TRACKER_SPACING);
        int size = (int)(dimen * TRACKER_SIZE);
        Size rectSize = new Size(size, size);
        Point loc = new Point();
        MathUtils.set(loc, w / 2 - spacing, h / 2 - spacing);
        trackers[0] = new FovTracker(w, h, MathUtils.createCenteredRect(loc, rectSize));
        trackers[0].setPause(2);
       
        MathUtils.set(loc, w / 2 + spacing, h / 2 - spacing);
        trackers[1] = new FovTracker(w, h, MathUtils.createCenteredRect(loc, rectSize));
        trackers[1].setPause(2);
       
        MathUtils.set(loc, w / 2 - spacing, h / 2 + spacing);
        trackers[2] = new FovTracker(w, h, MathUtils.createCenteredRect(loc, rectSize));
        trackers[2].setPause(2);
        
        MathUtils.set(loc, w / 2 + spacing, h / 2 + spacing);
        trackers[3] = new FovTracker(w, h, MathUtils.createCenteredRect(loc, rectSize));
        trackers[3].setPause(2);
        
        trackerResults = new TrackerResult[TRACKER_COUNT];
        for (int i = 0; i < TRACKER_COUNT; i ++)
        	trackerResults[i] = new TrackerResult(trackers[i]);
        
        xPosStep = new Point();
        xNegStep = new Point();
        yPosStep = new Point();
        yNegStep = new Point();
        xStep = new Point();
        yStep = new Point();
        backlashResults = new int[4];
        stepResults = new Point[]{xPosStep, xNegStep, yPosStep, yNegStep};
	}
	
	public void start() {
		if (busy || !stage.bluetoothConnected())
			return;
		busy = true;
		System.out.println("begin calibration");
		currentState = STATE_RESET;
		currentDir = 0;
		calibrated = false;
		continueCalibration = false;
		moves = 0;
		for (int i = 0; i < backlashResults.length; i ++) {
			backlashResults[i] = 0;
			MathUtils.set(stepResults[i], 0, 0);
		}
		for (FovTracker tracker: trackers)
			tracker.start();
	}
	
	private void executeCalibration() {
		if (currentState == STATE_RESET) {
			System.out.println("reset " + currentDir);
			if (RESET_DIR[currentDir] == TouchSwipeControl.stopMotor)
				continueRunning();
			else
				stage.swipe(RESET_DIR[currentDir], BACKLASH_LIMIT);
			wait = WAIT_FRAMES;
			toNextStep();
		}
		else if (currentState == STATE_WAIT) {
			System.out.println("wait " + currentDir);
			wait --;
			if (wait <= 0)
				toNextStep();
			continueRunning();
		}
		else if (currentState == STATE_BACKLASH) {
			System.out.println("backlash " + currentDir);
			if (!continueCalibration)
				moves ++;
			else
				moves = 0;
			if (moves == REQUIRED_BACKLASH_MOVES) {
				backlashResults[currentDir] -= moves * STRIDE_SIZE;
				moves = 0;
				toNextStep();
				stage.swipe(MOVE_DIR[currentDir], STRIDE_SIZE);
			}
			else {
				continueCalibration = false;
				backlashResults[currentDir] += STRIDE_SIZE;
				if (backlashResults[currentDir] >= BACKLASH_LIMIT && moves == 0)
					calibrationFailed();
				else
					stage.swipe(MOVE_DIR[currentDir], STRIDE_SIZE);
			}
		}
		else if (currentState == STATE_STEP) {
			System.out.println("step " + currentDir);
			for (int i = 0; i < TRACKER_COUNT; i ++)
				MathUtils.add(stepResults[currentDir], trackerResults[i].movement);
			System.out.println(stepResults[currentDir]);
			moves ++;
			if (moves == REQUIRED_STRIDES) {
				moves = 0;
				if (toNextStep())
					calibrationComplete();
				else
					stage.swipe(MOVE_DIR[currentDir], STRIDE_SIZE);
			}
			else
				stage.swipe(MOVE_DIR[currentDir], STRIDE_SIZE);
		}
	}
	
	//return true when done
	private boolean toNextStep() {
		currentState ++;
		if (currentState > STATE_STEP) {
			currentState = STATE_RESET;
			currentDir ++;
			if (currentDir >= MOVE_DIR.length)
				return true;
		}
		return false;
	}
	
	public void processFrame(Mat mat) {
		for (FovTracker tracker: trackers)
			tracker.processFrame(mat);
	}

	public void displayFrame(Mat mat) {
		for (FovTracker tracker: trackers)
			tracker.displayFrame(mat);
	}
	
	public void continueRunning() {
		for (FovTracker tracker: trackers)
			tracker.resume();
	}
	
	
	public boolean isCalibrated() {
		return calibrated;
	}
	
	public boolean isRunning() {
		return busy;
	}
	
	private void calibrationComplete() {
		System.out.println("calibration complete");
		for (FovTracker tracker: trackers)
			tracker.stop();
		System.out.println("Step sizes: ");
		System.out.println("x - " + xPosStep + " " + xNegStep);
		System.out.println("y - " + yPosStep + " " + yNegStep);
		for (int i = 0; i < stepResults.length; i ++)
			MathUtils.divide(stepResults[i], TRACKER_COUNT * REQUIRED_STRIDES * STRIDE_SIZE);
		xPosBacklash = backlashResults[0];
		xNegBacklash = backlashResults[1];
		yPosBacklash = backlashResults[2];
		yNegBacklash = backlashResults[3];
		xBacklash = (xPosBacklash + xNegBacklash) / 2;
		yBacklash = (yPosBacklash + yNegBacklash) / 2;
		MathUtils.set(xStep, xPosStep);
		MathUtils.subtract(xStep, xNegStep);
		MathUtils.divide(xStep, 2);
		MathUtils.set(yStep, yPosStep);
		MathUtils.subtract(yStep, yNegStep);
		MathUtils.divide(yStep, 2);
		busy = false;
		calibrated = true;
		callback.calibrationComplete(true);
		System.out.println("Backlash: ");
		System.out.println("x - "+ xPosBacklash + " " + xNegBacklash);
		System.out.println("y - "+ yPosBacklash + " " + yNegBacklash);
		System.out.println("Step sizes: ");
		System.out.println("x - " + xPosStep + " " + xNegStep);
		System.out.println("y - " + yPosStep + " " + yNegStep);
	}
	
	
	public void stop() {
		calibrationFailed();
	}
	
	public void calibrationFailed() {
		System.out.println("calibration failed");
		for (FovTracker tracker: trackers)
			tracker.stop();
		busy = false;
		calibrated = false;
		callback.calibrationComplete(false);
	}
	
	public void setCallback(CalibrationCallback c) {
		callback = c;
	}
	
	private synchronized void continueCalibration(boolean result) {
		continueCalibration = continueCalibration || result;
	}
	
	private synchronized boolean trackerResponsesComplete() {
		trackerResponses ++;
		if (trackerResponses == TRACKER_COUNT) {
			trackerResponses = 0;
			return true;
		}
		return false;
	}
	
	public static interface CalibrationCallback {
		public void calibrationComplete(boolean success);
	}
	
	private class TrackerResult implements FovTracker.MotionCallback {
		private FovTracker tracker;
		Point movement;
		
		private TrackerResult(FovTracker ft) {
			tracker = ft;
			tracker.setCallback(this);
			movement = new Point();
		}
		
		public synchronized void onMotionResult(Point result) {
			continueCalibration(result.x == 0 && result.y == 0);
			tracker.pause();
			MathUtils.set(movement, result);
			if (trackerResponsesComplete())
				executeCalibration();
		}
	}

	/*
	 * Convert a target location in the screen's x-y to the number of steps in the motor's x-y.
	 */
	public Point getRequiredStrides(Point target) {
		if (!calibrated)
			return new Point(0, 0);
		double a1 = xStep.x, a2 = xStep.y, b1 = yStep.x, b2 = yStep.y;
		double det = (a1 * b2 - b1 * a2);
		double x1 = target.x, x2 = target.y;
		double c1 = (x1 * b2) + (-b1 * x2);
		double c2 = (x1 * -a2) + (b2 * x2);
		c1 /= det;
		c2 /= det;
		/*int xDir = (c1 > 0) ? TouchSwipeControl.xPositive : TouchSwipeControl.xNegative;
		int yDir = (c2 > 0) ? TouchSwipeControl.yPositive : TouchSwipeControl.yNegative;
		if (stage.backlashOccurs(xDir)) {
			if (c1 > 0)
				c1 += xBacklash;
			else if (c1 < 0)
				c1 -= xBacklash;
		}
		if (stage.backlashOccurs(yDir)) {
			if (c2 > 0)
				c2 += yBacklash;
			else if (c1 < 0)
				c2 -= yBacklash;
		}*/
		return new Point(c1, c2);
	}

}
