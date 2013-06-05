package edu.berkeley.cellscope.cscore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity runs the camera, allowing either photos or video to be taken.
 */

public class CameraActivity extends Activity {
	//PhotoSurface mSurfaceView; 
	SurfaceView mSurfaceView;
	SurfaceHolder mHolder;
	Camera mCamera;
	MediaRecorder recorder;
	boolean previewRunning;
	boolean cameraBusy;
	Activity activity;
	double touchX, touchY;
	boolean videoState; //false for camera, true for video
	boolean recording; //true when the video camera is running
	boolean forceUpdateCamera;
	ImageButton takePhoto, switchMode, zoomIn, zoomOut;
	TextView zoomText;
	
	private static final double firstTouchEvent = -1;
	private static final double PAN_THRESHOLD = 25;
	
	private static final String TAG = "Camera";
	private static final int COMPRESSION_QUALITY = 90;
	
	//Bluetooth stuff

    private MenuItem mMenuItemConnect;
    private static BluetoothSerialService mSerialService = null;
    private boolean mEnablingBT;
    
    private int panState;
    private static final int xRightMotor = BluetoothActivity.xRightMotor;
    private static final int xLeftMotor = BluetoothActivity.xLeftMotor;
    private static final int yBackMotor = BluetoothActivity.yBackMotor;
    private static final int yForwardMotor = BluetoothActivity.yForwardMotor;
    private static final int zUpMotor = BluetoothActivity.zUpMotor;
    private static final int zDownMotor = BluetoothActivity.zDownMotor;
    private static final int stopMotor = 0;
	
	// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
	

    // Name of the connected device
    private String mConnectedDeviceName = null;
    
	private BluetoothAdapter mBluetoothAdapter = null;

    private boolean mLocalEcho = false;
    private boolean bluetoothEnabled = false;
    private boolean proceedWithConnection = true;
    TextView bluetoothNameLabel;
    
    // Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;	

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";


    /**
     * Set to true to add debugging code and logging.
     */
    public static final boolean DEBUG = true;

    /**
     * Set to true to log each character received from the remote process to the
     * android log, which makes it easier to debug some kinds of problems with
     * emulating escape sequences and control codes.
     */
    public static final boolean LOG_CHARACTERS_FLAG = DEBUG && true;

    /**
     * Set to true to log unknown escape sequences.
     */
    public static final boolean LOG_UNKNOWN_ESCAPE_SEQUENCES = DEBUG && true;

    /**
     * The tag we use when logging, so that our messages can be distinguished
     * from other messages in the log. Public because it's used by several
     * classes.
     */
	public static final String LOG_TAG = "CellScope";
	

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	// Create the storage directories
	 public static File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
	 public static File videoStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyVideoApp");
	 static {
		 if (!mediaStorageDir.exists())
			 mediaStorageDir.mkdirs();
		 if (!videoStorageDir.exists())
			 videoStorageDir.mkdirs();
	 }
	 
	// The Handler that gets information back from the BluetoothService
	    private final Handler mHandlerBT = new Handler() {
	    	
	        @Override
	        public void handleMessage(Message msg) {        	
	            switch (msg.what) {
	            case MESSAGE_STATE_CHANGE:
	                if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
	                case BluetoothSerialService.STATE_CONNECTED:
	                    if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE/STATE_CONNECTED");
	                	if (mMenuItemConnect != null) {
	                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
	                		mMenuItemConnect.setTitle(R.string.disconnect);
	                	}
	                	
	                	//Replace my input variable to the bluetooth device below
	//------           	mInputManager.showSoftInput(mEmulatorView, InputMethodManager.SHOW_IMPLICIT);
	                	
	                	bluetoothNameLabel.setText(R.string.title_connected_to);
	                	bluetoothNameLabel.append(mConnectedDeviceName);
                		bluetoothEnabled = true;
	                    break;
	                    
	                case BluetoothSerialService.STATE_CONNECTING:
	                	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE/STATE_CONNECTING");
	                	bluetoothNameLabel.setText(R.string.title_connecting);
	                    break;
	                    
	                case BluetoothSerialService.STATE_LISTEN:
	                	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE/STATE_LISTEN");
	                case BluetoothSerialService.STATE_NONE:
	                	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE/STATE_NONE");
	                	if (mMenuItemConnect != null) {
	                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
	                		mMenuItemConnect.setTitle(R.string.connect);
	                	}

	            		//I have to replace this line with whatever I am using as an input to the bluetooth device
	//-----                	mInputManager.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);
	                	bluetoothNameLabel.setText(R.string.title_not_connected);
                		bluetoothEnabled = false;
	                	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE/STATE_CONNECTED/CACA");

	                    break;
	                }
	                break;
	            case MESSAGE_WRITE:
	            	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_WRITE " + msg.arg1);
	            	if (mLocalEcho) {
	            		byte[] writeBuf = (byte[]) msg.obj;
	            		//mEmulatorView.write(writeBuf, msg.arg1);
	            	}
	                
	                break;
	                
	            case MESSAGE_READ:
	            	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_READ " + msg.arg1);
	            	byte[] readBuf = (byte[]) msg.obj;              
	                //mEmulatorView.write(readBuf, msg.arg1);
	                
	                break;
	                
	            case MESSAGE_DEVICE_NAME:
	            	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_DEVICE_NAME: " + msg.arg1);
	            	// save the connected device's name
	                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	                Toast.makeText(getApplicationContext(), "Connected to "
	                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	                break;
	            case MESSAGE_TOAST:
	            	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_TOAST: " + msg.arg1);
	            	Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                               Toast.LENGTH_SHORT).show();
	                break;
	            }
	        }
	    };
	 
    /*
     * surfaceChanged() is automatically called whenever the screen changes,
     * including when the app is started.
     * 
     * This method sets the camera to display the preview on mSurfaceView,
     * sets the preview to the appropriate size,
     * and starts the preview.
     */
    SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {}
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			if (previewRunning)
				stopCameraPreview();
			setCameraParameters();
			/*Display display = ((WindowManager)(activity.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
			Camera.Parameters parameters = mCamera.getParameters();
		  //  Camera.Size mPreviewSize = getPreviewSize(parameters, width, height);
		  //  parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
			try {
				int orientation = display.getRotation();
				int rotation = 0;
				switch (orientation) {
					case Surface.ROTATION_0:
						//parameters.setPreviewSize(mPreviewSize.height, mPreviewSize.width); //No need to resize the picture
						rotation = 90;
						break;
					case Surface.ROTATION_180:
						//parameters.setPreviewSize(mPreviewSize.height, mPreviewSize.width);
						rotation = 270;
						break;
					case Surface.ROTATION_270:
						//parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
						rotation = 180;
						break;
					case Surface.ROTATION_90:
						rotation = 0;
						break;
				}
				mCamera.setDisplayOrientation(rotation);
				mCamera.setParameters(parameters);
				mCamera.setPreviewDisplay(mHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			mCamera.setParameters(parameters);*/
		    startCameraPreview();
		}
		
		public void surfaceDestroyed(SurfaceHolder holder) {}
	};
	
	
	/**
	 * Called as the picture is being taken. Contains code for saving the picture data to an image file.
	 */
	PictureCallback mPicture = new PictureCallback() {
	    public void onPictureTaken(byte[] data, Camera camera) {
	        File fileName = getOutputMediaFile(MEDIA_TYPE_IMAGE);
	        if (fileName == null){
	            System.out.println("Error creating media file, check storage permissions: ");
	            return;
	        }
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
	        stopCameraPreview();
	        startCameraPreview();
	        cameraBusy = false;
	    }
	};
	
	/*
	 * onShutter() is called as the picture is taken.
	 */
	ShutterCallback mShutter = new ShutterCallback() {
		public void onShutter() {
			
		}
	};
	
	
	/*
	 * Controls responses to touching the screen
	 */
	View.OnTouchListener touchListener = new View.OnTouchListener() {
		double pinchDist;
		int lastZoom;
		double screenDiagonal;
		double maxZoom;
		
		public boolean onTouch(View v, MotionEvent event) {
			if (cameraBusy)
				return true;
			
			int pointers = event.getPointerCount();
			int action = event.getActionMasked();
			int newState = stopMotor;
			//Pinch zoom
			if (pointers == 2){
				if (maxZoom == 0)
					maxZoom = mCamera.getParameters().getMaxZoom();
				if (screenDiagonal == 0)
					screenDiagonal = getScreenDiagonal(CameraActivity.this);
				
				double newDist = Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
				if (action == MotionEvent.ACTION_MOVE) {
					if (pinchDist != firstTouchEvent) { //Prevents jumping
						int newZoom = (int)((newDist-pinchDist) / screenDiagonal * maxZoom * 2);
						zoom(newZoom - lastZoom);
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
			
			else if (bluetoothEnabled && pointers == 1) {
				if (action == MotionEvent.ACTION_DOWN) {
					touchX = event.getX();
					touchY = event.getY();
				}
				else if (action == MotionEvent.ACTION_MOVE) {
					double x = event.getX() - touchX;
					double y = event.getY() - touchY;
					double absX = Math.abs(x);
					double absY = Math.abs(y);
					if (absX >= absY && absX >= PAN_THRESHOLD) {
						newState = x > 0 ? xRightMotor : xLeftMotor;
					}
					else if (absY > absX && absY > PAN_THRESHOLD) {
						newState = y > 0 ? yForwardMotor : yBackMotor;
					}
				}
				else if (action == MotionEvent.ACTION_UP) {
					newState = stopMotor;
					touchX = touchY = firstTouchEvent;
				}
			}
			
			/*
			else if (bluetoothEnabled && pointers == 3) {
				if (action == MotionEvent.ACTION_DOWN) {
					touchY = (event.getY(0) + event.getY(1) + event.getY(2)) / 3;
					System.out.println(touchY);
				}
				else if (action == MotionEvent.ACTION_MOVE) {
					double y = (event.getY(0) + event.getY(1) + event.getY(2)) / 3;
					y -= touchY;
					System.out.println(y);
					if (Math.abs(y) >= PAN_THRESHOLD)
						newState = y > 0 ? zUpMotor : zDownMotor;
				}
				else if (action == MotionEvent.ACTION_UP) {
					touchY = firstTouchEvent;
					newState = stopMotor;
				}
			}*/
			

			if (bluetoothEnabled && newState != panState) {
				panState = newState;
				byte[] buffer = new byte[1];
	        	buffer[0] = (byte)panState;
	        	mSerialService.write(buffer);	
			}
			return true;
		}
	};

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_camera);
        mSurfaceView = (SurfaceView)findViewById(R.id.previewSurface);
        mSurfaceView.setOnTouchListener(touchListener);
        mHolder = mSurfaceView.getHolder();
	    mHolder.addCallback(mCallback);
	    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	    zoomText = (TextView)findViewById(R.id.zoomtext);
	   // RotateAnimation rotate= (RotateAnimation)AnimationUtils.loadAnimation(this,R.anim.rotate_textview);
	    zoomText.setText("100%");
	   // zoomText.setAnimation(rotate);
	    takePhoto = (ImageButton)findViewById(R.id.takePhotoButton);
	    switchMode = (ImageButton)findViewById(R.id.switchCameraMode);
	    zoomIn = (ImageButton)findViewById(R.id.zoomInButton);
	    zoomOut = (ImageButton)findViewById(R.id.zoomOutButton);
	    
	    mSerialService = new BluetoothSerialService(this, mHandlerBT/*, mEmulatorView*/);


		bluetoothNameLabel = (TextView) findViewById(R.id.bluetoothtext);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		//BluetoothAdapter myBluetoothAdapter = null; //This was to test to see what the noBluetoothAdapter() method did
		if (mBluetoothAdapter == null){
			mMenuItemConnect.setEnabled(false);
		}
		
		
    }
    
    @Override
    public void onStart() {
    	super.onStart();
		mEnablingBT = false;
		forceUpdateCamera = false;
    }
    /*
     * This is automatically called when the application is opened
     * or resumed.
     */
    public void onResume() {
    	super.onResume();
    	if (!safeCameraOpen())
    		return;
    	/* Set up the camera parameters. Not doing this will cause the preview to stay black.
    	 * Usually this is done in surfaceChanged in SurfaceHolder.Callback,
    	 * but surfaceChange is not called when resuming from non-fullscreen Activities such as dialogs.
    	 * Setting forceUpdateCamera to true whenever these kinds of Activities completes gets around this*/
    	if (forceUpdateCamera) {
    		setCameraParameters();
    		forceUpdateCamera = false;
    	}
		startCameraPreview();
    }
    
    public void onPause() {
    	super.onPause();
    	if (recording)
    		stopRecording();
    	stopCameraPreview();
    	releaseCameraAndPreview();
    }
    
    public void onDestroy() {
    	super.onDestroy();
        if (mSerialService != null)
        	mSerialService.stop();
    }
    
    private void setCameraParameters() {
    	Camera.Parameters parameters = mCamera.getParameters();
		try {
			int rotation = 0;
			mCamera.setDisplayOrientation(rotation);
			mCamera.setParameters(parameters);
			mCamera.setPreviewDisplay(mHolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mCamera.setParameters(parameters);
    }
    
    boolean safeCameraOpen() {
        boolean qOpened = false;
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(); /* This is the important thing!
            							It makes an instance of a Camera object that
            							lets the application do stuff with the hardware.
            							*/
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(activity.getString(R.string.app_name), "failed to open Camera");
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
	public static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    
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
	        mediaFile = new File(videoStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
	public void startCameraPreview() {
		mCamera.startPreview();
		previewRunning = true;
	}
	
	public void stopCameraPreview() {
		if (mCamera != null)
			mCamera.stopPreview();
		previewRunning = false;
	}
	
	public synchronized void takePhoto(View v) {
		System.out.println("take photo - " + cameraBusy);
		if (cameraBusy)
			return;
		if (!videoState) {
			cameraBusy = true;
			mCamera.takePicture(mShutter, null, mPicture);
		}
		else {
			if (!recording) {
				startRecording();
				takePhoto.setImageResource(R.drawable.stop);
				switchMode.setVisibility(View.INVISIBLE);
				zoomIn.setVisibility(View.INVISIBLE);
				zoomOut.setVisibility(View.INVISIBLE);
			}
			else {
				stopRecording();
				takePhoto.setImageResource(R.drawable.record);
				switchMode.setVisibility(View.VISIBLE);
				zoomIn.setVisibility(View.VISIBLE);
				zoomOut.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void startRecording() {
		if (cameraBusy)
			return;
		System.out.println("start - lock enabled");
		cameraBusy = true;
		mCamera.unlock();
		recorder = new MediaRecorder();
		recorder.setCamera(mCamera);
		recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		recorder.setOutputFormat(profile.fileFormat);
		recorder.setVideoFrameRate(profile.videoFrameRate);
		recorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
		recorder.setVideoEncodingBitRate(profile.videoBitRate);
		recorder.setVideoEncoder(profile.videoCodec);
		recorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
		recorder.setPreviewDisplay(mHolder.getSurface());
		try {
			recorder.prepare();
			recorder.start();
			recording = true;
			System.out.println("start - lock disabled");
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		cameraBusy = false;
	}
	
	private void stopRecording() {
		System.out.println(cameraBusy + " " + recording);
		if (cameraBusy || !recording)
			return;
		System.out.println("stop - lock enabled");
		cameraBusy = true;
		recorder.stop();
		recorder.reset();
		recorder.release();
		mCamera.lock();
		stopCameraPreview();
		startCameraPreview();
		recording = false;
		cameraBusy = false;

		System.out.println("stop - lock disabled");
	}
	
	private void zoom(int step) {
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
		zoomText.setText(str);
		mCamera.setParameters(parameters);
	}
	
	public void zoomIn(View view) {
		if (cameraBusy)
			return;
		zoom(10);
	}
	public void zoomOut(View view) {
		if (cameraBusy)
			return;
		zoom(-10);
	}
	
	public void switchMode(View view) {
		if (recording || cameraBusy)
			return;
		videoState = !videoState;
		if (videoState) {
			switchMode.setImageResource(R.drawable.camera);
			takePhoto.setImageResource(R.drawable.record);
		}
		else {
			switchMode.setImageResource(R.drawable.record);
			takePhoto.setImageResource(R.drawable.camera);
		}
	}
	

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bluetooth, menu);
        mMenuItemConnect = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.connect:
        	proceedWithConnection = true;
        	if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
        		if (!mEnablingBT) { // If we are turning on the BT we cannot check if it's enable
        		    if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
                		proceedWithConnection = false;           	
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.alert_dialog_turn_on_bt)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.alert_dialog_warning_title)
                            .setCancelable( false )
                            .setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                            	public void onClick(DialogInterface dialog, int id) {
                            		mEnablingBT = true;
                            		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
                            	}
                            })
                            .setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
                            	public void onClick(DialogInterface dialog, int id) {
                            	}
                            });
                        AlertDialog alert = builder.create();
                        alert.show();
        		    }		
        		
        		    if (mSerialService != null) {
        		    	// Only if the state is STATE_NONE, do we know that we haven't started already
        		    	if (mSerialService.getState() == BluetoothSerialService.STATE_NONE) {
        		    		// Start the Bluetooth chat services
        		    		mSerialService.start();
        		    	}
        		    }
        		}
        		if (proceedWithConnection) {
	        		// Launch the DeviceListActivity to see devices and do scan
	        		Intent serverIntent = new Intent(this, DeviceListActivity.class);
	        		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        		}
        	}
        	else
            	if (getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
            		mSerialService.stop();
		    		mSerialService.start();
            	}
            return true;
        //case R.id.preferences:
        	//doPreferences();
            //return true;
        //case R.id.menu_special_keys:
            //doDocumentKeys();
            //return true;
        }
        return false;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:
            forceUpdateCamera = true;
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address); //I deleted "m." before the method getRemoteDevice()
                // Attempt to connect to the device
                mSerialService.connect(device);
            }
            break;

        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode != Activity.RESULT_OK) {
                Log.d(LOG_TAG, "BT not enabled");
                forceUpdateCamera = true;
                mEnablingBT = false;
                //finishDialogNoBluetooth();                
            }
            else {
            	Intent serverIntent = new Intent(this, DeviceListActivity.class);
            	startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        }
    }
    
	public int getConnectionState() {
		return mSerialService.getState();
	}
	
	public static double getScreenDiagonal(Activity activity) {
		int width, height;
		Display display = activity.getWindowManager().getDefaultDisplay();
		if (Build.VERSION.SDK_INT < 13) {
			width = display.getWidth();
			height = display.getHeight();
		}
		else {
		    Point size = new Point();
		    display.getSize(size);
		    width = size.x;
		    height = size.y;	
		}
	    return Math.hypot(width, height);
	}
	

	private void toast(String message) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}
}
