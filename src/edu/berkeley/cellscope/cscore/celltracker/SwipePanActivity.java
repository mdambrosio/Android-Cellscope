package edu.berkeley.cellscope.cscore.celltracker;

import android.os.Bundle;
import android.os.Message;
import edu.berkeley.cellscope.cscore.cameraui.TouchPanControl;
import edu.berkeley.cellscope.cscore.cameraui.TouchSwipeControl;

/*
 * Class for testing the stepper counter.
 * 
 * Observations: commands do not queue. Sending multiple commands at once
 * to the stage will cause the first command to be executed, but all commands' steps
 * to count down. When the first command is done executing (i.e. no more steps remaining), 
 * the next command will be executed if it has remaining steps.
 */
public class SwipePanActivity extends OpenCVCameraActivity implements TouchSwipeControl.Swipeable {
	protected TouchSwipeControl touchSwipe;
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		touchPan.setEnabled(false);
		touchSwipe = new TouchSwipeControl(this, this);
		touchSwipe.setEnabled(true);
		compoundTouch.addTouchListener(touchSwipe);
	}
	
	public void swipe(int dir, int dist) {
		System.out.println("swipe " + dir + " " + dist);
		byte[] buffer = new byte[1];
		buffer[0] = (byte)dir;
		btConnector.write(buffer);
		byte[] buffer2 = new byte[1];
		buffer2[0] = (byte)dist;
		btConnector.write(buffer2);
	}
	
	@Override
	public void readMessage(Message msg) {
		super.readMessage(msg);
		byte[] buffer = (byte[])(msg.obj);
		if (buffer.length > 0 && (int)buffer[0] == TouchPanControl.stopMotor)
			System.out.println("motor stopped");
	}
	
	public boolean swipeAvailable() {
		return panAvailable();
	}
}
