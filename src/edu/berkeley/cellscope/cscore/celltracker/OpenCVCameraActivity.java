package edu.berkeley.cellscope.cscore.celltracker;

import java.io.File;
import java.io.FileOutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import edu.berkeley.cellscope.cscore.CameraActivity;
import edu.berkeley.cellscope.cscore.R;
import edu.berkeley.cellscope.cscore.cameraui.BluetoothConnectable;
import edu.berkeley.cellscope.cscore.cameraui.BluetoothConnector;
import edu.berkeley.cellscope.cscore.cameraui.CompoundTouchListener;
import edu.berkeley.cellscope.cscore.cameraui.ManualExposure;
import edu.berkeley.cellscope.cscore.cameraui.PannableStage;
import edu.berkeley.cellscope.cscore.cameraui.PinchSelectActivity;
import edu.berkeley.cellscope.cscore.cameraui.TouchControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchExposureControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchPanControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchZoomControl;
import edu.berkeley.cellscope.cscore.cameraui.ZoomablePreview;

public class OpenCVCameraActivity extends Activity implements CvCameraViewListener2, PannableStage, ZoomablePreview, ManualExposure, BluetoothConnectable {
	
	BluetoothConnector btConnector;
	private static final String TAG = "OpenCV_Camera";
	
	protected OpenCVCameraView cameraView;
	protected TextView infoText;
	protected ImageButton takePicture, toggleTimelapse;
	protected Mat mRgba;
	private boolean firstFrame;
    protected MenuItem mMenuItemConnect, mMenuItemPinch;
	
	protected CompoundTouchListener compoundTouch;
	private TouchControl touchPan, touchZoom, touchExposure;
	
	private boolean maintainCamera; //Set to true for popup activities.
	
	long timeElapsed;
	long currentTime;
	boolean timelapseOn = false;
	
	public static File mediaStorageDir = CameraActivity.mediaStorageDir;
	
	
    private static final int REQUEST_PINCH_CONTROL = 3;
	

    TextView bluetoothNameLabel;
    

	protected static final int COMPRESSION_QUALITY = 90; //0-100
    private static final long TIMELAPSE_INTERVAL = 5 * 1000; //milliseconds
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    synchronized(OpenCVCameraActivity.this) {
                    	cameraView.enableView();
                    }
                    //cameraView.disableAutoFocus();
                    //cameraView.setOnTouchListener(OpenCVCameraActivity.this);
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
		
	    
        takePicture = (ImageButton)findViewById(R.id.takePhotoButton);
        //toggleTimelapse = (ImageButton)findViewById(R.id.opencv_timelapse);
	    
	    infoText = (TextView)findViewById(R.id.infotext);
	    
	    compoundTouch = new CompoundTouchListener();
	    touchPan = new TouchPanControl(this, this);
	    touchZoom = new TouchZoomControl(this);
	    touchExposure = new TouchExposureControl(this);
	    touchPan.setEnabled(true);
	    touchZoom.setEnabled(true);
	    compoundTouch.addTouchListener(touchPan);
	    compoundTouch.addTouchListener(touchZoom);
	    compoundTouch.addTouchListener(touchExposure);
	    
		bluetoothNameLabel = (TextView) findViewById(R.id.bluetoothtext);

		firstFrame = true;
		btConnector = new BluetoothConnector(this, this);
		

		synchronized(this) {
	        cameraView = (OpenCVCameraView) findViewById(R.id.opencv_camera_view);
	        cameraView.setVisibility(SurfaceView.VISIBLE);
	        cameraView.setCvCameraViewListener(this);
	        cameraView.setActivity(this);
		    cameraView.setOnTouchListener(compoundTouch);
		}
    }
	
    @Override
    public void onStart() {
    	super.onStart();
    	btConnector.onStart();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (cameraView != null && !maintainCamera)
           cameraView.disableView();
        else
        	maintainCamera = false;
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
        if (cameraView != null)
            cameraView.disableView();
        btConnector.stopBluetooth();
    }
    
    public void bluetoothUnavailable() {
		mMenuItemConnect.setEnabled(false);
    }

    public void bluetoothConnected() {
    	if (mMenuItemConnect != null) {
    		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    		mMenuItemConnect.setTitle(R.string.disconnect);
    	}
    	bluetoothNameLabel.setText(R.string.title_connected_to);
    	bluetoothNameLabel.append(btConnector.getDeviceName());
		//stepperThread.schedule(stepper, 0, TimeUnit.MILLISECONDS);
    }
    
    public void bluetoothDisconnected() {
    	if (mMenuItemConnect != null) {
    		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
    		mMenuItemConnect.setTitle(R.string.connect);
    	}
    }
    
    public void updateStatusMessage(int id) {
     	bluetoothNameLabel.setText(id);
    }

	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		if (firstFrame) {
			initialFrame();
			firstFrame = false;
		}
		if (timelapseOn)
			timelapse();
		return mRgba;
	}
	
	public void initialFrame() {
		
	}
	
	public boolean timelapse() {
		if (currentTime != -1) {
			long newTime = System.currentTimeMillis();
			timeElapsed += newTime - currentTime;
			if (timeElapsed > TIMELAPSE_INTERVAL) {
				takePhoto();
				timeElapsed -= TIMELAPSE_INTERVAL;
			}
			currentTime = newTime;
		}
		else
			currentTime = System.currentTimeMillis();
		return false;
	}
	
	public void toggleTimelapse(View v) {
		if (!timelapseOn) {
			toggleTimelapse.setImageResource(R.drawable.stop);
			timelapseOn = true;
			timeElapsed = 0;
			currentTime = -1;
		}
		else {
			toggleTimelapse.setImageResource(R.drawable.record);
			timelapseOn = false;
		}
	}
	
	public void takePhoto() {
		File file = CameraActivity.getOutputMediaFile(CameraActivity.MEDIA_TYPE_IMAGE);
		cameraView.takePicture(file);
	}
	
	public void takePhoto(View view) {
		takePhoto();
	}
	
	public void zoomIn(View view) {
		zoom(10);
	}
	public void zoomOut(View view) {
		zoom(-10);
	}
	
	public double getDiagonal() {
		return CameraActivity.getScreenDiagonal(this);
	}
	
	public double getMaxZoom() {
		return cameraView.getMaxZoom();
	}
	
	public void zoom(int amount) {
		String str = cameraView.zoom(amount);
		infoText.setText(getString(R.string.zoom_label) + str);
	}
	
	public int getMaxExposure() {
		return cameraView.getMaxExposure();
	}
	
	public int getMinExposure() {
		return cameraView.getMinExposure();
	}
	
	public void adjustExposure(int amount) {
		String str = cameraView.adjustExposure(amount);
		infoText.setText(getString(R.string.exposure_label) + str);
	}
	
	public boolean panAvailable() {
		return btConnector.enabled();
	}
	
	public void panStage(int newState) {
		byte[] buffer = new byte[1];
    	buffer[0] = (byte)newState;
    	btConnector.write(buffer);
    	/*
		if (btConnector.enabled()) {
			/*
			if (panState == zUpMotor || panState == zDownMotor || panState == stopMotor) {
				byte[] buffer = new byte[]{(byte)panState};
				stepper.setFirst(buffer);
				stepper.setSecond(buffer);
				stepper.delayOne = stepper.delayTwo = 100;
			}
			else {
				byte[] stop = new byte[]{(byte)stopMotor};
				byte[] command = new byte[]{(byte)panState};
				stepper.setFirst(stop);
				stepper.setSecond(command);
				stepper.delayOne = 200;
				stepper.delayTwo = 5;
			}
			
		}*/
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bluetooth, menu);
        mMenuItemConnect = menu.getItem(0);
        inflater.inflate(R.menu.menu_controls, menu);
        mMenuItemPinch = menu.getItem(1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int id = item.getItemId();
       	if (id == R.id.connect) {
       		maintainCamera = true;
        	btConnector.connectBluetooth();
            return true;
        }
       	else if (id == R.id.menu_pinch) {
       		maintainCamera = true;
       		Intent intent = new Intent(this, PinchSelectActivity.class);
    		startActivityForResult(intent, REQUEST_PINCH_CONTROL);
       	}
        return false;
    }
    
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONNECT_DEVICE) {
        	btConnector.queryResultConnect(resultCode, data);
        }
        else if (requestCode == REQUEST_ENABLE_BT) {
        	btConnector.queryResultEnabled(resultCode, data);
        }
        
        else if (requestCode == REQUEST_PINCH_CONTROL) {
        	if (resultCode == PinchSelectActivity.SELECT_ZOOM) {
        		touchZoom.setEnabled(true);
        		touchExposure.setEnabled(false);
        		infoText.setText("");
        	}
        	else if (resultCode == PinchSelectActivity.SELECT_EXPOSURE) {
        		touchZoom.setEnabled(false);
        		touchExposure.setEnabled(true);
        	}
        }
    }

    public void savePicture(File fileName, byte[] data) {
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
    }
    
	protected void toast(String message) {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(getApplicationContext(), message, duration);
		toast.show();
	}
}
