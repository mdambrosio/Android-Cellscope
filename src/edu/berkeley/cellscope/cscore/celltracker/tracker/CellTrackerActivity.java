package edu.berkeley.cellscope.cscore.celltracker.tracker;

import java.util.ArrayList;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import edu.berkeley.cellscope.cscore.CameraActivity;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.ScreenDimension;
import edu.berkeley.cellscope.cscore.celltracker.Colors;
import edu.berkeley.cellscope.cscore.celltracker.OpenCVCameraActivity;
import edu.berkeley.cellscope.cscore.celltracker.TrackedCallback;
import edu.berkeley.cellscope.cscore.celltracker.TrackedField;

public class CellTrackerActivity extends OpenCVCameraActivity implements TrackedCallback {
	
	private TrackedField field;
	private int screenWidth, screenHeight, imWidth, imHeight;
	private Point fovCenter;
	private int fovRadius;
	private int zoom, exposure;
	private MenuItem mMenuItemApply;
	private List<Rect> regions;
	static final int TEST_WIDTH = 50;
	static final int TEST_HEIGHT = 50;
	
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
		toggleTimelapse.setVisibility(View.INVISIBLE);
		
	}

	public void onCameraViewStarted(int width, int height) {
		super.onCameraViewStarted(width, height);
		cameraView.zoom(zoom);
		cameraView.adjustExposure(exposure);
	}
	
	//the field must be instantiated using a valid frame, but openCV yields a blank screen on the first frame.
	public void createField() {
		field = new TrackedField(mRgba, fovCenter, fovRadius);
		for (Rect r: regions)
			field.addObject(r);
		field.initiateUpdateThread();
	}
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		super.onCameraFrame(inputFrame);
		if (field == null)
			return temporaryDisplay(mRgba);
		field.queueFrame(mRgba);
		return field.display();
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
	public void timelapse() {
		return;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (super.onOptionsItemSelected(item))
    		return true;
    	int id = item.getItemId();
    	if (id == R.id.celltracker_apply) {
    		createField();
    		mMenuItemApply.setVisible(false);
    		toggleTimelapse.setVisibility(View.VISIBLE);
    	}
    	return true;
    }
    
    @Override
    public void toggleTimelapse(View v) {
    	if (field == null) {
    		createField();
    		return;
    	}
    	super.toggleTimelapse(v);
    	if (timelapseOn)
    		field.startTracking();
    	else
    		field.stopTracking();
    		
    }

	public void updateComplete(Mat mat) {
		Bitmap bmp = Bitmap.createBitmap(imWidth, imHeight, Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(mat, bmp);
		savePicture(CameraActivity.getOutputMediaFile(CameraActivity.MEDIA_TYPE_IMAGE), bmp);
		bmp.recycle();
	}
	
	private Mat temporaryDisplay(Mat mat) {
		for (Rect r: regions) {
			Core.rectangle(mat, r.tl(), r.br(), Colors.GREEN);
		}
		return mat;
	}
    
}
