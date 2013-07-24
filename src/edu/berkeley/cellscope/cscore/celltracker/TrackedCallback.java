package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Mat;

public interface TrackedCallback {
	public void updateComplete(Mat mat);
}
