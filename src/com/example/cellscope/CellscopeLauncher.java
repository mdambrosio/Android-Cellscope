package com.example.cellscope;

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
import android.view.SurfaceHolder;


/*
 * Lots of code from Android's camera tutorial:
 * Added a comment by Matthew Bakalar
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
	
	/*
	 * Interface that lets application access and modify
	 * the surface.
	 */
    SurfaceHolder mHolder;
    
    Camera mCamera;
    
    /*
     * surfaceChanged() is automatically called whenever the screen changes,
     * including when the app is started.
     * Currently, the app is set so that it starts in portrait mode and cannot
     * switch to landscape, so this is only called once at the start.
     * 
     * This method sets the camera to display the preview on mSurfaceView,
     * sets the preview to the appropriate size,
     * and starts the preview.
     */
    SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {}
		public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
			System.out.println("Surface changed...");
			
			try {
				mCamera.setPreviewDisplay(mHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		    Camera.Parameters parameters = mCamera.getParameters();
		    Camera.Size mPreviewSize = getPreviewSize(parameters, w, h);
		    parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		    mCamera.setParameters(parameters);
		    mCamera.startPreview();
		}
		
		public void surfaceDestroyed(SurfaceHolder holder) {}
	};
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	PictureCallback mPicture = new PictureCallback() {

	    public void onPictureTaken(byte[] data, Camera camera) {
	        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
	        System.out.println(data);
	        if (pictureFile == null){
	            System.out.println("Error creating media file, check storage permissions: ");
	            return;
	        }

	        try {
	            FileOutputStream fos = new FileOutputStream(pictureFile);
	            fos.write(data);
	            fos.close();
	        } catch (FileNotFoundException e) {
	           System.out.println("File not found: " + e.getMessage());
	        } catch (IOException e) {
	        	 System.out.println("Error accessing file: " + e.getMessage());
	        }
	    }
	};
	
	ShutterCallback mShutter = new ShutterCallback() {
		public void onShutter() {
			
		}
	};
	
	/*
	 * This is automatically called when the program starts,
	 * basically the main() method for Android.
	 * 
	 * It sets the display to show mSurfaceView
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Launching Cellscope...");
        mSurfaceView = new PhotoSurface(this);
    	setContentView(mSurfaceView);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(mCallback);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
    }
    
    /*
     * This is automatically called when the application is opened
     * or resumed.
     */
    public void onResume() {
    	super.onResume();
    	System.out.println("Surface resuming...");
    	if (safeCameraOpen())
    		mCamera.startPreview();
    }
    
    private boolean safeCameraOpen() {
        boolean qOpened = false;
        System.out.println("Opening camera...");
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(); /* This is the important thing!
            							It makes an instance of a Camera object that
            							lets the application do stuff with the hardware.
            							*/
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }
        return qOpened;    
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
    
    /*
     * Returns the best size that the camera preview should be, based
     * on the size of the screen.
     */
	static Camera.Size getPreviewSize(Camera.Parameters parameters, int width, int height) {
		Camera.Size result = null;
		for (Camera.Size current: parameters.getSupportedPreviewSizes()) {
			if (current.width < width && current.height < height) {
				if (result == null)
					result = current;
				else if (result.width * result.height < current.width * current.height)
					result = current;
			}
		}
		return result;
	}
	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "MyCameraApp");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
}
