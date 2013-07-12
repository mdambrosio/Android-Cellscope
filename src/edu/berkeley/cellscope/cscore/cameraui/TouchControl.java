package edu.berkeley.cellscope.cscore.cameraui;

import android.view.View;

public abstract class TouchControl implements View.OnTouchListener {
	protected boolean enabled;
	
	public void setEnabled(boolean b) {
		enabled = b;
	}
}
