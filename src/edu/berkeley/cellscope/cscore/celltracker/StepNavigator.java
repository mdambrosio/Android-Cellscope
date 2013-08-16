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
	private FovTracker.MotionCallback tCallback;
	private Point target;
	private boolean moving;
	private int xDir, xSteps, yDir, ySteps;
	
	private static final int[] STEP_SIZES = new int[]{8, 4, 2, 1};
	
	public StepNavigator(BluetoothControllable bt, int w, int h) {
		TouchSwipeControl ctrl = new TouchSwipeControl(bt, w, h);
		FovTracker track = new FovTracker(w, h);
		StepCalibrator calib = new StepCalibrator(ctrl, track);
		Autofocus focus = new Autofocus(ctrl);
		init(ctrl, track, calib, focus);
	}
	
	public StepNavigator(TouchSwipeControl ctrl, FovTracker track, StepCalibrator calib, Autofocus focus) {
		init(ctrl, track, calib, focus);
	}
	
	private void init(TouchSwipeControl ctrl, FovTracker track, StepCalibrator calib, Autofocus focus) {
		stage = ctrl;
		tracker = track;
		tCallback = tracker.callback;
		tracker.setCallback(this);
		calibrator = calib;
		autofocus = focus;
		target = new Point();
	}
	

	public void onMotionResult(Point result) {
		if (tCallback != null) {
			tCallback.onMotionResult(result);
		}
		tracker.pause();
	}
	
	public void proceed() {
		tracker.resume();
	}
	
	public void setTargetPosition(int x, int y) {
		if (!calibrator.isCalibrated())
			return;
		MathUtils.set(target, x, y);
		Point movement = calibrator.getSteps(target);
		xSteps = (int) Math.round(Math.abs(movement.x));
		ySteps = (int) Math.round(Math.abs(movement.y));
		xDir = (movement.x > 0) ? TouchControl.xPositive : TouchControl.xNegative;
		yDir = (movement.y > 0) ? TouchControl.yPositive : TouchControl.yNegative;
	}
	
	public void start() {
		if (!calibrator.isCalibrated() || !stage.bluetoothConnected())
			return;
		moving = true;
	}
	
	public void stop() {
		moving = false;
	}
	
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	public void processFrame(Mat mat) {
		// TODO Auto-generated method stub
		
	}

	public void displayFrame(Mat mat) {
		// TODO Auto-generated method stub
		
	}
	
}
