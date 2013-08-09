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
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import edu.berkeley.cellscope.cscore.ScreenDimension;
import edu.berkeley.cellscope.cscore.cameraui.BluetoothConnectable;
import edu.berkeley.cellscope.cscore.cameraui.BluetoothConnector;
import edu.berkeley.cellscope.cscore.cameraui.CompoundTouchListener;
import edu.berkeley.cellscope.cscore.cameraui.PinchSelectActivity;
import edu.berkeley.cellscope.cscore.cameraui.TouchControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchExposureControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchPanControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchZoomControl;

public class OpenCVCameraActivity extends Activity implements CvCameraViewListener2, Autofocus.Autofocusable, TouchControl.BluetoothControllable, TouchZoomControl.Zoomable, TouchExposureControl.ManualExposure, BluetoothConnectable {
	
	BluetoothConnector btConnector;
	private static final String TAG = "OpenCV_Camera";
	
	protected OpenCVCameraView cameraView;
	protected TextView infoText;
	protected ImageButton takePicture, toggleRecord;
	protected Mat mRgba;
	private boolean firstFrame;
    protected MenuItem mMenuItemConnect, mMenuItemPinch;
	private Autofocus autofocus;
	protected CompoundTouchListener compoundTouch;
	protected TouchControl touchPan, touchZoom, touchExposure;
	
	private boolean maintainCamera; //Set to true for popup activities.
	
	long timeElapsed;
	long currentTime;
	protected boolean record = false;
	
	public static File mediaStorageDir = CameraActivity.mediaStorageDir;
	
	
    private static final int REQUEST_PINCH_CONTROL = 3;
	

    TextView bluetoothNameLabel;
    

	protected static final int COMPRESSION_QUALITY = 90; //0-100
    private static final long TIMELAPSE_INTERVAL = 5 * 1000; //milliseconds
    private static final int TOAST_DURATION = 750; //milliseconds
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
        toggleRecord = (ImageButton)findViewById(R.id.record_button);
	    
	    infoText = (TextView)findViewById(R.id.infotext);
	    
	    compoundTouch = new CompoundTouchListener();
	    touchPan = new TouchPanControl(this, this);
	    touchZoom = new TouchZoomControl(this, this);
	    touchExposure = new TouchExposureControl(this, this);
	    touchPan.setEnabled(true);
	    touchZoom.setEnabled(true);
	    compoundTouch.addTouchListener(touchPan);
	    compoundTouch.addTouchListener(touchZoom);
	    compoundTouch.addTouchListener(touchExposure);
	    
		bluetoothNameLabel = (TextView) findViewById(R.id.bluetoothtext);

		firstFrame = true;
		btConnector = new BluetoothConnector(this, this);
		autofocus = new Autofocus(new TouchSwipeControl(this, this));
		

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
        if (autofocus.isFocusing())
        	autofocus.focusFailed();
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
    	cameraView.disableAutoFocus();
		System.out.println("camera started");
	}

	public void onCameraViewStopped() {
		System.out.println("camera stopped");
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		if (firstFrame) {
			initialFrame();
			firstFrame = false;
		}
		if (record)
			record();
		queueAutofocusFrame(mRgba);
		return mRgba;
	}
	
	public void initialFrame() {
		
	}
	public void queueAutofocusFrame(Mat m) {
		if (autofocus.isFocusing()) {
			autofocus.queueFrame(m);
		}
	}
	
	public void record() {
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
	}
	

	public void toggleTimelapse(View v) {
		if (!record) {
			toggleRecord.setImageResource(R.drawable.stop);
			record = true;
			timeElapsed = 0;
			currentTime = -1;
		}
		else {
			toggleRecord.setImageResource(R.drawable.record);
			record = false;
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
		return ScreenDimension.getScreenDiagonal(this);
	}
	
	public int getMaxZoom() {
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
	
	public boolean controlReady() {
		return btConnector.enabled();
	}
	
	public BluetoothConnector getBluetooth() {
		return btConnector;
	}
	
	
	public void readMessage(Message msg) {
		byte[] buffer = (byte[])(msg.obj);
		//System.out.println("message read + " + buffer[0]);
		if (buffer.length > 0 && autofocus.isFocusing()) {
			notifyAutofocus((int)buffer[0]);
		}
	}
	
	public void notifyAutofocus(int message) {
		autofocus.motionComplete();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		System.out.println("creation");
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
       	else if (id == R.id.autofocus)
       		autofocus.focus();
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

    public void savePicture(File fileName, Bitmap picture) {
    	Log.i(TAG, "Saving a bitmap to file: " + fileName.getPath());
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
    
	protected void toast(final String message) {
		this.runOnUiThread( new Runnable() {
			public void run() {
				int duration = Toast.LENGTH_SHORT;
				final Toast toast = Toast.makeText(getApplicationContext(), message, duration);
				toast.show();
				Handler handler = new Handler();
		            handler.postDelayed(new Runnable() {
		               public void run() {
		                   toast.cancel(); 
		               }
		        }, TOAST_DURATION);
				
			}
			
		});
	}
}
