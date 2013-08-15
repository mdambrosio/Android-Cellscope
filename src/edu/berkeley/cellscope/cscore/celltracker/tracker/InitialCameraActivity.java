package edu.berkeley.cellscope.cscore.celltracker.tracker;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import edu.berkeley.cellscope.cscore.celltracker.OpenCVCameraActivity;

public class InitialCameraActivity extends OpenCVCameraActivity {
	private static final String TEMP_FILE = "tmp.png";
	private static final int COMPRESSION_QUALITY = 100;
	static final String TEMP_PATH_INFO = "temporary";
	static final String CAM_ZOOM_INFO = "zoom";
	static final String CAM_EXPOSURE_INFO = "exposure";
	private boolean available;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		available = true;
		toggleRecord.setVisibility(View.GONE);
	}
	
	@Override
	protected void createAddons(int width, int height) {
		super.createAddons(width, height);
		touchExposure.setEnabled(true);
		touchZoom.setEnabled(false);
	}
	
	@Override
	public void takePhoto() {
		if (!available)
			return;
		available = false;
		super.takePhoto();
	}

	@Override
    public void savePicture(File fileName, Bitmap picture) {
        try {
            FileOutputStream out = openFileOutput(TEMP_FILE, Context.MODE_PRIVATE);
            picture.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, out);
            picture.recycle();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(this, CellDetectActivity.class);
        intent.putExtra(TEMP_PATH_INFO, TEMP_FILE);
        intent.putExtra(CAM_ZOOM_INFO, cameraView.getCurrentZoom());
        intent.putExtra(CAM_EXPOSURE_INFO, cameraView.getCurrentExposure());
        intent.putExtras(getIntent());
        startActivity(intent);
        finish();
    }
	
	
}
