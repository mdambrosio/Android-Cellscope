package edu.berkeley.cellscope.cscore.cameraui;

import android.view.MotionEvent;

/*
 * Touch listener that responds to single-finger gestures.
 * 
 * Sliding a finger in one direction will cause the stage to move in that direction. The panning
 * will not stop until the finger is released.
 */

public class TouchPanControl extends TouchControl {
	private BluetoothControllable stage;
	private double touchX, touchY;
	private double zZone;
	private int panState;
	
	
	private static final double PAN_THRESHOLD = 50; //Gestures smaller than this are ignored.
	private static final double Z_CONTROL_ZONE = 0.3; //Gestures left of this part of the screen are used to control Z
	
	public TouchPanControl(BluetoothControllable p, int w, int h) {
		super(w, h);
		stage = p;
		zZone = screenWidth * Z_CONTROL_ZONE;
	}
	
	@Override
	protected boolean touch(MotionEvent event) {
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();
		int newState = stopMotor;
	
		if (stage.controlReady() && pointers == 1) {
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
					newState = x > 0 ? xPositive : xNegative;
					//newState = x > 0 ? PannableStage.xRightMotor : PannableStage.xLeftMotor;
				}
				else if (absY > absX && absY > PAN_THRESHOLD) {
					if (touchX < zZone)
						newState = y > 0 ? zPositive : zNegative;
					else
						newState = y > 0 ? yPositive : yNegative;
						//newState = y > 0 ? PannableStage.yForwardMotor : PannableStage.yBackMotor;
				}
			}
			else if (action == MotionEvent.ACTION_UP) {
				newState = stopMotor;
			}
		}
		
		if (newState != panState) {
			panState = newState;
			panStage(newState);
		}
		
		return true;
	}


	public void panStage(int newState) {
		System.out.println("pan " + newState);
    	BluetoothConnector bt = stage.getBluetooth();
		byte[] buffer = new byte[1];
    	buffer[0] = (byte)newState;
    	bt.write(buffer);
		byte[] buffer2 = new byte[1];
		buffer2[0] = (byte)0;
		bt.write(buffer2);
	}
}
