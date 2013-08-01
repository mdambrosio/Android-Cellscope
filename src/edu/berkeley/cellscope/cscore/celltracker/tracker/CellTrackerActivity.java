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

import android.annotation.SuppressLint;
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
import edu.berkeley.cellscope.cscore.celltracker.MathUtils;
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
	private List<Rect> rects;
	private Rect selected;
	
	private int imgCounter;
	private String save;
	private boolean timelapse;
	private int interval;
	private File storageDir;
	private File outputFile;
	private String fileHeader;
	
	//Cell detection parameters
	private int colorChannel, colorThreshold, noiseThreshold;
	private double debrisThreshold, backgroundThreshold, oblongThreshold;
	private double minSize, maxSize;
	private int addedMargin;
	
	private boolean fieldReady;
	private double touchX, touchY;
	
	
	private static final int MINIMUM_SIZE = 10;
	private static final int firstTouchEvent = -1;
	private static final double TOUCH_SENSITIVITY = 0.1;
	
	private static final File defaultPictureDir = CameraActivity.mediaStorageDir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		screenWidth = ScreenDimension.getScreenWidth(this);
		screenHeight = ScreenDimension.getScreenHeight(this);
		
		Intent intent = getIntent(); 
		
		rects = new ArrayList<Rect>();

		zoom = intent.getIntExtra(InitialCameraActivity.CAM_ZOOM_INFO, 0);
		exposure = intent.getIntExtra(InitialCameraActivity.CAM_EXPOSURE_INFO, 0);
		
		imWidth = intent.getIntExtra(ViewFieldActivity.IMG_WIDTH_INFO, screenWidth);
		imHeight = intent.getIntExtra(ViewFieldActivity.IMG_HEIGHT_INFO, screenHeight);
		int fovX = intent.getIntExtra(ViewFieldActivity.FOV_X_INFO, imWidth / 2);
		int fovY = intent.getIntExtra(ViewFieldActivity.FOV_Y_INFO, imHeight / 2);
		fovCenter = new Point(fovX, fovY);
		fovRadius = intent.getIntExtra(ViewFieldActivity.FOV_RADIUS_INFO, imHeight / 2);
		
		colorChannel = intent.getIntExtra(CellDetectActivity.DETECT_COLOR_INFO, 0);
		colorThreshold = intent.getIntExtra(CellDetectActivity.DETECT_GRAYSCALE_INFO, 0);
		noiseThreshold = intent.getIntExtra(CellDetectActivity.DETECT_NOISE_INFO, 0);
		debrisThreshold = intent.getDoubleExtra(CellDetectActivity.DETECT_DEBRIS_INFO, 0);
		backgroundThreshold = intent.getDoubleExtra(CellDetectActivity.DETECT_BACKGROUND_INFO, 60);
		oblongThreshold = intent.getDoubleExtra(CellDetectActivity.DETECT_OBLONG_INFO, 0);
		minSize = intent.getDoubleExtra(ViewFieldActivity.PARAM_SIZE_LOWER_INFO, 0);
		maxSize = intent.getDoubleExtra(ViewFieldActivity.PARAM_SIZE_UPPER_INFO, 0);
		addedMargin = intent.getIntExtra(ViewFieldActivity.PARAM_MARGIN_INFO, 0);
		
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
			fileHeader =  save;
		}
		if (!storageDir.exists())
			storageDir.mkdirs();

		touchX = touchY = firstTouchEvent;
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
				field.resetData();
				fieldReady = false;
			}
			if (mMenuItemApply != null)
				mMenuItemApply.setVisible(true);
			selected = null;
			toggleRecord.setVisibility(View.INVISIBLE);
		}
	}
	
	//the field must be instantiated using a valid frame, but openCV yields a blank screen on the first frame.
	public void detectCells() {
		if (field == null)
			field = new TrackedField(mRgba, fovCenter, fovRadius);
		else
			field.resetData();
		field.setOutputFile(outputFile, fileHeader);
		runDetection(mRgba);
		for (Rect r: rects)
			field.addObject(r);
		field.setCallback(this);
		field.initiateUpdateThread(interval);
		fieldReady = true;
		selected = null;
	}
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		super.onCameraFrame(inputFrame);
		synchronized(this) {
			if (field == null || !fieldReady)
				return temporaryDisplay(mRgba);
			field.queueFrame(mRgba);
			Mat display = field.display();
			if (selected != null) {
				if (MathUtils.circleContainsRect(selected, fovCenter, fovRadius))
					Core.rectangle(display, selected.tl(), selected.br(), Colors.CYAN, 2);
				else
					Core.rectangle(display, selected.tl(), selected.br(), Colors.RED, 2);
			}
			return display;
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
    	if (id == R.id.celltracker_detect) {
    		detectCells();
    		toggleRecord.setVisibility(View.VISIBLE);
    	}
    	return true;
    }
    
    @Override
    public void toggleTimelapse(View v) {
    	synchronized(this) {
	    	if (field == null) {
	    		detectCells();
	    		return;
	    	}
	    	if (selected != null) {
	    		selected = null;
	    		return;
	    	}
	    	super.toggleTimelapse(v);
	    	if (record)
    			field.startTracking();
	    	else
	    		field.stopTracking();
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
	
	@SuppressLint("SimpleDateFormat")
	public File getPictureName() {
		
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String name = "IMG_" + timeStamp + "_" + imgCounter+  ".jpg";
	    imgCounter ++;
		if (save.length() != 0)
			name = save + "_" + name;
		return new File(storageDir.getPath() + File.separator + name);
	}
	
	private Mat temporaryDisplay(Mat mat) {
		Core.circle(mat, fovCenter, fovRadius, Colors.WHITE);
		return mat;
	}
    
	public boolean onTouch(View v, MotionEvent evt) {
		int action = evt.getActionMasked();
		int pointers = evt.getPointerCount();
		if (action == MotionEvent.ACTION_UP) {
			touchX = touchY = firstTouchEvent;
			return true;
		}
		if (field == null || field.isTracking())
			return true;
		Point pt = convertPoint(evt.getX(), evt.getY());
		synchronized(this) {
		//If no object is currently selected, select whatever is touched
		//If nothing is touched, create an object
		if (pointers == 1 && action == MotionEvent.ACTION_DOWN && selected == null) {
			selected = field.selectObject(pt);
			if (selected == null) {
				int aveSize = (int)(Math.sqrt((minSize + maxSize) / 2)) + addedMargin;
				if (aveSize < MINIMUM_SIZE) aveSize = MINIMUM_SIZE;
				selected = MathUtils.createCenteredRect(pt, aveSize, aveSize);
			}
			return true;
		}
		
		//If the currently selected object is touched, add to field
		if (pointers == 1 && action == MotionEvent.ACTION_DOWN && selected.contains(pt)) {
			field.addObject(selected);
			selected = null;
			return true;
		}
		
		//Translate the rectangle 
		if (pointers == 1) {
			if (touchX != firstTouchEvent && touchY != firstTouchEvent) {
				double x = pt.x - touchX;
				double y = pt.y - touchY;
				if (Math.abs(x) > Math.abs(y))
					selected.x += x * TOUCH_SENSITIVITY;
				else
					selected.y += y * TOUCH_SENSITIVITY;
				MathUtils.cropRectToRegion(selected, imWidth, imHeight);
			}
			touchX = pt.x;
			touchY = pt.y;
			return true;
		}
		
		//Expand the rectangle
		if (pointers == 2) {
			double xDist = Math.abs(evt.getX(0) - evt.getX(1));
			double yDist = Math.abs(evt.getY(0) - evt.getY(1));
			if (action == MotionEvent.ACTION_MOVE) {
				if (touchX != firstTouchEvent && touchY != firstTouchEvent) { //Prevents jumping
					System.out.println(xDist + " " + yDist);
					if (Math.abs(xDist) > Math.abs(yDist))
						MathUtils.resizeRect(selected, (int)( (xDist - touchX) * TOUCH_SENSITIVITY) / 2 * 2, 0);
					else
						MathUtils.resizeRect(selected, 0, (int)( (yDist - touchY) * TOUCH_SENSITIVITY) / 2 * 2);
					//MathUtils.cropRectToRegion(selected, imWidth, imHeight);
				}
				touchX = xDist;
				touchY = yDist;
			}
			else {
				touchX = touchY = firstTouchEvent;
			}
		}
		return true;
		}
	}

	public Point convertPoint(float tX, float tY) {
		float x = tX / screenWidth;
		float y = tY / screenHeight;
		return new Point(imWidth * x, imHeight * y);
	}
	
	private void runDetection(Mat mat) {
		CellDetection.ContourData data = CellDetection.filterImage(mat, colorChannel, colorThreshold);
		//CellDetection.removeNoise(data, noiseThreshold);
		//CellDetection.removeBackground(data, backgroundThreshold);
		//CellDetection.removeDebris(data, debrisThreshold);
		CellDetection.removeOblong(data, oblongThreshold);
		
		rects.clear();
		data.getRects(rects);
		int size = rects.size();
		for (int i = 0; i < size; i ++) {
			double area = rects.get(i).area();
			if (area > maxSize || area < minSize) {
				rects.remove(i);
				i --;
				size --;
			}
			else {
				Rect r = rects.get(i);
				MathUtils.resizeRect(r, addedMargin, addedMargin);
				MathUtils.cropRectToRegion(r, imWidth, imHeight);
			}
		}
		
	}
}