package edu.berkeley.cellscope.cscore.celltracker;

import java.io.File;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import edu.berkeley.cellscope.cscore.CameraActivity;
import edu.berkeley.cellscope.cscore.R;

public class OpenCVCameraActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener {

	private static final String TAG = "OpenCV_Camera";
	
	private OpenCVCameraView mOpenCvCameraView;
	TextView zoomText;
	
	double pinchDist;
	int lastZoom;
	double screenDiagonal;
	double maxZoom;
	
	public static File mediaStorageDir = CameraActivity.mediaStorageDir;
	private static final int firstTouchEvent = -1;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    //mOpenCvCameraView.setOnTouchListener(OpenCVCameraActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public OpenCVCameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_opencv_camera);
        mOpenCvCameraView = (OpenCVCameraView) findViewById(R.id.opencv_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setActivity(this);
	    
        zoomText = (TextView)findViewById(R.id.opencv_zoomtext);
	    zoomText.setText("100%");
	    
	    screenDiagonal = CameraActivity.getScreenDiagonal(this);
	    maxZoom = -1;
	    
	    mOpenCvCameraView.setOnTouchListener(this);
	}
	

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		return inputFrame.rgba();
	}

	public void takePhoto(View view) {
		File file = CameraActivity.getOutputMediaFile(CameraActivity.MEDIA_TYPE_IMAGE);
		mOpenCvCameraView.takePicture(file);
	}
	
	public void zoomIn(View view) {
		mOpenCvCameraView.zoom(10);
	}
	public void zoomOut(View view) {
		mOpenCvCameraView.zoom(-10);
	}

	public boolean onTouch(View v, MotionEvent event) {
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();
		//Pinch zoom
		if (pointers == 2){
			if (maxZoom == -1)
				maxZoom = mOpenCvCameraView.getMaxZoom();
			double newDist = Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
			if (action == MotionEvent.ACTION_MOVE) {
				if (pinchDist != firstTouchEvent) { //Prevents jumping
					int newZoom = (int)((newDist-pinchDist) / screenDiagonal * maxZoom * 2);
					System.out.println(lastZoom + " " + newZoom);
					mOpenCvCameraView.zoom(newZoom - lastZoom);
					lastZoom = newZoom;
				}
				else {
					pinchDist = newDist;
					lastZoom = 0;
				}
			}
			else {
				pinchDist = firstTouchEvent;
			}
		}
		return true;
	}
}
