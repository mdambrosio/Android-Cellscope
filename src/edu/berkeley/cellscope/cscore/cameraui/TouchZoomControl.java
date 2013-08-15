package edu.berkeley.cellscope.cscore.cameraui;

import android.app.Activity;

public class TouchZoomControl extends TouchPinchControl {
	private Zoomable screen;
	private int maxZoom;
	public TouchZoomControl(Zoomable p, int w, int h) {
		super(w, h);
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
	
	public static interface Zoomable extends TouchControllable {

		public int getMaxZoom();
		public void zoom(int amount);
	}

}
