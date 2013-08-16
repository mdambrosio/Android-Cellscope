package edu.berkeley.cellscope.cscore.celltracker;

import java.util.Arrays;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchControl.BluetoothControllable;

/*
 * Runs x/y motor calibration to determine how many pixels a step is.
 * The result is not precise or accurate, so use it to calculate roughly
 * the number of steps necessary to reach a certain position. It is not
 * reliable enough to be used directly in gathering data.
 * 
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
public class StepCalibrator implements RealtimeImageProcessor, FovTracker.MotionCallback {
	private boolean busy;
	private boolean calibrated;
	private boolean processSub;
	private Point xPosRate, xNegRate, yPosRate, yNegRate;
	private Point accumulated;
	private Point[] results;
	private int currentState;
	private FovTracker tracker;
	private TouchSwipeControl stage;
	private CalibrationCallback callback;
	private FovTracker.MotionCallback tCallback;
	private int substep;
	
	private static int[] DIR_ORDER = new int[]{TouchSwipeControl.xPositive, TouchSwipeControl.xNegative, TouchSwipeControl.yPositive, TouchSwipeControl.yNegative};
	//private static int[] DIR_ORDER = new int[]{TouchSwipeControl.yForwardMotor, TouchSwipeControl.yForwardMotor, TouchSwipeControl.yForwardMotor, TouchSwipeControl.yForwardMotor};
	private static int[] STEPS = new int[]{8, 8, 7, 7, 6, 6, 5, 5, 4, 4};
	private static int REALIGN_STEP_SIZE = 24;
	private static int REALIGN_STEP = -1;
	
	public static final int PROCEED = 0;
	public static final int FAILED = 1;
	
	public static final String SUCCESS_MESSAGE = "Calibration successful";
	public static final String FAILURE_MESSAGE = "Calibration failed";

	public StepCalibrator(BluetoothControllable bt, int w, int h) {
		TouchSwipeControl ctrl = new TouchSwipeControl(bt, w, h);
		FovTracker track = new FovTracker(w, h);
		processSub = true;
		init(ctrl, track);
	}
	
	public StepCalibrator(TouchSwipeControl s, FovTracker pt) {
		processSub = false;
		init(s, pt);
	}
	
	private void init(TouchSwipeControl s, FovTracker pt) {
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
	
	public void start() {
		if (busy || !stage.bluetoothConnected())
			return;
		busy = true;
		currentState = 0;
		substep = REALIGN_STEP;
		calibrated = false;
		tCallback = tracker.callback;
		tracker.setCallback(this);
		if (!tracker.isRunning())
			tracker.start();
		tracker.resume();
	}
	
	public void onMotionResult(Point result) {
		if (tCallback != null) {
			tCallback.onMotionResult(result);
		}
		saveResult(result);
		executeStep();
		tracker.pause();
	}

	private void saveResult(Point point) {
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
	
	private void executeStep() {
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
	
	public void processFrame(Mat mat) {
		if (processSub)
			tracker.processFrame(mat);
	}

	public void displayFrame(Mat mat) {
		if (processSub)
			tracker.displayFrame(mat);
	}
	
	public void proceedWithCalibration() {
		System.out.println("resumed");
		tracker.resume();
	}
	
	public boolean isCalibrated() {
		return calibrated;
	}
	
	public boolean isRunning() {
		return busy;
	}
	
	private void calibrationComplete() {
		tracker.setCallback(tCallback);
		tCallback = null;
		busy = false;
		calibrated = true;
		tracker.stop();
		Point tmp = MathUtils.subtract(xPosRate.clone(), xNegRate);
		MathUtils.multiply(tmp, 0.5);
		MathUtils.set(xPosRate, tmp);
		MathUtils.set(xNegRate, MathUtils.multiply(tmp, -1));
		tmp = MathUtils.subtract(yPosRate.clone(), yNegRate);
		MathUtils.multiply(tmp, 0.5);
		MathUtils.set(yPosRate, tmp);
		MathUtils.set(yNegRate, MathUtils.multiply(tmp, -1));
		System.out.println(Arrays.toString(results));
		callback.calibrationComplete(true);
	}
	
	/*
	 * Convert a target location in the screen's x-y to the number of steps in the motor's x-y.
	 */
	public Point getSteps(Point target) {
		double a1 = xPosRate.x, a2 = xPosRate.y, b1 = yPosRate.x, b2 = yPosRate.y;
		double det = (a1 * b2 - b1 * a2);
		double x1 = target.x, x2 = target.y;
		double c1 = (x1 * b2) + (-b1 * x2);
		double c2 = (x1 * -a2) + (b2 * x2);
		c1 /= det;
		c2 /= det;
		return new Point(c1, c2);
	}
	
	public void stop() {
		calibrationFailed();
	}
	
	public void calibrationFailed() {
		tracker.setCallback(tCallback);
		tCallback = null;
		busy = false;
		calibrated = false;
		tracker.stop();
		callback.calibrationComplete(true);
	}
	
	public void setCallback(CalibrationCallback c) {
		callback = c;
	}
	
	public static interface CalibrationCallback {
		public void calibrationComplete(boolean success);
		public void notifyCalibrator(int message);
	}

}
