package edu.berkeley.cellscope.cscore.cameraui;

import android.view.MotionEvent;
import android.view.View;

public class TouchZoomControl extends TouchControl {
	private ZoomablePreview stage;
	private double maxZoom, screenDiagonal, pinchDist;
	private int lastZoom;
	
	private static final int firstTouchEvent = -1;
	public TouchZoomControl(ZoomablePreview p) {
		stage = p;
		setEnabled(false);
		pinchDist = maxZoom = firstTouchEvent;
		screenDiagonal = stage.getDiagonal();
	}
	public boolean onTouch(View v, MotionEvent event) {
		if (!enabled)
			return false;
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();
		
		if (maxZoom == firstTouchEvent)
			maxZoom = stage.getMaxZoom();
		//Pinch zoom
		if (pointers == 2){
			double newDist = Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
			if (action == MotionEvent.ACTION_MOVE) {
				if (pinchDist != firstTouchEvent) { //Prevents jumping
					int newZoom = (int)((newDist-pinchDist) / screenDiagonal * maxZoom * 2);
					stage.zoom(newZoom - lastZoom);
					lastZoom = newZoom;
				}
				else {
					pinchDist = newDist;
					lastZoom = 0;
				}
			}
			else {
				pinchDist = firstTouchEvent;
			}
		}
		
		return true;
	}
}
