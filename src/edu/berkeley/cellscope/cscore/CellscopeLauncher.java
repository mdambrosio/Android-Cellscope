package edu.berkeley.cellscope.cscore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;


/*
 * Lots of code from Android's camera tutorial:
 * http://developer.android.com/training/camera/cameradirect.html
 * 
 * It's implemented differently in the tutorial in a way
 * that makes the code much neater and easier to use--
 * Basically, it takes most of this code and puts it
 * into its own class, so all you have to do is create
 * and instance of that class and everything will start up
 * on its own.
 * 
 * Unfortunately, the preview did not display correctly.
 * Sticking the code in here makes it work, for some reason.
 */
public class CellscopeLauncher extends Activity {
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
