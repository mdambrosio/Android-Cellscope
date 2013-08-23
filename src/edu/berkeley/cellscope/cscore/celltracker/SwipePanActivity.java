package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Point;

import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.cameraui.TouchControl.BluetoothControllable;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;
import edu.berkeley.cellscope.cscore.celltracker.StepCalibrator.CalibrationCallback;

/*
 * Class for testing the stepper counter.
 * 
 * Observations: commands do not queue. Sending multiple commands at once
 * to the stage will cause the first command to be executed, but all commands' steps
 * to count down. When the first command is done executing (i.e. no more steps remaining), 
 * the next command will be executed if it has remaining steps.
 */
public class SwipePanActivity extends OpenCVCameraActivity implements CalibrationCallback, FovTracker.MotionCallback {
	private MenuItem mMenuItemCalibrate;
	
	protected TouchSwipeControl touchSwipe;
	private StepCalibrator calibrator;
	private FovTracker tracker;
	private StepNavigator navigator;
	
	@Override
	protected void createAddons(int width, int height) {
		super.createAddons(width, height);
		touchPan.setEnabled(false);
		touchSwipe = new NavigationSwipeControl(this, width, height);
		touchSwipe.setEnabled(true);
		compoundTouch.addTouchListener(touchSwipe);
    	tracker = new FovTracker(width, height);
    	tracker.setCallback(this);
        calibrator = new StepCalibrator(touchSwipe, tracker);
        calibrator.setCallback(this);
        TouchSwipeControl navSwipe = new TouchSwipeControl(this, width, height);
        navigator = new StepNavigator(navSwipe, tracker, calibrator, autofocus);
        realtimeProcessors.add(tracker);
        realtimeProcessors.add(calibrator);
        realtimeProcessors.add(navigator);
	}
	
	@Override
	public void readMessage(Message msg) {
		super.readMessage(msg);
		byte[] buffer = (byte[])(msg.obj);
		if (buffer.length > 0) {
			int message = (int)(buffer[0]);
			if (calibrator.isRunning()) {
				//if (message == BluetoothControllable.PROCEED)
					calibrator.continueRunning();
				//else if (message == BluetoothControllable.FAILED)
				//	calibrator.calibrationFailed();
			}
			else if (navigator.isRunning()) {
				//if (message == BluetoothControllable.PROCEED)
					navigator.continueRunning();
				//else if (message == BluetoothControllable.FAILED)
				//	navigator.stop();
			}
		}
	}
	
	
	/* Override this to perform post-calculation operations
	 * in subclasses.
	 */
	public void onMotionResult(Point result) {
		//System.out.println(result);
	}
	
	@Override
    public void bluetoothConnected() {
		super.bluetoothConnected();
		if (mMenuItemCalibrate != null) {
			mMenuItemCalibrate.setEnabled(true);
		}
    }
    
	@Override
    public void bluetoothDisconnected() {
		super.bluetoothDisconnected();
		if (mMenuItemCalibrate != null) {
			//mMenuItemCalibrate.setEnabled(false);
		}
    }
	
	@Override
	public boolean controlReady() {
		return super.controlReady() && !calibrator.isRunning();
	}
	
	public void hideControls() {
		takePicture.setVisibility(View.INVISIBLE);
		toggleRecord.setVisibility(View.INVISIBLE);
	}
	
	public void showControls() {
		takePicture.setVisibility(View.VISIBLE);
        toggleRecord.setVisibility(View.VISIBLE);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_calibrate, menu);
        mMenuItemCalibrate = menu.getItem(2);
        //mMenuItemCalibrate.setEnabled(false);
        return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if (super.onOptionsItemSelected(item))
			return true;
		int id = item.getItemId();
		if (id == R.id.calibrate) {
			runStageCalibration();
			return true;
		}
		return false;
    }
	
	public void runStageCalibration() {
		calibrator.start();
	}
	
	public void calibrationComplete(boolean success) {
		if (success)
			toast(StepCalibrator.SUCCESS_MESSAGE);
		else
			toast(StepCalibrator.FAILURE_MESSAGE);
	}
	
	private class NavigationSwipeControl extends TouchSwipeControl {

		public NavigationSwipeControl(BluetoothControllable s, int w, int h) {
			super(s, w, h);
		}
		
		@Override
		public void swipeStage(double x, double y) {
			System.out.println("test " + x + " " + y);
			if (!stage.controlReady() || navigator.isRunning())
				return;
			System.out.println("pass");
			navigator.setTarget((int)x, (int)y);
			navigator.start();
		}
		
	}
	
}
