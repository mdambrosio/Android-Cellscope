package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;

public class ScreenDimension {
	public static double getScreenDiagonal(Activity activity) {
		int width, height;
		Display display = activity.getWindowManager().getDefaultDisplay();
		if (Build.VERSION.SDK_INT < 13) {
			width = display.getWidth();
			height = display.getHeight();
		}
		else {
		    Point size = new Point();
		    display.getSize(size);
		    width = size.x;
		    height = size.y;	
		}
	    return Math.hypot(width, height);
	}
	
	public static int getScreenWidth(Activity activity) {
		Display display = activity.getWindowManager().getDefaultDisplay();
		if (Build.VERSION.SDK_INT < 13) {
			return display.getWidth();
		}
		else {
		    Point size = new Point();
		    display.getSize(size);
		    return size.x;	
		}
	}
	
	public static int getScreenHeight(Activity activity) {
		Display display = activity.getWindowManager().getDefaultDisplay();
		if (Build.VERSION.SDK_INT < 13) {
			return display.getHeight();
		}
		else {
		    Point size = new Point();
		    display.getSize(size);
		    return size.y;	
		}
	}
}
