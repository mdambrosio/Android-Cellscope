package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import edu.berkeley.cellscope.cscore.R;

public class PanTrackActivity extends OpenCVCameraActivity implements Calibrator.CalibratorCallback, PanTracker.TrackerCallback {
	
	private MenuItem mMenuItemCalibrate;
	private MenuItem mMenuItemTrackPan;
	
	private Calibrator calibrator;
	private PanTracker pantracker;
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
		pantracker.enableTracking();
		if (mMenuItemTrackPan != null)
			mMenuItemTrackPan.setTitle(R.string.track_pan_disable);
	}
	
	public void disableTracking() {
		pantracker.disableTracking();
		if (mMenuItemTrackPan != null)
			mMenuItemTrackPan.setTitle(R.string.track_pan_enable);
	}
	
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        super.onCameraFrame(inputFrame);
        synchronized(calibrator) {
        	if (reenableControls) {
        		reenableControls = false;
        	}
        }
        if (pantracker.isTracking()) {
        	pantracker.track(mRgba);
        	pantracker.draw(mRgba);
        }
        //if (preparedMessage != null)
        //	Toast.makeText(getApplicationContext(), preparedMessage, Toast.LENGTH_SHORT).show();
        return mRgba;
    }
	
	/* OpenCV-related classes cannot be constructed onCreate, because OpenCV is loaded
	 * asynchronously. onCreate is likely to be called before loading is complete.
	 */
	@Override
	public void initialFrame() {
            pantracker = new PanTracker(mRgba);
            pantracker.setCallback(this);
            calibrator = new Calibrator(this, pantracker);
            calibrator.setCallback(this);
	}
	
	/* Override this to perform post-calculation operations
	 * in subclasses.
	 */
	public void onTrackResult(Point result) {
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
        toggleTimelapse.setVisibility(View.INVISIBLE);
	}
	
	public void showControls() {
		takePicture.setVisibility(View.VISIBLE);
        toggleTimelapse.setVisibility(View.VISIBLE);
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
			if (pantracker.isTracking())
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
		synchronized (calibrator) {
			reenableControls = true;
		}
	}
	
	public void toastMessage(String s) {
		preparedMessage = s;
	}
	
}
