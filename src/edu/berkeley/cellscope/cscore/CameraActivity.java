package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.os.Bundle;

public class CameraActivity extends Activity {
	/*
	 * The surface that the preview will show up on--
	 * Kind of like JPanels in swing
	 */
	PhotoSurface mSurfaceView; 
	
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Launching Cellscope...");
        mSurfaceView = new PhotoSurface(this);
    	setContentView(mSurfaceView);
    }
    
    /*
     * This is automatically called when the application is opened
     * or resumed.
     */
    public void onResume() {
    	super.onResume();
    	System.out.println("Surface resuming...");
    	if (mSurfaceView.safeCameraOpen())
    		mSurfaceView.startCameraPreview();
    }
    
    public void onPause() {
    	super.onPause();
    	mSurfaceView.stopCameraPreview();
    	mSurfaceView.mCamera.release();
    }
}
