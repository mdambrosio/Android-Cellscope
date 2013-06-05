package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothActivity extends Activity implements OnTouchListener {
	//MotorButton and its listener
	private Vibrator vibr;
    private Button xRightButton;
    private Button xLeftButton;
    private Button yBackButton;
    private Button yForwardButton;
    private Button zUpButton;
    private Button zDownButton;
    
    //Assign bytes and their variable names that will transmit
    public static final int xRightMotor = 1;
    public static final int xLeftMotor = 2;
    public static final int yBackMotor = 3;
    public static final int yForwardMotor = 4;
    public static final int zUpMotor = 5;
    public static final int zDownMotor = 6;
	
	// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private static TextView mTitle;

    // Name of the connected device
    private String mConnectedDeviceName = null;

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

    // Message types sent from the BluetoothReadService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;	

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
	private BluetoothAdapter mBluetoothAdapter = null;
	
    /**
     * A key listener that tracks the modifier keys and allows the full ASCII
     * character set to be entered.
     */
    private TermKeyListener mKeyListener;
		
	
    private static BluetoothSerialService mSerialService = null;
    
	private static InputMethodManager mInputManager;
	
	private boolean mEnablingBT;
    private boolean mLocalEcho = false;
    private int mFontSize = 9;
    private int mColorId = 2;
    private int mControlKeyId = 0;

    private static final String LOCALECHO_KEY = "localecho";
    private static final String FONTSIZE_KEY = "fontsize";
    private static final String COLOR_KEY = "color";
    private static final String CONTROLKEY_KEY = "controlkey";

    public static final int WHITE = 0xffffffff;
    public static final int BLACK = 0xff000000;
    public static final int BLUE = 0xff344ebd;

    private static final int[][] COLOR_SCHEMES = {
        {BLACK, WHITE}, {WHITE, BLACK}, {WHITE, BLUE}};

    private static final int[] CONTROL_KEY_SCHEMES = {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_AT,
        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_ALT_RIGHT
    };
//    private static final String[] CONTROL_KEY_NAME = {
//        "Ball", "@", "Left-Alt", "Right-Alt"
//    };
    private static String[] CONTROL_KEY_NAME;

    private int mControlKeyCode;

    private SharedPreferences mPrefs;
	
    private MenuItem mMenuItemConnect;

    

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (DEBUG)
			Log.e(LOG_TAG, "+++ ON CREATE +++");


//        CONTROL_KEY_NAME = getResources().getStringArray(R.array.entries_controlkey_preference);

    	mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);	

        // Set up the window layout
//        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);    	

    	setContentView(R.layout.activity_bluetooth);
//        if (DEBUG)
			Log.e(LOG_TAG, "ONCREATE/setupwindowlayout");
//    	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
    	if (DEBUG)
			Log.e(LOG_TAG, "ONCREATE/setupthecustomtitle");
        //Set up the custom title
//        mTitle = (TextView) findViewById(R.id.title_left_text);
//        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.status_indicator);
        

    	
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		//BluetoothAdapter myBluetoothAdapter = null; //This was to test to see what the noBluetoothAdapter() method did
		if (mBluetoothAdapter == null){
			//Call the noBluetoothAdapter method
			finishDialogNoBluetooth();
			return;
		}

        
		//mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		//if (mBluetoothAdapter == null) {
        //    finishDialogNoBluetooth();
		//	return;
		//}
		
 /*       setContentView(R.layout.term_activity);

        mEmulatorView = (EmulatorView) findViewById(R.id.emulatorView);

        mEmulatorView.initialize( this );

        mKeyListener = new TermKeyListener();

        mEmulatorView.setFocusable(true);
        mEmulatorView.setFocusableInTouchMode(true);
        mEmulatorView.requestFocus();
        mEmulatorView.register(mKeyListener);
*/
        
		mSerialService = new BluetoothSerialService(this, mHandlerBT/*, mEmulatorView*/);        
        
		vibr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
		//Create our buttons and their listener
		xRightButton = (Button) findViewById(R.id.xRightMotorButton);
        xRightButton.setOnTouchListener(this);
        xLeftButton = (Button) findViewById(R.id.xLeftMotorButton);
        xLeftButton.setOnTouchListener(this);
        yBackButton = (Button) findViewById(R.id.yBackMotorButton);
        yBackButton.setOnTouchListener(this);
        yForwardButton = (Button) findViewById(R.id.yForwardMotorButton);
        yForwardButton.setOnTouchListener(this);
        zUpButton = (Button) findViewById(R.id.zUpMotorButton);
        zUpButton.setOnTouchListener(this);
        zDownButton = (Button) findViewById(R.id.zDownMotorButton);
        zDownButton.setOnTouchListener(this);
        
        
        
        
		if (DEBUG)
			Log.e(LOG_TAG, "+++ DONE IN ON CREATE +++");
	}
	

	
	//This method will process "Touch" events and transmits them
	public boolean onTouch(View v, MotionEvent event){

		//declare a temporary variable
		int myTempButton = 0;
		switch(v.getId()){
		case R.id.xRightMotorButton:
			myTempButton = xRightMotor;
			break;
		case R.id.xLeftMotorButton:
			myTempButton = xLeftMotor;
			break;
		case R.id.yBackMotorButton:
			myTempButton = yBackMotor;
			break;
		case R.id.yForwardMotorButton:
			myTempButton = yForwardMotor;
			break;
		case R.id.zUpMotorButton:
			myTempButton = zUpMotor;
			break;
		case R.id.zDownMotorButton:
			myTempButton = zDownMotor;
			break;
		}
		switch(event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
        	//vibr.vibrate(100); Remove the vibrator; it's annoying!
	        if (myTempButton >= 1) {
	        	byte[] buffer = new byte[1];
	        	buffer[0] = (byte)myTempButton;
	        	mSerialService.write(buffer);
	        }
          return true;
        case MotionEvent.ACTION_UP:
        	//vibr.vibrate(100); Remove the vibrator; it's annoying!
        	byte[] buffer = new byte[1];
        	myTempButton = 0; //Stop will mean zero
        	buffer[0] = (byte)myTempButton; 
        	mSerialService.write(buffer);
          return true;
        default:
          return false;
      }
    }

	@Override
	public void onStart() {
		super.onStart();
		if (DEBUG)
			Log.e(LOG_TAG, "++ ON START ++");
		
		mEnablingBT = false;
	}

	@Override
	public synchronized void onResume() {
		super.onResume();

		if (DEBUG) {
			Log.e(LOG_TAG, "+ ON RESUME +");
		}
		
		if (!mEnablingBT) { // If we are turning on the BT we cannot check if it's enable
		    if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
			
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
                    		finishDialogNoBluetooth();            	
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

		    /*if (mBluetoothAdapter != null) {
		    	readPrefs();
		    	updatePrefs();

		    	mEmulatorView.onResume();
		    }*/
		}
	}
/*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mEmulatorView.updateSize();
    }
*/
	@Override
	public synchronized void onPause() {
		super.onPause();
		if (DEBUG)
			Log.e(LOG_TAG, "- ON PAUSE -");

		/*if (mEmulatorView != null) {
			mInputManager.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);
			mEmulatorView.onPause();
		}*/
	}

    @Override
    public void onStop() {
        super.onStop();
        if(DEBUG)
        	Log.e(LOG_TAG, "-- ON STOP --");
    }


	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG)
			Log.e(LOG_TAG, "--- ON DESTROY ---");
		
        if (mSerialService != null)
        	mSerialService.stop();
        
	}
/*
    private void readPrefs() {
        mLocalEcho = mPrefs.getBoolean(LOCALECHO_KEY, mLocalEcho);
        mFontSize = readIntPref(FONTSIZE_KEY, mFontSize, 20);
        mColorId = readIntPref(COLOR_KEY, mColorId, COLOR_SCHEMES.length - 1);
        mControlKeyId = readIntPref(CONTROLKEY_KEY, mControlKeyId,
                CONTROL_KEY_SCHEMES.length - 1);
    }

    private void updatePrefs() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mEmulatorView.setTextSize((int) (mFontSize * metrics.density));
        setColors();
        mControlKeyCode = CONTROL_KEY_SCHEMES[mControlKeyId];
    }

    private int readIntPref(String key, int defaultValue, int maxValue) {
        int val;
        try {
            val = Integer.parseInt(
                mPrefs.getString(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            val = defaultValue;
        }
        val = Math.max(0, Math.min(val, maxValue));
        return val;
    }
*/ 
	public int getConnectionState() {
		return mSerialService.getState();
	}


    public void send(byte[] out) {
    	mSerialService.write( out );
    }
    
    public void toggleKeyboard() {
  		mInputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
    
    public int getTitleHeight() {
    	return mTitle.getHeight();
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
                	
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                	if(DEBUG) Log.i(LOG_TAG, "MESSAGE_STATE_CHANGE/STATE_CONNECTING");
                	mTitle.setText(R.string.title_connecting);
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
                    mTitle.setText(R.string.title_not_connected);
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

  //This method warns the user that no bluetooth adapter exists on their phone and will close the app
    public void finishDialogNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_bt)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle(R.string.app_name)
        .setCancelable( false )
        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       finish();            	
                	   }
               });
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(DEBUG) Log.d(LOG_TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        
        case REQUEST_CONNECT_DEVICE:

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
                
                finishDialogNoBluetooth();                
            }
        }
    }
    
/*    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (handleControlKey(keyCode, true)) {
            return true;
        } else if (isSystemKey(keyCode, event)) {
            // Don't intercept the system keys
            return super.onKeyDown(keyCode, event);
        } else if (handleDPad(keyCode, true)) {
            return true;
        }

        // Translate the keyCode into an ASCII character.
        int letter = mKeyListener.keyDown(keyCode, event);

        if (letter >= 0) {
        	byte[] buffer = new byte[1];
        	buffer[0] = (byte)letter;
        	mSerialService.write(buffer);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (handleControlKey(keyCode, false)) {
            return true;
        } else if (isSystemKey(keyCode, event)) {
            // Don't intercept the system keys
            return super.onKeyUp(keyCode, event);
        } else if (handleDPad(keyCode, false)) {
            return true;
        }

        mKeyListener.keyUp(keyCode);
        return true;
    }

    private boolean handleControlKey(int keyCode, boolean down) {
        if (keyCode == mControlKeyCode) {
            mKeyListener.handleControlKey(down);
            return true;
        }
        return false;
    }
*/
    /**
     * Handle dpad left-right-up-down events. Don't handle
     * dpad-center, that's our control key.
     * @param keyCode
     * @param down
     */
    private boolean handleDPad(int keyCode, boolean down) {
    	byte[] buffer = new byte[1];

        if (keyCode < KeyEvent.KEYCODE_DPAD_UP ||
                keyCode > KeyEvent.KEYCODE_DPAD_CENTER) {
            return false;
        }

        if (down) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            	buffer[0] = '\r';
            	mSerialService.write( buffer );
            } else {
                char code;
                switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    code = 'A';
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    code = 'B';
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    code = 'D';
                    break;
                default:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    code = 'C';
                    break;
                }
            	buffer[0] = 27; // ESC
            	mSerialService.write( buffer );
//-------------
                if (false) { //Write dummy code for now; it used to say mEmulatorView.getKeypadApplicationMode()
                	buffer[0] = 'O';
                	mSerialService.write( buffer );                    
                } else {
                	buffer[0] = '[';
                	mSerialService.write( buffer );                    
                }
            	buffer[0] = (byte)code;
            	mSerialService.write( buffer );                    
            }
        }
        return true;
    }

    private boolean isSystemKey(int keyCode, KeyEvent event) {
        return event.isSystem();
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
        	
        	if (getConnectionState() == BluetoothSerialService.STATE_NONE) {
        		// Launch the DeviceListActivity to see devices and do scan
        		Intent serverIntent = new Intent(this, DeviceListActivity.class);
        		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
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
    
    //private void doPreferences() {
        //startActivity(new Intent(this, TermPreferences.class));
    //}

    //private void setColors() {
    //    int[] scheme = COLOR_SCHEMES[mColorId];
    //    mEmulatorView.setColors(scheme[0], scheme[1]);
    //}

/*    private void doDocumentKeys() {
        String controlKey = CONTROL_KEY_NAME[mControlKeyId];
        new AlertDialog.Builder(this).
            setTitle( getString(R.string.title_document_key_press) + " \"" + controlKey + "\" "+ getString(R.string.title_document_key_rest)).
            setMessage(" Space ==> Control-@ (NUL)\n"
                    + " A..Z ==> Control-A..Z\n"
                    + " I ==> Control-I (TAB)\n"
                    + " 1 ==> Control-[ (ESC)\n"
                    + " 5 ==> Control-_\n"
                    + " . ==> Control-\\\n"
                    + " 0 ==> Control-]\n"
                    + " 6 ==> Control-^").
            show();
     }
*/		


}



/**
 * An ASCII key listener. Supports control characters and escape. Keeps track of
 * the current state of the alt, shift, and control keys.
 */
class TermKeyListener {
    /**
     * The state engine for a modifier key. Can be pressed, released, locked,
     * and so on.
     *
     */
    private class ModifierKey {

        private int mState;

        private static final int UNPRESSED = 0;

        private static final int PRESSED = 1;

        private static final int RELEASED = 2;

        private static final int USED = 3;

        private static final int LOCKED = 4;

        /**
         * Construct a modifier key. UNPRESSED by default.
         *
         */
        public ModifierKey() {
            mState = UNPRESSED;
        }

        public void onPress() {
            switch (mState) {
            case PRESSED:
                // This is a repeat before use
                break;
            case RELEASED:
                mState = LOCKED;
                break;
            case USED:
                // This is a repeat after use
                break;
            case LOCKED:
                mState = UNPRESSED;
                break;
            default:
                mState = PRESSED;
                break;
            }
        }

        public void onRelease() {
            switch (mState) {
            case USED:
                mState = UNPRESSED;
                break;
            case PRESSED:
                mState = RELEASED;
                break;
            default:
                // Leave state alone
                break;
            }
        }

        public void adjustAfterKeypress() {
            switch (mState) {
            case PRESSED:
                mState = USED;
                break;
            case RELEASED:
                mState = UNPRESSED;
                break;
            default:
                // Leave state alone
                break;
            }
        }

        public boolean isActive() {
            return mState != UNPRESSED;
        }
    }

    private ModifierKey mAltKey = new ModifierKey();

    private ModifierKey mCapKey = new ModifierKey();

    private ModifierKey mControlKey = new ModifierKey();

    /**
     * Construct a term key listener.
     *
     */
    public TermKeyListener() {
    }

    public void handleControlKey(boolean down) {
        if (down) {
            mControlKey.onPress();
        } else {
            mControlKey.onRelease();
        }
    }

    public int mapControlChar(int ch) {
        int result = ch;
        if (mControlKey.isActive()) {
            // Search is the control key.
            if (result >= 'a' && result <= 'z') {
                result = (char) (result - 'a' + '\001');
            } else if (result == ' ') {
                result = 0;
            } else if ((result == '[') || (result == '1')) {
                result = 27;
            } else if ((result == '\\') || (result == '.')) {
                result = 28;
            } else if ((result == ']') || (result == '0')) {
                result = 29;
            } else if ((result == '^') || (result == '6')) {
                result = 30; // control-^
            } else if ((result == '_') || (result == '5')) {
                result = 31;
            }
        }

        if (result > -1) {
            mAltKey.adjustAfterKeypress();
            mCapKey.adjustAfterKeypress();
            mControlKey.adjustAfterKeypress();
        }
        return result;
    }

    /**
     * Handle a keyDown event.
     *
     * @param keyCode the keycode of the keyDown event
     * @return the ASCII byte to transmit to the pseudo-teletype, or -1 if this
     *         event does not produce an ASCII byte.
     */
    public int keyDown(int keyCode, KeyEvent event) {
        int result = -1;
        switch (keyCode) {
        case KeyEvent.KEYCODE_ALT_RIGHT:
        case KeyEvent.KEYCODE_ALT_LEFT:
            mAltKey.onPress();
            break;

        case KeyEvent.KEYCODE_SHIFT_LEFT:
        case KeyEvent.KEYCODE_SHIFT_RIGHT:
            mCapKey.onPress();
            break;

        case KeyEvent.KEYCODE_ENTER:
            // Convert newlines into returns. The vt100 sends a
            // '\r' when the 'Return' key is pressed, but our
            // KeyEvent translates this as a '\n'.
            result = '\r';
            break;

        case KeyEvent.KEYCODE_DEL:
            // Convert DEL into 127 (instead of 8)
            result = 127;
            break;

        default: {
            result = event.getUnicodeChar(
                   (mCapKey.isActive() ? KeyEvent.META_SHIFT_ON : 0) |
                   (mAltKey.isActive() ? KeyEvent.META_ALT_ON : 0));
            break;
            }
        }

        result = mapControlChar(result);

        return result;
    }

    /**
     * Handle a keyUp event.
     *
     * @param keyCode the keyCode of the keyUp event
     */
    public void keyUp(int keyCode) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ALT_LEFT:
        case KeyEvent.KEYCODE_ALT_RIGHT:
            mAltKey.onRelease();
            break;
        case KeyEvent.KEYCODE_SHIFT_LEFT:
        case KeyEvent.KEYCODE_SHIFT_RIGHT:
            mCapKey.onRelease();
            break;
        default:
            // Ignore other keyUps
            break;
        }
    }
}
