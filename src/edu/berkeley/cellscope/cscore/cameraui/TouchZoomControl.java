package edu.berkeley.cellscope.cscore.cameraui;

import android.app.Activity;

public class TouchZoomControl extends TouchPinchControl {
	private ZoomablePreview screen;
	private int maxZoom;
	public TouchZoomControl(ZoomablePreview p, Activity activity) {
		super(activity);
		screen = p;
	}
	
	@Override
	public boolean pinch(double amount) {
		if (maxZoom == 0)
			maxZoom = screen.getMaxZoom();
		int zoom = (int)(amount * maxZoom);
		if (zoom == 0)
			return false;
		screen.zoom(zoom);
		return true;
	}
}
