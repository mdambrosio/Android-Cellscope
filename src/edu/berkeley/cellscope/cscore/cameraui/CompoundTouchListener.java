package edu.berkeley.cellscope.cscore.cameraui;

import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;
import android.view.View;

//Permits usage of multiple touch listeners
public class CompoundTouchListener implements View.OnTouchListener {
	private List<View.OnTouchListener> listeners;
	
	public CompoundTouchListener() {
		listeners = new ArrayList<View.OnTouchListener>();
	}

	public boolean onTouch(View v, MotionEvent event) {
		for (View.OnTouchListener lis: listeners)
			lis.onTouch(v, event);
		return true;
	}
	
	public void addTouchListener(View.OnTouchListener lis) {
		if (!listeners.contains(lis))
			listeners.add(lis);
	}
}
