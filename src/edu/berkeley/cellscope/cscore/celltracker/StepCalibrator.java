package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Point;

import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

/*
 * Calibration steps:
 * 1. Calibrator started. Calibrator instructs Tracker to begin.
 * 2. Tracker notifies Calibrator of frame, then pauses.
 * 3. Calibrator saves Tracker data.
 * 4. Calibrator instructs stage to move.
 * 5. Stage notifies calibrator that the stage has stopped moving.
 * 6. Calibrator instructs Tracker to resume.
 * 7. Tracker notifies Calibrator of frame, then pauses.
 * 6. Repeat steps 2 thru 5.
 */
public class StepCalibrator implements FovTracker.MotionCallback {
	private boolean busy;
	private boolean calibrated;
	Point xPosRate, xNegRate, yPosRate, yNegRate;
	private Point accumulated;
	private Point[] results;
	private int currentState;
	private FovTracker tracker;
	private Calibratable stage;
	private Calibratable callback;
	FovTracker.MotionCallback tCallback;
	private int substep;
	
	private static int[] DIR_ORDER = new int[]{TouchSwipeControl.xRightMotor, TouchSwipeControl.xLeftMotor, TouchSwipeControl.yForwardMotor, TouchSwipeControl.yBackMotor};
	//private static int[] DIR_ORDER = new int[]{TouchSwipeControl.yForwardMotor, TouchSwipeControl.yForwardMotor, TouchSwipeControl.yForwardMotor, TouchSwipeControl.yForwardMotor};
	private static int[] STEPS = new int[]{8, 8, 7, 7, 6, 6, 5, 5, 4, 4};
	private static int REALIGN_STEP_SIZE = 24;
	private static int REALIGN_STEP = -1;
	
	public StepCalibrator(Calibratable s, FovTracker pt) {
		xPosRate = new Point();
        xNegRate = new Point();
        yPosRate = new Point();
        yNegRate = new Point();
        results = new Point[]{xPosRate, xNegRate, yPosRate, yNegRate};
        calibrated = false;
        busy = false;
        stage = s;
        tracker = pt;
        accumulated = new Point();
	}
	
	public void calibrate() {
		busy = true;
		currentState = 0;
		substep = REALIGN_STEP;
		calibrated = false;
		tCallback = tracker.callback;
		tracker.setCallback(this);
		if (!tracker.isTracking())
			tracker.enableTracking();
		tracker.resumeTracking();
	}
	
	public void onMotionResult(Point result) {
		if (tCallback != null) {
			tCallback.onMotionResult(result);
		}
		saveResult(result);
		executeStep();
		tracker.pauseTracking();
	}

	public void saveResult(Point point) {
		int prevState = currentState;
		int prevStep = substep - 1;
		if (prevStep < REALIGN_STEP) {
			prevStep = STEPS.length - 1;
			prevState --;
		}
		System.out.println("save " + prevStep);
		if (prevState >= 0 && prevStep != REALIGN_STEP) {
			System.out.println("accumulated " + point);
			MathUtils.add(accumulated, point);
			if (prevStep == STEPS.length -1) {
				System.out.println("recording... " + prevState);
				int weights = 0;
				for (int i = 0; i < STEPS.length; i ++)
					weights += STEPS[i];
				MathUtils.divide(accumulated, weights);
				MathUtils.set(results[prevState], accumulated);
				MathUtils.set(accumulated, 0, 0);
			}
		}
		else
			MathUtils.set(accumulated, 0, 0);
	}
	
	public void executeStep() {
		if (currentState < DIR_ORDER.length) {
			int steps = substep == REALIGN_STEP ? REALIGN_STEP_SIZE : STEPS[substep];
			stage.swipe(DIR_ORDER[currentState], steps);
			System.out.println("swipe " + DIR_ORDER[currentState] + " " + steps + "\n.");
		}
		else
			calibrationComplete();
		
		substep ++;
		if (substep >= STEPS.length) {
			substep = REALIGN_STEP;
			currentState ++;
		}
	}
	
	public void notifyMovementCompleted() {
		System.out.println("resumed");
		tracker.resumeTracking();
	}
	
	public boolean isCalibrated() {
		return calibrated;
	}
	
	public boolean isCalibrating() {
		return busy;
	}
	
	private void calibrationComplete() {
		tracker.setCallback(tCallback);
		tCallback = null;
		busy = false;
		calibrated = true;
		tracker.disableTracking();
		System.out.println(xPosRate);
		System.out.println(xNegRate);
		System.out.println(yPosRate);
		System.out.println(yNegRate);
	}
	
	public void setCallback(Calibratable c) {
		callback = c;
	}
	
	public static interface Calibratable extends TouchSwipeControl.Swipeable {
		public void calibrationComplete();
	}

}
