package edu.berkeley.cellscope.cscore.cameraui;

import edu.berkeley.cellscope.cscore.BluetoothActivity;
import android.app.Activity;
import android.view.MotionEvent;

/*
 * Touch listener that responds to single-finger gestures.
 */

public class TouchSwipeControl extends TouchControl {
	private BluetoothControllable stage;
	private double touchX, touchY;
	
	private static final double SENSITIVITY = 0.1;

    public static final int xRightMotor = BluetoothActivity.xRightMotor;
    public static final int xLeftMotor = BluetoothActivity.xLeftMotor;
    public static final int yBackMotor = BluetoothActivity.yBackMotor;
    public static final int yForwardMotor = BluetoothActivity.yForwardMotor;
    public static final int zUpMotor = BluetoothActivity.zUpMotor;
    public static final int zDownMotor = BluetoothActivity.zDownMotor;
    public static final int stopMotor = 0;
	
	public TouchSwipeControl(BluetoothControllable s, Activity activity) {
		super(activity);
		stage = s;
		touchX = touchY = firstTouchEvent;
	}
	
	@Override
	public boolean touch(MotionEvent event) {
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();
	
		if (pointers == 1) {
			if (action == MotionEvent.ACTION_DOWN) {
				touchX = event.getX();
				touchY = event.getY();
			}
			else if (action == MotionEvent.ACTION_UP) {
				if (touchX != firstTouchEvent && touchY != firstTouchEvent)
					swipeStage(event.getX() - touchX, event.getY() - touchY);
				touchX = touchY = firstTouchEvent;
			}
		}
		else
			touchX = touchY = firstTouchEvent;
		return true;
	}
	
	public void swipeStage(double x, double y) {
		if (!stage.controlReady())
			return;
		int dir = 0, dist = 0;
		if (Math.abs(x) > Math.abs(y)) {
			dir = x > 0 ? TouchPanControl.yForwardMotor : TouchPanControl.yBackMotor;
			dist = (int)(Math.abs(x) * SENSITIVITY);
		}
		else {
			dir = y < 0 ? TouchPanControl.xLeftMotor : TouchPanControl.xRightMotor;
			dist = (int)(Math.abs(y) * SENSITIVITY);
		}
		swipe(dir, dist);
	}
	

	public void swipe(int dir, int dist) {
		BluetoothConnector bt = stage.getBluetooth();
		byte[] buffer = new byte[1];
		buffer[0] = (byte)dir;
		bt.write(buffer);
		byte[] buffer2 = new byte[1];
		buffer2[0] = (byte)dist;
		bt.write(buffer2);
	}
	
	public boolean bluetoothConnected() {
		return stage.controlReady();
	}
	
}