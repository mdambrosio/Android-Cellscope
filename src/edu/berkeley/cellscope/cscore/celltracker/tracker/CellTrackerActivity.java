package edu.berkeley.cellscope.cscore.celltracker.tracker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import edu.berkeley.cellscope.cscore.CameraActivity;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.ScreenDimension;
import edu.berkeley.cellscope.cscore.celltracker.Colors;
import edu.berkeley.cellscope.cscore.celltracker.OpenCVCameraActivity;
import edu.berkeley.cellscope.cscore.celltracker.TrackedCallback;
import edu.berkeley.cellscope.cscore.celltracker.TrackedField;

public class CellTrackerActivity extends OpenCVCameraActivity implements TrackedCallback, View.OnTouchListener {
	
	private TrackedField field;
	private int screenWidth, screenHeight, imWidth, imHeight;
	private Point fovCenter;
	private int fovRadius;
	private int zoom, exposure;
	private MenuItem mMenuItemApply;
	private List<Rect> regions;
	private Rect selected;
	

	private String save;
	private boolean timelapse;
	private int interval;
	private File storageDir;
	private File outputFile;
	private String fileHeader;
	
	private boolean fieldReady;
	
	static final int TEST_WIDTH = 50;
	static final int TEST_HEIGHT = 50;
	
	private static final File defaultPictureDir = CameraActivity.mediaStorageDir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		screenWidth = ScreenDimension.getScreenWidth(this);
		screenHeight = ScreenDimension.getScreenHeight(this);
		
		Intent intent = getIntent(); 
		
		regions = new ArrayList<Rect>();
		int[] x = intent.getIntArrayExtra(ViewFieldActivity.DATA_X_INFO);
		int[] y = intent.getIntArrayExtra(ViewFieldActivity.DATA_Y_INFO);
		int[] w = intent.getIntArrayExtra(ViewFieldActivity.DATA_W_INFO);
		int[] h = intent.getIntArrayExtra(ViewFieldActivity.DATA_H_INFO);
		
		int size = x.length;
		for (int i = 0; i < size; i ++) {
			regions.add(new Rect(x[i], y[i], w[i], h[i]));
		}

		zoom = intent.getIntExtra(InitialCameraActivity.CAM_ZOOM_INFO, 0);
		exposure = intent.getIntExtra(InitialCameraActivity.CAM_EXPOSURE_INFO, 0);
		
		imWidth = intent.getIntExtra(ViewFieldActivity.IMG_WIDTH_INFO, screenWidth);
		imHeight = intent.getIntExtra(ViewFieldActivity.IMG_HEIGHT_INFO, screenHeight);

		int fovX = intent.getIntExtra(ViewFieldActivity.FOV_X_INFO, imWidth / 2);
		int fovY = intent.getIntExtra(ViewFieldActivity.FOV_Y_INFO, imHeight / 2);
		fovCenter = new Point(fovX, fovY);
		fovRadius = intent.getIntExtra(ViewFieldActivity.FOV_RADIUS_INFO, imHeight / 2);
		
		touchZoom.setEnabled(false);
		touchPan.setEnabled(false);
		toggleRecord.setVisibility(View.INVISIBLE);
		
		compoundTouch.addTouchListener(this);

		fieldReady = false;

		save = intent.getStringExtra(TrackerSettingsActivity.SAVE_INFO);
		if (save == null) save = "";
		timelapse = intent.getBooleanExtra(TrackerSettingsActivity.TIMELAPSE_INFO, false);
		interval = intent.getIntExtra(TrackerSettingsActivity.INTERVAL_INFO, TrackerSettingsActivity.DEFAULT_INTERVAL);
		
		String name = (save.length() == 0) ? "unsaved" : save;
		String intervalText = interval + "ms";
		String picturesSaved = timelapse ? "timelapse on" : "timelapse off";
		infoText.setText(name + " // " + intervalText + " // " + picturesSaved);
		
		if (save.length() == 0)
			storageDir = defaultPictureDir;
		else
			storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "cell_tracker_data/" + save);

		if (save.length() != 0) {
			outputFile = new File(storageDir.getPath() + File.separator + save + ".csv");
			fileHeader =  save + " data";
		}
		if (!storageDir.exists())
			storageDir.mkdirs();
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		super.onCameraViewStarted(width, height);
		cameraView.zoom(zoom);
		cameraView.adjustExposure(exposure);
	}
	
	@Override
	public void onCameraViewStopped() {
		super.onCameraViewStopped();
		if (field != null)
			field.stopTracking();
		resetField();
	}
	
	public void resetField() {
		synchronized(this) {
			if (field != null) {
				regions = field.getBoundingBoxes();
				field.resetData();
				fieldReady = false;
			}
			if (mMenuItemApply != null)
				mMenuItemApply.setVisible(true);
			toggleRecord.setVisibility(View.INVISIBLE);
		}
	}
	
	//the field must be instantiated using a valid frame, but openCV yields a blank screen on the first frame.
	public void startField() {
		if (field == null)
			field = new TrackedField(mRgba, fovCenter, fovRadius);
		else
			field.resetData();
		field.setOutputFile(outputFile, fileHeader);
		for (Rect r: regions)
			field.addObject(r);
		field.setCallback(this);
		field.initiateUpdateThread(interval);
		fieldReady = true;
	}
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		super.onCameraFrame(inputFrame);
		synchronized(this) {
			if (field == null || !fieldReady)
				return temporaryDisplay(mRgba);
			field.queueFrame(mRgba);
			return field.display();
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_celltracker, menu);
        mMenuItemApply = menu.getItem(0);
        inflater.inflate(R.menu.menu_bluetooth, menu);
        mMenuItemConnect = menu.getItem(1);
        return true;
    }
	
	@Override
	public void record() {
		return;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (super.onOptionsItemSelected(item))
    		return true;
    	int id = item.getItemId();
    	if (id == R.id.celltracker_apply) {
    		startField();
    		mMenuItemApply.setVisible(false);
    		toggleRecord.setVisibility(View.VISIBLE);
    	}
    	return true;
    }
    
    @Override
    public void toggleTimelapse(View v) {
    	if (field == null) {
    		startField();
    		return;
    	}
    	super.toggleTimelapse(v);
    	synchronized(this) {
	    	if (record) {
	    		field.startTracking();
	    	}
	    	else {
	    		field.stopTracking();
	    	}
    		
    	}
    }

	public void trackingUpdateComplete(Mat mat) {
		if (timelapse) {
			Bitmap bmp = Bitmap.createBitmap(imWidth, imHeight, Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(mat, bmp);
			savePicture(getPictureName(), bmp);
			bmp.recycle();
		}
	}
	
	public File getPictureName() {
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String name = "IMG_" + timeStamp + ".jpg";
		if (save.length() != 0)
			name = save + "_" + name;
		return new File(storageDir.getPath() + File.separator + name);
	}
	
	private Mat temporaryDisplay(Mat mat) {
		for (Rect r: regions) {
			Core.rectangle(mat, r.tl(), r.br(), Colors.GREEN);
		}
		if (selected != null)
			Core.rectangle(mat, selected.tl(), selected.br(), Colors.RED);
		return mat;
	}
    
	public boolean onTouch(View v, MotionEvent evt) {
		int action = evt.getActionMasked();
		int pointers = evt.getPointerCount();
		if (pointers != 1 || action != MotionEvent.ACTION_DOWN)
			return false;
		Point pt = convertPoint(evt.getX(), evt.getY());
		if (field == null) {
			if (selected == null) {
				for (Rect r: regions) {
					if (r.contains(pt))
						selected = r;
				}
				if (selected == null) {
					for (Rect r: regions)
						if (Math.abs(pt.x - (r.x + r.width / 2)) < r.width / 2 + ViewFieldActivity.APPROXIMATE_TOUCH &&
								Math.abs(pt.y - (r.y + r.height / 2)) < r.height/ 2 + ViewFieldActivity.APPROXIMATE_TOUCH)
							selected = r;
				}
			}
			else if (selected.contains(pt)){
				regions.remove(selected);
				selected = null;
			}
			else 
				selected = null;
		}
		return true;
	}

	public Point convertPoint(float tX, float tY) {
		float x = tX / screenWidth;
		float y = tY / screenHeight;
		return new Point(imWidth * x, imHeight * y);
	}
}
