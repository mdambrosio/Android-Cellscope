package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import edu.berkeley.cellscope.cscore.cameraui.TouchControl.BluetoothControllable;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

public class StepNavigator implements RealtimeImageProcessor, FovTracker.MotionCallback {
	private StepCalibrator calibrator;
	private TouchSwipeControl stage;
	private Autofocus autofocus;
	private boolean processSub;
	private Point target, steps;	//Target location and the distance moved so far
	private Point offtarget; //How off-target the final movement was--how many pixels the stage needs to move by to be on target
	private boolean moving;
	private FovTracker tracker;
	private boolean targetSet;
	
	private static final int STRIDE_SIZE = StepCalibrator.STRIDE_SIZE;
	
	public StepNavigator(BluetoothControllable bt, int w, int h) {
		TouchSwipeControl ctrl = new TouchSwipeControl(bt, w, h);
		StepCalibrator calib = new StepCalibrator(ctrl, w, h);
		Autofocus focus = new Autofocus(ctrl);
		FovTracker track = new FovTracker(w, h);
		processSub = true;
		init(calib, focus, track);
	}
	
	public StepNavigator(StepCalibrator calib, Autofocus focus, FovTracker track) {
		processSub = false;
		init(calib, focus, track);
	}
	
	private void init(StepCalibrator calib, Autofocus focus, FovTracker track) {
		stage = calib.getStageController();
		calibrator = calib;
		autofocus = focus;
		tracker = track;
		tracker.setCallback(this);
		target = new Point();
		offtarget = new Point();
	}

	public void setTarget(int x, int y) {
		if (!calibrator.isCalibrated())
			return;
		MathUtils.set(target, x, y);
		MathUtils.set(offtarget, target);
		calibrator.getRequiredSteps(target);
		targetSet = true;
		steps.x = (steps.x > 0) ? (int)(steps.x + 0.5) : (int)(steps.x - 0.5); //round no. of steps
		steps.y = (steps.y > 0) ? (int)(steps.y + 0.5) : (int)(steps.y - 0.5);

		calibrator.adjustBacklash(steps);
	}
	
	public void start() {
		System.out.println("initiate navigation");
		if (!calibrator.isCalibrated() || !stage.bluetoothConnected() || calibrator.isRunning()
				|| autofocus.isRunning() || isRunning() || !targetSet)
			return;
		System.out.println("prerequisites fulfilled");
		targetSet = false;
		moving = true;
		tracker.start();
	}
	
	public void stop() {
		moving = false;
		tracker.stop();
	}
	
	public boolean isRunning() {
		return moving;
	}

	public void processFrame(Mat mat) {
		if (processSub) {
			if (autofocus.isRunning())
				autofocus.processFrame(mat);
			if (calibrator.isRunning())
				calibrator.processFrame(mat);
		}
	}

	public void displayFrame(Mat mat) {
		if (processSub) {
			if (autofocus.isRunning())
				autofocus.displayFrame(mat);
			if (calibrator.isRunning())
				calibrator.displayFrame(mat);
		}
	}

	/** Called when motor completes stride. */
	public void continueRunning() {
		tracker.pause();
	}

	public void onMotionResult(Point result) {
		tracker.pause();
		MathUtils.subtract(offtarget, result);
		
		if (targetReached()) {
			stop();
		} else if (steps.y > steps.x) {
			steps.y -= STRIDE_SIZE;
			stage.swipeY(STRIDE_SIZE);
		} else if (steps.x > steps.y) {
			steps.x -= STRIDE_SIZE;
			stage.swipeX(STRIDE_SIZE);
		}
	}
	
	private boolean targetReached() {
		return steps.x < STRIDE_SIZE && steps.y < STRIDE_SIZE;
	}
	
	public Point getErrorDistance() {
		return offtarget;
	}
}
