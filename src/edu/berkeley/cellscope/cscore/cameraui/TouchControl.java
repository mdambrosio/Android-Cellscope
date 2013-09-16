package edu.berkeley.cellscope.cscore.cameraui;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import edu.berkeley.cellscope.cscore.BluetoothActivity;
import edu.berkeley.cellscope.cscore.ScreenDimension;

/*
 * Parent class.
 * Classes that extend this one are to be used as touch listeners for specific actions,
 * and should be used in conjunction with the CompoundListener class.
 */
public abstract class TouchControl implements View.OnTouchListener {
	private boolean enabled;
	protected final int screenWidth, screenHeight;
	protected static final int firstTouchEvent = -1;

	
	private static final int xRightMotor = BluetoothActivity.xRightMotor;
    private static final int xLeftMotor = BluetoothActivity.xLeftMotor;
    private static final int yBackMotor = BluetoothActivity.yBackMotor;
    private static final int yForwardMotor = BluetoothActivity.yForwardMotor;
    private static final int zUpMotor = BluetoothActivity.zUpMotor;
    private static final int zDownMotor = BluetoothActivity.zDownMotor;
    
    public static final int xPositive = yForwardMotor;
    public static final int xNegative = yBackMotor;
    public static final int yPositive = xRightMotor;
    public static final int yNegative = xLeftMotor;
    public static final int zPositive = zUpMotor;
    public static final int zNegative = zDownMotor;
    public static final int stopMotor = 0;
    
	public TouchControl(Activity activity) {
		this(ScreenDimension.getScreenWidth(activity), ScreenDimension.getScreenHeight(activity));
	}
	
	public TouchControl(int w, int h) {
		setEnabled(false);
		screenWidth = w;
		screenHeight = h;
	}
	
	public void setEnabled(boolean b) {
		enabled = b;
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		if (!enabled)
			return false;
		else
			return touch(event);
	}
	
	protected abstract boolean touch(MotionEvent event);
	

	public static interface TouchControllable {
	
		public double getDiagonal();
	}
	
	public static interface BluetoothControllable {
		public boolean controlReady();
		public BluetoothConnector getBluetooth();
		
		public static final int PROCEED = 0;
		public static final int FAILED = 1;
	}

}
