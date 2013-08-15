package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Point;

import android.app.Activity;
import edu.berkeley.cellscope.cscore.ScreenDimension;
import edu.berkeley.cellscope.cscore.cameraui.TouchControl.BluetoothControllable;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;
import edu.berkeley.cellscope.cscore.celltracker.StepCalibrator.CalibrationCallback;

public class StepNavigator implements FovTracker.MotionCallback {
	private final StepCalibrator calibrator;
	private final TouchSwipeControl stage;
	private final Autofocus autofocus;
	private final FovTracker tracker;
	
	public StepNavigator(BluetoothControllable bt, Activity activity) {
		int width = ScreenDimension.getScreenWidth(activity);
		int height = ScreenDimension.getScreenHeight(activity);
		stage = new TouchSwipeControl(bt, activity); 
		tracker = new FovTracker(width, height);
		calibrator = new StepCalibrator(stage, tracker);
		tracker.setCallback(this);
		autofocus = new Autofocus(stage);
	}
	
	public void onMotionResult(Point result) {
		
	}
	
	public void setCalibratorCallback(CalibrationCallback c) {
		calibrator.setCallback(c);
	}
}
