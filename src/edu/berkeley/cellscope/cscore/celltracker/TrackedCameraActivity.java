package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import edu.berkeley.cellscope.cscore.ScreenDimension;

public class TrackedCameraActivity extends OpenCVCameraActivity implements View.OnTouchListener {
	
	private TrackedField field;
	private int screenWidth, screenHeight;
	
	static final int TEST_WIDTH = 50;
	static final int TEST_HEIGHT = 50;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		compoundTouch.addTouchListener(this);
		screenWidth = ScreenDimension.getScreenWidth(this);
		screenHeight = ScreenDimension.getScreenHeight(this);
	}
	
	@Override
	public void initialFrame() {
		super.initialFrame();
		field = new TrackedField(mRgba, new Point(cameraView.width / 2, cameraView.height / 2), cameraView.height / 2);
		field.initiateUpdateThread(250);
	}
	
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		super.onCameraFrame(inputFrame);
		field.queueFrame(mRgba);
		return field.display();
	}

	public boolean onTouch(View view, MotionEvent evt) {
		if (field == null)
			return false;
		if (evt.getActionMasked() == MotionEvent.ACTION_DOWN) {
			//Convert touch coordinates to screen coordinates
			float x = evt.getX() / screenWidth;
			float y = evt.getY() / screenHeight;
			Point p = new Point(cameraView.width * x, cameraView.height * y);
			Size s = new Size(TEST_WIDTH, TEST_HEIGHT);
			field.stopTracking();
			field.addObject(MathUtils.createCenteredRect(p, s));
			field.startTracking();
		}
		return true;
	}
}
