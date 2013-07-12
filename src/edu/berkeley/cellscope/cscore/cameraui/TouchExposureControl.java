package edu.berkeley.cellscope.cscore.cameraui;

import android.view.MotionEvent;
import android.view.View;


public class TouchExposureControl extends TouchControl {
	private ManualExposure screen;
	private double screenDiagonal, pinchDist;
	private int minExposure, maxExposure, currentExposure;

	private static final int firstTouchEvent = -1;
	public TouchExposureControl(ManualExposure m) {
		screen = m;
		setEnabled(false);
		currentExposure = 0;
		screenDiagonal = m.getDiagonal();
		minExposure = maxExposure = firstTouchEvent;
		pinchDist = firstTouchEvent;
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		if (!enabled)
			return false;
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();
		
		if (minExposure == firstTouchEvent) {
			minExposure = screen.getMinExposure();
			maxExposure = screen.getMaxExposure();
		}
		
		if (pointers == 2){
			double newDist = Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
			if (action == MotionEvent.ACTION_MOVE) {
				if (pinchDist != firstTouchEvent) { //Prevents jumping
					int newExposure = (int)((newDist-pinchDist) / screenDiagonal * (maxExposure - minExposure) * 2);
					screen.adjustExposure(newExposure - currentExposure);
					currentExposure = newExposure;
				}
				else {
					pinchDist = newDist;
					currentExposure = 0;
				}
			}
			else {
				pinchDist = firstTouchEvent;
			}
		}
		
		return true;
	}
}
