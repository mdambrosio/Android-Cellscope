package edu.berkeley.cellscope.cscore.celltracker;

import java.io.File;
import java.io.FileOutputStream;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

public class OpenCVCameraView extends JavaCameraView {
	private static final String TAG = "OpenCvCameraView";
	private static final int COMPRESSION_QUALITY = 90; //0-100
	
	private OpenCVCameraActivity activity;

	public OpenCVCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setActivity(OpenCVCameraActivity activity) {
		this.activity = activity;
	}

	public void takePicture(final File fileName) {
        Log.i(TAG, "Taking picture");
        PictureCallback callback = new PictureCallback() {

            public void onPictureTaken(byte[] data, Camera camera) {
                Log.i(TAG, "Saving a bitmap to file: " + fileName.getPath());
                Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);
                try {
                    FileOutputStream out = new FileOutputStream(fileName);
                    picture.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out);
                    picture.recycle();
                    out.close();
                    toast("Picture saved as " + fileName.getName());
   //             	FileOutputStream fos = new FileOutputStream(fileName);
   // 	            fos.write(data);
   // 	            fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //mCamera.stopPreview();
                mCamera.startPreview();
            }
        };
        Camera.Parameters params = mCamera.getParameters();
        Camera.Size size = params.getPreviewSize();
        params.setPictureSize(size.width, size.height);
        mCamera.setParameters(params);
        mCamera.takePicture(null, null, callback);
    }
    
	public void zoom(int step) {
		Camera.Parameters parameters = mCamera.getParameters();
		if (!parameters.isZoomSupported())
			return;
		int zoom = parameters.getZoom() + step;
		if (zoom > parameters.getMaxZoom())
			zoom = parameters.getMaxZoom();
		else if (zoom < 0)
			zoom = 0;
		parameters.setZoom(zoom);
		String str= parameters.getZoomRatios().get(zoom) + "%";
		activity.zoomText.setText(str);
		mCamera.setParameters(parameters);
	}
	
	public int getMaxZoom() {
		Camera.Parameters parameters = mCamera.getParameters();
		if (!parameters.isZoomSupported())
			return 0;
		return parameters.getMaxZoom();
	}
	
	private void toast(String message) {
		Context context = activity.getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}
}
