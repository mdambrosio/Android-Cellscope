package edu.berkeley.cellscope.cscore.cameraui;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import edu.berkeley.cellscope.cscore.CameraActivity;

public class TouchPanControl extends TouchControl {
	private PannableStage stage;
	private double touchX, touchY;
	private double zZone;
	private int panState;
	
	private static final int firstTouchEvent = -1;
	public TouchPanControl(PannableStage p, Activity activity) {
		setEnabled(false);
		stage = p;
		zZone = CameraActivity.getScreenWidth(activity) * PannableStage.Z_CONTROL_ZONE;
	}
	

	public boolean onTouch(View v, MotionEvent event) {
		if (!enabled)
			return false;
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();
		int newState = PannableStage.stopMotor;
	
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
				if (absX >= absY && absX >= PannableStage.PAN_THRESHOLD) {
					newState = x > 0 ? PannableStage.yForwardMotor : PannableStage.yBackMotor;
					//newState = x > 0 ? PannableStage.xRightMotor : PannableStage.xLeftMotor;
				}
				else if (absY > absX && absY > PannableStage.PAN_THRESHOLD) {
					if (touchX < zZone)
						newState = y > 0 ? PannableStage.zUpMotor : PannableStage.zDownMotor;
					else
						newState = y < 0 ? PannableStage.xLeftMotor : PannableStage.xRightMotor;
						//newState = y > 0 ? PannableStage.yForwardMotor : PannableStage.yBackMotor;
				}
			}
			else if (action == MotionEvent.ACTION_UP) {
				newState = PannableStage.stopMotor;
				touchX = touchY = firstTouchEvent;
			}
		}
		
		if (newState != panState) {
			panState = newState;
			stage.panStage(newState);
		}
		
		return true;
	}
	
	public int getState() {
		return panState;
	}
	
}
