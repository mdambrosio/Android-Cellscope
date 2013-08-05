package edu.berkeley.cellscope.cscore.cameraui;

import edu.berkeley.cellscope.cscore.BluetoothActivity;
import android.app.Activity;
import android.view.MotionEvent;

/*
 * Touch listener that responds to single-finger gestures.
 */

public class TouchSlideControl extends TouchControl {
	private Slideable stage;
	private double touchX, touchY;
	
    public static final int xRightMotor = BluetoothActivity.xRightMotor;
    public static final int xLeftMotor = BluetoothActivity.xLeftMotor;
    public static final int yBackMotor = BluetoothActivity.yBackMotor;
    public static final int yForwardMotor = BluetoothActivity.yForwardMotor;
    public static final int zUpMotor = BluetoothActivity.zUpMotor;
    public static final int zDownMotor = BluetoothActivity.zDownMotor;
    public static final int stopMotor = 0;
	
	private static final int firstTouchEvent = -1;
	public TouchSlideControl(Slideable s, Activity activity) {
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
			else if (action == MotionEvent.ACTION_MOVE && touchX != firstTouchEvent && touchY != firstTouchEvent) {
				double x = event.getX() - touchX;
				double y = event.getY() - touchY;
				stage.slide(x, y);
				touchX = event.getX();
				touchY = event.getY();
			}
			else if (action == MotionEvent.ACTION_UP) {
				touchX = touchY = firstTouchEvent;
			}
		}
		else
			touchX = touchY = firstTouchEvent;
		return true;
	}
	
	public static interface Slideable {
		public void slide(double x, double y);
	}

}
