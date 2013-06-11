package edu.berkeley.cellscope.cscore.celltracker;

import java.io.File;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import edu.berkeley.cellscope.cscore.BluetoothActivity;
import edu.berkeley.cellscope.cscore.BluetoothSerialService;
import edu.berkeley.cellscope.cscore.CameraActivity;
import edu.berkeley.cellscope.cscore.DeviceListActivity;
import edu.berkeley.cellscope.cscore.R;

public class OpenCVCameraActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener {

	private static final String TAG = "OpenCV_Camera";
	
	private OpenCVCameraView mOpenCvCameraView;
	TextView zoomText;
	
	double pinchDist;
	int lastZoom;
	double screenDiagonal;
	double maxZoom;
	double zZone;
	
	public static File mediaStorageDir = CameraActivity.mediaStorageDir;
	private static final int firstTouchEvent = -1;
	private static final double PAN_THRESHOLD = 25;
	private static final double Z_CONTROL_ZONE = 0.1;
	
	private static final int COMPRESSION_QUALITY = 90;
	
	//Bluetooth stuff

    private MenuItem mMenuItemConnect;
    private static BluetoothSerialService mSerialService = null;
    private boolean mEnablingBT;
    
    private int panState;
    private float touchX, touchY;
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
	    zZone = CameraActivity.getScreenHeight(this) * Z_CONTROL_ZONE;
	    maxZoom = -1;
	    
	    mOpenCvCameraView.setOnTouchListener(this);
	    
	    mSerialService = new BluetoothSerialService(this, mHandlerBT/*, mEmulatorView*/);


		bluetoothNameLabel = (TextView) findViewById(R.id.opencv_bluetoothtext);

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
        if (mSerialService != null)
        	mSerialService.stop();
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
		int newState = stopMotor;
		//Pinch zoom
		if (pointers == 2){
			if (maxZoom == -1)
				maxZoom = mOpenCvCameraView.getMaxZoom();
			double newDist = Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
			if (action == MotionEvent.ACTION_MOVE) {
				if (pinchDist != firstTouchEvent) { //Prevents jumping
					int newZoom = (int)((newDist-pinchDist) / screenDiagonal * maxZoom * 2);
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
					System.out.println(touchX + " " + zZone);
					if (touchX < zZone)
						newState = y > 0 ? zUpMotor : zDownMotor;
					else
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
            //forceUpdateCamera = true;
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
                //forceUpdateCamera = true;
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
}
