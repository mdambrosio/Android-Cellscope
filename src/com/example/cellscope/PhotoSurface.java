package com.example.cellscope;

import android.content.Context;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class PhotoSurface extends SurfaceView {
	CellscopeLauncher activity;
	public PhotoSurface(CellscopeLauncher context) {
		super(context);
		activity = context;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent evt) {
		System.out.println("MOTION EVENT");
		if (evt.getActionMasked() == MotionEvent.ACTION_DOWN) {
			activity.mCamera.takePicture(null, null, activity.mPicture);
			System.out.println("PICTURE TAKEN");
		}
		return true;
	}

}
