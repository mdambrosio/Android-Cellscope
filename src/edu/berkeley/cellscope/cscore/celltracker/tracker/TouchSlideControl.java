package edu.berkeley.cellscope.cscore.celltracker.tracker;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import edu.berkeley.cellscope.cscore.cameraui.TouchControl;

public class TouchSlideControl extends TouchControl {
	private SlideableStage stage;
	private double touchX, touchY;
	
	private static final int firstTouchEvent = -1;
	public TouchSlideControl(SlideableStage s, Activity activity) {
		setEnabled(false);
		stage = s;
		touchX = touchY = firstTouchEvent;
	}
	

	public boolean onTouch(View v, MotionEvent event) {
		if (!enabled)
			return false;
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
				stage.slide(x,  y);
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
}
