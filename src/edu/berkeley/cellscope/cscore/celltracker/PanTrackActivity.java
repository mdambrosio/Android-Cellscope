package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.berkeley.cellscope.cscore.R;

public class PanTrackActivity extends OpenCVCameraActivity implements FovTracker.MotionCallback {
	
	private MenuItem mMenuItemCalibrate;
	private MenuItem mMenuItemTrackPan;
	
	private FovTracker tracker;
	private boolean reenableControls;
	private String preparedMessage;
	
	private static final String TAG = "Pan Tracker";
	protected static Scalar RED = new Scalar(255, 0, 0, 255);
	protected static Scalar GREEN = new Scalar(0, 255, 0, 255);
	protected static Scalar BLUE = new Scalar(0, 0, 255, 255);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
        //if (preparedMessage != null)
        //	Toast.makeText(getApplicationContext(), preparedMessage, Toast.LENGTH_SHORT).show();
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
		return false;
    }
	
	public void toastMessage(String s) {
		preparedMessage = s;
	}
	
}
