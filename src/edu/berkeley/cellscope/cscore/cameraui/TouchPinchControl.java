package edu.berkeley.cellscope.cscore.cameraui;

import android.view.MotionEvent;

/*
 * Touch listener that responds to pinches.
 */

public abstract class TouchPinchControl extends TouchControl {
	protected final double screenDiagonal;
	private double pinchDist; //How far apart were the fingers when they started?
	private double lastAmount;
	
	public TouchPinchControl(int w, int h) {
		super(w, h);
		screenDiagonal = Math.hypot(screenWidth, screenHeight);
		pinchDist = firstTouchEvent;
	}
	
	@Override
	public boolean touch(MotionEvent event) {
		int pointers = event.getPointerCount();
		int action = event.getActionMasked();
		
		if (pointers == 2){
			double newDist = Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
			if (action == MotionEvent.ACTION_MOVE) {
				//Ignore events that are triggered when one finger is put down or lifted while the other is held down
				if (pinchDist != firstTouchEvent) {
					double amount = (newDist-pinchDist) / screenDiagonal * 2;
					boolean result = pinch(amount - lastAmount);
					if (result)
						lastAmount = amount;
				}
				else {
					pinchDist = newDist;
					lastAmount = 0;
				}
			}
			else {
				pinchDist = firstTouchEvent;
			}
		}
		
		return true;
	}
	
	/*
	 * return true when the gesture successfully causes a response
	 */
	public abstract boolean pinch(double amount);
}
