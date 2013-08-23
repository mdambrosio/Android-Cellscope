package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import edu.berkeley.cellscope.cscore.cameraui.TouchControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchControl.BluetoothControllable;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

public class StepNavigator implements RealtimeImageProcessor, FovTracker.MotionCallback {
	private StepCalibrator calibrator;
	private TouchSwipeControl stage;
	private Autofocus autofocus;
	private FovTracker tracker;
	private boolean processSub;
	private FovTracker.MotionCallback tCallback;
	private Point target;
	private boolean moving, first;
	private int direction, steps;
	
	private static final int[] STEP_SIZES = new int[]{8, 6, 4, 2};
	
	public StepNavigator(BluetoothControllable bt, int w, int h) {
		TouchSwipeControl ctrl = new TouchSwipeControl(bt, w, h);
		FovTracker track = new FovTracker(w, h);
		StepCalibrator calib = new StepCalibrator(ctrl, track);
		Autofocus focus = new Autofocus(ctrl);
		processSub = true;
		init(ctrl, track, calib, focus);
	}
	
	public StepNavigator(TouchSwipeControl ctrl, FovTracker track, StepCalibrator calib, Autofocus focus) {
		processSub = false;
		init(ctrl, track, calib, focus);
	}
	
	private void init(TouchSwipeControl ctrl, FovTracker track, StepCalibrator calib, Autofocus focus) {
		stage = ctrl;
		tracker = track;
		calibrator = calib;
		autofocus = focus;
		target = new Point();
	}
	
	public void proceed() {
		tracker.resume();
	}
	
	public void setTarget(int x, int y) {
		if (!calibrator.isCalibrated())
			return;
		MathUtils.set(target, x, y);
		setTarget(target);
	}
	
	private boolean setTarget(Point pt) {
		MathUtils.set(target, pt.x, pt.y);
		Point movement = calibrator.getSteps(target);
		int xSteps = (int) Math.round(Math.abs(movement.x));
		int ySteps = (int) Math.round(Math.abs(movement.y));
		int xDir = (movement.x > 0) ? TouchControl.xPositive : TouchControl.xNegative;
		int yDir = (movement.y > 0) ? TouchControl.yPositive : TouchControl.yNegative;
		System.out.println(xSteps + " " + ySteps);
		for (int size: STEP_SIZES) {
			if (xSteps > ySteps && xSteps >= size) {
				direction = xDir;
				steps = size;
				return false;
			}
			else if (ySteps > xSteps && ySteps >= size) {
				direction = yDir;
				steps = size;
				return false;
			}
		}
		if (first) {
			first = false;
			return false;
		}
		stop();
		return true;
	}
	
	public void start() {
		System.out.println("trigger");
		if (!calibrator.isCalibrated() || !stage.bluetoothConnected() || calibrator.isRunning() || autofocus.isRunning())
			return;
		System.out.println("pass");
		moving = true;
		first = true;
		tCallback = tracker.callback;
		tracker.setCallback(this);
		if (!tracker.isRunning())
			tracker.start();
		tracker.resume();
	}
	
	public void stop() {
		moving = false;
		tracker.setCallback(tCallback);
		tracker.stop();
	}
	
	public boolean isRunning() {
		return moving;
	}

	public void processFrame(Mat mat) {
		if (processSub) {
			if (autofocus.isRunning())
				autofocus.processFrame(mat);
			if (tracker.isRunning())
				tracker.processFrame(mat);
			if (calibrator.isRunning())
				calibrator.processFrame(mat);
		}
	}

	public void displayFrame(Mat mat) {
		if (processSub) {
			if (autofocus.isRunning())
				autofocus.displayFrame(mat);
			if (tracker.isRunning())
				tracker.displayFrame(mat);
			if (calibrator.isRunning())
				calibrator.displayFrame(mat);
		}
	}
	
	public void continueRunning() {
		tracker.resume();
	}
	

	public void onMotionResult(Point result) {
		if (tCallback != null) {
			tCallback.onMotionResult(result);
		}
		System.out.println(target);
		tracker.pause();
		MathUtils.subtract(target, result);
		if (!setTarget(target))
			stage.swipe(direction, steps);
	}
	
}
