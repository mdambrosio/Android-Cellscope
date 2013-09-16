package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import edu.berkeley.cellscope.cscore.cameraui.TouchControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchControl.BluetoothControllable;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

public class StepNavigator implements RealtimeImageProcessor {
	private StepCalibrator calibrator;
	private TouchSwipeControl stage;
	private Autofocus autofocus;
	private boolean processSub;
	private Point target;
	private boolean moving;
	private boolean yMoved;
	private int xDirection, yDirection, xSteps, ySteps;
	
	public StepNavigator(BluetoothControllable bt, int w, int h) {
		TouchSwipeControl ctrl = new TouchSwipeControl(bt, w, h);
		StepCalibrator calib = new StepCalibrator(ctrl, w, h);
		Autofocus focus = new Autofocus(ctrl);
		processSub = true;
		init(ctrl, calib, focus);
	}
	
	public StepNavigator(TouchSwipeControl ctrl, StepCalibrator calib, Autofocus focus) {
		processSub = false;
		init(ctrl, calib, focus);
	}
	
	private void init(TouchSwipeControl ctrl, StepCalibrator calib, Autofocus focus) {
		stage = ctrl;
		calibrator = calib;
		autofocus = focus;
		target = new Point();
	}
	
	public void proceed() {
	}
	
	public void setTarget(int x, int y) {
		if (!calibrator.isCalibrated())
			return;
		MathUtils.set(target, x, y);
		setTarget(target);
	}
	
	private boolean setTarget(Point pt) {
		MathUtils.set(target, pt.x, pt.y);
		Point movement = calibrator.getRequiredSteps(target);
		xSteps = (int) Math.round(Math.abs(movement.x));
		ySteps = (int) Math.round(Math.abs(movement.y));
		xDirection = (movement.x > 0) ? TouchControl.xNegative : TouchControl.xPositive;
		yDirection = (movement.y > 0) ? TouchControl.yNegative : TouchControl.yPositive;
		if (xSteps > 0 && stage.backlashOccurs(xDirection))
			xSteps += calibrator.xBacklash;
		if (ySteps > 0 && stage.backlashOccurs(yDirection))
			ySteps += calibrator.yBacklash;
		System.out.println(xDirection + " " + xSteps);
		System.out.println(yDirection + " " + ySteps);
		return true;
	}
	
	public void start() {
		System.out.println("trigger");
		if (!calibrator.isCalibrated() || !stage.bluetoothConnected() || calibrator.isRunning() || autofocus.isRunning())
			return;
		System.out.println("pass");
		moving = true;
		yMoved = false;
		if (xSteps > 0)
			stage.swipe(xDirection, xSteps);
		else
			continueRunning();
	}
	
	public void stop() {
		moving = false;
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

	public void continueRunning() {
		if (!yMoved && ySteps > 0) {
			yMoved = true;
			stage.swipe(yDirection, ySteps);
		}
		else
			stop();
	}
	
}
