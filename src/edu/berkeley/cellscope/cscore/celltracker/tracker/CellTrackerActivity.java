package edu.berkeley.cellscope.cscore.celltracker.tracker;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	private DetectionParameters[] detection;
	private double minSize, maxSize;
	private int addedMargin;
	
	private boolean fieldReady;
	
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
		
		int channels = intent.getIntExtra(CellDetectActivity.CHANNEL_INFO, 0);
		detection = new DetectionParameters[channels];
		for (int i = 0; i < channels; i ++) {
			detection[i] = new DetectionParameters();
			detection[i].colorChannel = intent.getIntExtra(CellDetectActivity.DETECT_COLOR_INFO + CellDetectActivity.CHANNEL_INFO_TAG[i], 0);
			detection[i].colorThreshold = intent.getIntExtra(CellDetectActivity.DETECT_GRAYSCALE_INFO + CellDetectActivity.CHANNEL_INFO_TAG[i], 0);
			detection[i].noiseThreshold = intent.getIntExtra(CellDetectActivity.DETECT_NOISE_INFO + CellDetectActivity.CHANNEL_INFO_TAG[i], 0);
			detection[i].debrisThreshold = intent.getDoubleExtra(CellDetectActivity.DETECT_DEBRIS_INFO + CellDetectActivity.CHANNEL_INFO_TAG[i], 0);
			detection[i].backgroundThreshold = intent.getDoubleExtra(CellDetectActivity.DETECT_BACKGROUND_INFO + CellDetectActivity.CHANNEL_INFO_TAG[i], 60);
			detection[i].oblongThreshold = intent.getDoubleExtra(CellDetectActivity.DETECT_OBLONG_INFO + CellDetectActivity.CHANNEL_INFO_TAG[i], 0);
		}
		minSize = intent.getDoubleExtra(ViewFieldActivity.PARAM_SIZE_LOWER_INFO, 0);
		maxSize = intent.getDoubleExtra(ViewFieldActivity.PARAM_SIZE_UPPER_INFO, 0);
		addedMargin = intent.getIntExtra(ViewFieldActivity.PARAM_MARGIN_INFO, 0);
		
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

	}
	
	@Override
	protected void createAddons(int width, int height) {
		super.createAddons(width, height);
		touchZoom.setEnabled(false);
		touchPan.setEnabled(false);
		toggleRecord.setVisibility(View.INVISIBLE);
		compoundTouch.addTouchListener(this);
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
		resetField();
	}
	
	public void resetField() {
		synchronized(this) {
			if (record)
		    	toggleTimelapse(null);
			if (field != null) {
				field.resetData();
				fieldReady = false;
				realtimeProcessors.remove(field);
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
		field.setInterval(interval);
		field.initiateUpdateThread();
		fieldReady = true;
		selected = null;
		realtimeProcessors.add(field);
	}
	
	@Override
	protected void drawImageProcessors(Mat mat) {
		super.drawImageProcessors(mat);
		if (!fieldReady)
			temporaryDisplay(mRgba);
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
			return true;
		}
		if (field == null || field.isRunning())
			return true;
		Point pt = convertPoint(evt.getX(), evt.getY());
		if (pointers == 1 && action == MotionEvent.ACTION_DOWN && selected == null) {
			field.selectObject(pt);
		}
		return true;
	}

	public Point convertPoint(float tX, float tY) {
		float x = tX / screenWidth;
		float y = tY / screenHeight;
		return new Point(imWidth * x, imHeight * y);
	}
	
	private void runDetection(Mat mat) {
		CellDetection.MultiChannelContourData data = null;
		for (int i = 0; i < detection.length; i ++ ) {
			DetectionParameters params = detection[i];
			CellDetection.ContourData contour = CellDetection.filterImage(mat.clone(), params.colorChannel, params.colorThreshold);
			//CellDetection.removeNoise(data, noiseThresholdd);
			//CellDetection.removeBackground(data, backgroundThreshold);
			//CellDetection.removeDebris(data, debrisThreshold);
			CellDetection.removeOblong(contour, params.oblongThreshold);
			if (data == null)
				data = contour.generateMultiChannelData();
			data.add(contour);
		}
		
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
	

	@SuppressWarnings("unused")
	private static class DetectionParameters {
		int colorChannel, colorThreshold, noiseThreshold;
		double debrisThreshold, backgroundThreshold, oblongThreshold;
	}
}
