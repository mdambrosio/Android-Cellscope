package edu.berkeley.cellscope.cscore.cameraui;

import edu.berkeley.cellscope.cscore.BluetoothActivity;
import android.app.Activity;
import android.view.MotionEvent;

/*
 * Touch listener that responds to single-finger gestures.
 * 
 * Sliding a finger in one direction will cause the stage to move in that direction. The panning
 * will not stop until the finger is released.
 */

public class TouchPanControl extends TouchControl {
	private PannableStage stage;
	private double touchX, touchY;
	private double zZone;
	private int panState;
	
    public static final int xRightMotor = BluetoothActivity.xRightMotor;
    public static final int xLeftMotor = BluetoothActivity.xLeftMotor;
    public static final int yBackMotor = BluetoothActivity.yBackMotor;
    public static final int yForwardMotor = BluetoothActivity.yForwardMotor;
    public static final int zUpMotor = BluetoothActivity.zUpMotor;
    public static final int zDownMotor = BluetoothActivity.zDownMotor;
    public static final int stopMotor = 0;
	
	
	private static final double PAN_THRESHOLD = 50; //Gestures smaller than this are ignored.
	private static final double Z_CONTROL_ZONE = 0.3; //Gestures left of this part of the screen are used to control Z
	
	public TouchPanControl(PannableStage p, Activity activity) {
		super(activity);
		stage = p;
		zZone = screenWidth * Z_CONTROL_ZONE;
	}
	
	@Override
	protected boolean touch(MotionEvent event) {
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();
		int newState = stopMotor;
	
		if (stage.panAvailable() && pointers == 1) {
			if (action == MotionEvent.ACTION_DOWN) {
				touchX = event.getX();
				touchY = event.getY();
			}
			else if (action == MotionEvent.ACTION_MOVE) {
				double x = event.getX() - touchX;
				double y = event.getY() - touchY;
				double absX = Math.abs(x);
				double absY = Math.abs(y);
				//Pan in the direction that the the gesture has moved more in.
				if (absX >= absY && absX > PAN_THRESHOLD) {
					newState = x > 0 ? yForwardMotor : yBackMotor;
					//newState = x > 0 ? PannableStage.xRightMotor : PannableStage.xLeftMotor;
				}
				else if (absY > absX && absY > PAN_THRESHOLD) {
					if (touchX < zZone)
						newState = y > 0 ? zUpMotor : zDownMotor;
					else
						newState = y < 0 ? xLeftMotor : xRightMotor;
						//newState = y > 0 ? PannableStage.yForwardMotor : PannableStage.yBackMotor;
				}
			}
			else if (action == MotionEvent.ACTION_UP) {
				newState = stopMotor;
			}
		}
		
		if (newState != panState) {
			panState = newState;
			stage.panStage(newState);
		}
		
		return true;
	}

	public static interface PannableStage extends TouchControllable {
		
		public void panStage(int newStage);
		public boolean panAvailable();
	}
}
