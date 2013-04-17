package edu.berkeley.cellscope.cscore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ZoomableImageView extends View{
	Drawable img;
	double zoom;
	double pinchDist;
	double aspectRatio;
	double panX, panY;
	double touchX, touchY;
	private static final double firstTouchEvent = -1000000;
	private static final double pinchSensitivity = 0.01;
	private static final double maxZoom = 10;
	private static final double minZoom = 1;
	
	View.OnTouchListener touchListener = new View.OnTouchListener() {
		
		public boolean onTouch(View v, MotionEvent event) {
			double pointers = event.getPointerCount();
			if (pointers == 2){
				if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
					double newDist = Math.hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1));
					if (pinchDist != firstTouchEvent) {
						zoom += (newDist-pinchDist) * pinchSensitivity;
						if (zoom < minZoom)
							zoom = minZoom;
						else if (zoom > maxZoom)
							zoom = maxZoom;
					}
					pinchDist = newDist;
				}
				else {
					pinchDist = firstTouchEvent;
					touchX = touchY = firstTouchEvent;
				}
			}
			else if (pointers == 1) {
				double x = event.getX();
				double y = event.getY();
				if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
					if (touchX != firstTouchEvent) {
						panX += (x - touchX);
						panY += (y - touchY);
					}
					touchX = x;
					touchY = y;
				}
					
				else {
					touchX = touchY = firstTouchEvent;
				}
			}
			invalidate(); //redraw the image
			return true;
		}
	};

	public ZoomableImageView(Context context) {
		super(context);
		setFocusable(true);
		init();
	}
	
	 public ZoomableImageView(Context context, AttributeSet attrs) {
		 super(context, attrs);
		 setFocusable(true);
		 init();
	 }
	 
	 public ZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
		 super(context, attrs, defStyle);
		 setFocusable(true);
		 init();
	 }
	 
	 private void init() {
		 zoom = minZoom;
		 pinchDist = firstTouchEvent;
		 setOnTouchListener(touchListener);
	 }
	 
	 @Override
	 protected void onDraw(Canvas canvas) {
		 super.onDraw(canvas);
		 if (img != null) {
			 double screenWidth = getWidth();
			 double screenHeight = getHeight();
			 double baseWidth, baseHeight;
			 if (screenWidth / screenHeight >= aspectRatio) {
				 baseHeight = screenHeight;
				 baseWidth = baseHeight * aspectRatio;
			 }
			 else {
				 baseWidth = screenWidth;
				 baseHeight = baseWidth / aspectRatio;
			 }
			 baseWidth *= zoom;
			 baseHeight *= zoom;
			 int left = (int)(screenWidth / 2 - baseWidth / 2 + panX);
			 int top = (int)(screenHeight / 2 - baseHeight / 2 + panY);
			 int right = (int)(screenWidth / 2 + baseWidth / 2 + panX);
			 int bottom = (int)(screenHeight / 2 + baseHeight / 2 + panY);
			 img.setBounds(left, top, right, bottom);
			 img.draw(canvas);
		 }
	 }
	 
	 public void setImage(String path) {
		 img = Drawable.createFromPath(path);
		 aspectRatio = (double)img.getIntrinsicWidth() / img.getIntrinsicHeight();
	 }
	 
}
