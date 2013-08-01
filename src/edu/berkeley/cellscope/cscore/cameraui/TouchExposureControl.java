package edu.berkeley.cellscope.cscore.cameraui;

import android.app.Activity;


public class TouchExposureControl extends TouchPinchControl {
	private ManualExposure screen;
	private int maxExposure, minExposure;
	
	public TouchExposureControl(ManualExposure m, Activity activity) {
		super(activity);
		screen = m;
	}
	
	@Override
	public boolean pinch(double amount) {
		if (maxExposure == 0 && minExposure == 0) {
			maxExposure = screen.getMaxExposure();
			minExposure = screen.getMinExposure();
		}
		int exposure = (int)(amount * (maxExposure - minExposure));
		if (exposure == 0)
			return false;
		screen.adjustExposure(exposure);
		return true;
	}
}
