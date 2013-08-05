package edu.berkeley.cellscope.cscore.celltracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Point;

import edu.berkeley.cellscope.cscore.cameraui.TouchPanControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchPanControl.PannableStage;
import edu.berkeley.cellscope.cscore.celltracker.MathUtils;
import edu.berkeley.cellscope.cscore.celltracker.PanTracker;
import edu.berkeley.cellscope.cscore.celltracker.PanTracker.PanCallback;

public class StepCalibrator implements PanTracker.PanCallback {
	private boolean busy;
	private boolean calibrated;
	Point xPosRate, xNegRate, yPosRate, yNegRate;
	private Point start, end;
	private PanTracker tracker;
	private TouchPanControl.PannableStage stage;
	private CalibratorCallback callback;
	PanCallback tCallback;
	private ScheduledExecutorService service;
	private boolean wait;
	private int step;
	private long timeStart, timeStop;
	private PanCommand xPos, xNeg, yPos, yNeg, stop;
	
	private static int X_POS = 0;
	private static int X_NEG = 1;
	private static int Y_POS = 2;
	private static int Y_NEG = 3;
	private static int DONE = 4;
	
	private static final int TIME = 8000; //milliseconds
	private static final TimeUnit UNIT = TimeUnit.MILLISECONDS;
	
	public StepCalibrator(PannableStage stage, PanTracker pt) {
		xPosRate = new Point();
        xNegRate = new Point();
        yPosRate = new Point();
        yNegRate = new Point();
        calibrated = false;
        busy = false;
        this.stage = stage;
        tracker = pt;
        start = new Point();
        end = new Point();
        
        xPos = new PanCommand(TouchPanControl.xRightMotor);
        xNeg = new PanCommand(TouchPanControl.xLeftMotor);
        yPos = new PanCommand(TouchPanControl.yForwardMotor);
        yNeg = new PanCommand(TouchPanControl.yBackMotor);
        stop = new PanCommand(TouchPanControl.stopMotor);
	}
	
	public void calibrate() {
		busy = true;
		service = Executors.newScheduledThreadPool(1);
		tCallback = tracker.callback;
		tracker.setCallback(this);
		if (!tracker.isTracking())
			tracker.enableTracking();
		step = X_POS;
		setWait(true);
	}
	
	private synchronized void setWait(boolean b) {
		wait = b;
	}
	
	public void onPanResult(Point result) {
		if (tCallback != null)
			tCallback.onPanResult(result);
		if (wait) {
			setWait(false);
			executeTask(result);
		}
	}
	
	private void executeTask(Point result) {
		if (step == X_POS) {
			MathUtils.set(start, result);
			timeStart = System.currentTimeMillis();
			service.schedule(xPos, 0, UNIT);
			service.schedule(stop, TIME, UNIT);

			if (callback != null)
				callback.toastMessage("Calibrating positive x...");
		}
		else if (step == X_NEG) {
			MathUtils.set(end, result);
			long timeDiff = UNIT.toSeconds(timeStop - timeStart);
			MathUtils.set(xPosRate, start, end);
			MathUtils.divide(xPosRate, timeDiff);

			MathUtils.set(start, result);
			timeStart = System.currentTimeMillis();
			service.schedule(xNeg, 0, UNIT);
			service.schedule(stop, TIME, UNIT);

			if (callback != null)
				callback.toastMessage("Calibrating negative x...");
		}
		else if (step == Y_POS) {
			MathUtils.set(end, result);
			long timeDiff = timeStop - timeStart;
			MathUtils.set(xNegRate, start, end);
			MathUtils.divide(xNegRate, timeDiff);

			MathUtils.set(start, result);
			timeStart = System.currentTimeMillis();
			service.schedule(yPos, 0, UNIT);
			service.schedule(stop, TIME, UNIT);

			if (callback != null)
				callback.toastMessage("Calibrating positive y...");
		}
		else if (step == Y_NEG) {
			MathUtils.set(end, result);
			long timeDiff = timeStop - timeStart;
			MathUtils.set(yPosRate, start, end);
			MathUtils.divide(yPosRate, timeDiff);

			MathUtils.set(start, result);
			timeStart = System.currentTimeMillis();
			service.schedule(yNeg, 0, UNIT);
			service.schedule(stop, TIME, UNIT);
			
			if (callback != null)
				callback.toastMessage("Calibrating negative y...");
		}
		else if (step == DONE){
			MathUtils.set(end, result);
			long timeDiff = timeStop - timeStart;
			MathUtils.set(yNegRate, start, end);
			MathUtils.divide(yNegRate, timeDiff);
			
			service.shutdown();
			tracker.setCallback(tCallback);
			tCallback = null;
			busy = false;
			calibrated = true;
			tracker.disableTracking();
			
			System.out.println(xPosRate + " " + xNegRate + " " + yPosRate + " " + yNegRate);
			if (callback != null) {
	            callback.toastMessage("Calibration complete!");
				callback.calibrationComplete();
			}
		}
	}
	
	private void taskComplete() {
		timeStop = System.currentTimeMillis();
		setWait(true);
		step ++;
	}
	
	public boolean isCalibrated() {
		return calibrated;
	}
	
	public boolean isCalibrating() {
		return busy;
	}
	
	public static interface CalibratorCallback {
		public void calibrationComplete();
		public void toastMessage(String s);
	}
	
	public void setCallback(CalibratorCallback c) {
		callback = c;
	}

	private class PanCommand implements Runnable {
		int direction;
		
		PanCommand(int i) {
			direction = i;
		}
		public void run() {
			stage.panStage(direction);
			if (direction == TouchPanControl.stopMotor)
				taskComplete();
		}
	}

}
