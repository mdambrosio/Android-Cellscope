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
import edu.berkeley.cellscope.cscore.cameraui.TouchPanControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

/*
 * Class for testing the stepper counter.
 * 
 * Observations: commands do not queue. Sending multiple commands at once
 * to the stage will cause the first command to be executed, but all commands' steps
 * to count down. When the first command is done executing (i.e. no more steps remaining), 
 * the next command will be executed if it has remaining steps.
 */
public class SwipePanActivity extends OpenCVCameraActivity implements StepCalibrator.Calibratable, FovTracker.MotionCallback {
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
	
	public void swipe(int dir, int dist) {
		//System.out.println("swipe " + dir + " " + dist);
		byte[] buffer = new byte[1];
		buffer[0] = (byte)dir;
		btConnector.write(buffer);
		byte[] buffer2 = new byte[1];
		buffer2[0] = (byte)dist;
		btConnector.write(buffer2);
	}
	
	@Override
	public void readMessage(Message msg) {
		super.readMessage(msg);
		byte[] buffer = (byte[])(msg.obj);
		if (buffer.length > 0 && (int)buffer[0] == TouchPanControl.stopMotor)
			swipeComplete();
	}
	
	public void swipeComplete() {
		if (calibrator.isCalibrating())
			calibrator.notifyMovementCompleted();
	}
	
	public boolean swipeAvailable() {
		return panAvailable();
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
        calibrator = new StepCalibrator(this, tracker);
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
	public boolean panAvailable() {
		return super.panAvailable() && !calibrator.isCalibrating();
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
	
	public void calibrationComplete() {
	}
	
}
