package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.os.Bundle;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.berkeley.cellscope.cscore.R;
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
	private MenuItem mMenuItemTrackPan;
	
	protected TouchSwipeControl touchSwipe;
	private StepCalibrator calibrator;
	private FovTracker tracker;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		touchPan.setEnabled(false);
		touchSwipe = new TouchSwipeControl(this, this);
		touchSwipe.setEnabled(true);
		
		compoundTouch.addTouchListener(touchSwipe);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (calibrator.isCalibrating())
			calibrator.calibrationFailed();
	}
	
	@Override
	public void readMessage(Message msg) {
		super.readMessage(msg);
		byte[] buffer = (byte[])(msg.obj);
		System.out.println("message read + " + buffer[0]);
		if (buffer.length > 0 && calibrator.isCalibrating()) {
			notifyCalibrator((int)buffer[0]);
		}
	}
	
	public void notifyCalibrator(int message) {
		if (message == StepCalibrator.PROCEED)
			calibrator.proceedWithCalibration();
		else if (message == StepCalibrator.FAILED)
			calibrator.calibrationFailed();
	}
	
	public void enableTracking() {
		tracker.enableTracking();
		if (mMenuItemTrackPan != null)
			mMenuItemTrackPan.setTitle(R.string.track_pan_disable);
	}
	
	public void disableTracking() {
		tracker.disableTracking();
		if (mMenuItemTrackPan != null)
			mMenuItemTrackPan.setTitle(R.string.track_pan_enable);
	}
	
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        super.onCameraFrame(inputFrame);
        if (tracker.isTracking()) {
        	tracker.track(mRgba);
        	tracker.draw(mRgba);
        }
        return mRgba;
    }
	
	/* OpenCV-related classes cannot be constructed onCreate, because OpenCV is loaded
	 * asynchronously. onCreate is likely to be called before loading is complete.
	 */
	@Override
	public void onCameraViewStarted(int width, int height) {
    	super.onCameraViewStarted(width, height);
    	tracker = new FovTracker(width, height);
    	tracker.setCallback(this);
        calibrator = new StepCalibrator(touchSwipe, tracker);
        calibrator.setCallback(this);
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
		return super.controlReady() && !calibrator.isCalibrating();
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
        mMenuItemTrackPan = menu.getItem(2);
        mMenuItemCalibrate = menu.getItem(3);
        //mMenuItemCalibrate.setEnabled(false);
        return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if (super.onOptionsItemSelected(item))
			return true;
		int id = item.getItemId();
		if (id == R.id.track_pan) {
			if (tracker.isTracking())
				disableTracking();
			else
				enableTracking();
			return true;
		}
		else if (id == R.id.calibrate) {
			runStageCalibration();
			return true;
		}
		return false;
    }
	
	public void runStageCalibration() {
		calibrator.calibrate();
	}
	
	public void calibrationComplete(boolean success) {
	}
	
}
