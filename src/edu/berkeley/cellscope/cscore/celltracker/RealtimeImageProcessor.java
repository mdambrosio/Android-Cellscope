package edu.berkeley.cellscope.cscore.celltracker;

import org.opencv.core.Mat;

/*
 * General interface for anything that does real-time image processing on the camera preview.
 */
public interface RealtimeImageProcessor {
	public boolean isRunning();
	public void start();
	public void stop();
	public void processFrame(Mat mat);
}
