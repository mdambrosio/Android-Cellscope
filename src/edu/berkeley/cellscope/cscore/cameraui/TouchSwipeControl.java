package edu.berkeley.cellscope.cscore.cameraui;

import android.app.Activity;
import android.view.MotionEvent;

/*
 * Touch listener that responds to single-finger gestures.
 */

public class TouchSwipeControl extends TouchControl {
	private Swipeable stage;
	private double touchX, touchY;
	
	private static final double SENSITIVITY = 0.1;
	
	public TouchSwipeControl(Swipeable s, Activity activity) {
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
		if (!stage.swipeAvailable())
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
		stage.swipe(dir, dist);
	}
	

	public static interface Swipeable {
		public void swipe(int dir, int dist);
		public boolean swipeAvailable();
	}

}
