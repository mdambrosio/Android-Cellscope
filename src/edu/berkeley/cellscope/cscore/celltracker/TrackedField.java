package edu.berkeley.cellscope.cscore.celltracker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/*
 * Manages the objects being tracked in the current field of vision.
 */
public class TrackedField {
	private Mat currentField;
	private Mat nextFrame;
	private Mat display;
	private Point center;
	private int radius; //radius squared
	private List<TrackedObject> objects;
	private final Object lockUpdate, lockDisplay;
	
	private ScheduledExecutorService updateThread;

	static final Scalar BLUE = new Scalar(0,0,255);
	static final Scalar GREEN = new Scalar(0, 255, 0);
	static final Scalar RED = new Scalar(255,0,0);
	
	private static final int MINIMUM_UPDATE_INTERVAL = 100; //milliseconds between updates
	
	public TrackedField(Mat img, Point fovCenter, int fovRadius) {
		nextFrame = img;
		currentField = new Mat();
		img.copyTo(currentField);
		center = fovCenter;
		radius = fovRadius;
		display = new Mat(currentField.size(), currentField.type());
		img.copyTo(display);
		Imgproc.cvtColor(currentField, currentField, Imgproc.COLOR_BGR2GRAY);
		objects = new ArrayList<TrackedObject>();
		lockDisplay = new Object();
		lockUpdate = new Object();
	}
	
	public void addObject(Rect region) {
		synchronized(lockDisplay) {
			synchronized(lockUpdate) {
				if (!MathUtils.circleContainsRect(region, center, radius))
					return;
				cropRectToMat(region, currentField);
				objects.add(new TrackedObject(region, currentField));
			}
		}
	}
	
	public void initiateUpdateThread() {
		updateThread = Executors.newSingleThreadScheduledExecutor();
		Runnable updater = new Runnable() {
			public void run() {
				update(nextFrame);
			}
		};
		updateThread.scheduleWithFixedDelay(updater, 0, MINIMUM_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
	}
	
	public void queueFrame(Mat frame) {
		synchronized(lockDisplay) {
			synchronized(lockUpdate) {
				nextFrame = frame;
			}
		}
	}
	public void update(Mat newField) {
		synchronized(lockUpdate) {
			//System.out.println("Beginning update...");
			//newField.copyTo(display);
			newField.copyTo(currentField);
			Imgproc.cvtColor(currentField, currentField, Imgproc.COLOR_BGR2GRAY);
			long time = System.currentTimeMillis();
			long total = 0;
			for (TrackedObject o: objects) {
				o.update(currentField);
				long oldTime = time;
				time = System.currentTimeMillis();
				total += (time - oldTime);
				//System.out.println("\tUpdated one object in " + (time - oldTime) + "; match " + o.tMatch);
			}
			
			resolveConflicts();
			confirmUpdate();
			long oldTime = time;
			time = System.currentTimeMillis();
			total += (time - oldTime);
			//System.out.println("\tResolved conflicts in " + (time - oldTime));
			time = System.currentTimeMillis();
			System.out.println("Update complete in " + total + " with " + objects.size() + " object(s)");
		}
	}
	
	private void resolveConflicts() {
		int size = objects.size();
		for (int i = 0; i < size; i ++) {
			TrackedObject first = objects.get(i);
			if (!first.tracked)
				continue;
			for (int j = i + 1; j < size; j ++) {
				TrackedObject second = objects.get(j);
				if (!second.tracked || !first.tracked)
					continue;
				if (!first.overlapViolation(second))
					continue;
				boolean violation = first.trackingViolation(second);
				if (!violation)
					first.invalidateUpdate();
				else
					second.invalidateUpdate();
			}
		}
	}
	
	private void confirmUpdate() {
		for (TrackedObject o: objects) {
			if (!o.tracked)
				continue;
			if (o.newPosInFov(center, radius))
				o.confirmUpdate();
			else
				o.invalidateUpdate();
		}
	}
	
	//Return an image with object locations and paths overlaid.
	public Mat display() {
		synchronized(lockDisplay) {
			nextFrame.copyTo(display);
			Core.circle(display, center, radius, GREEN);
			for (TrackedObject o: objects) {
				//if (o.roi != null) {
				//	Core.rectangle(display, o.roi.tl(), o.roi.br(), GREEN);
				//}
				o.drawInfo(display);
			}
			return display;
		}
	}
	
	//Reduce the size of a rectangle to fit the matrix.
	//Useful for Mat.submat(), when the rectangle has potential to go out of bounds.
	public static void cropRectToMat(Rect rect, Mat mat) {
		if (rect.x < 0) {
			rect.width += rect.x;
			rect.x = 0;
		}
		if (rect.y < 0) {
			rect.height += rect.y;
			rect.y = 0;
		}
		if (rect.x + rect.width >= mat.cols())
			rect.width = mat.cols() - rect.x - 1;
		if (rect.y + rect.height >= mat.rows())
			rect.height = mat.rows() - rect.y - 1;
	}
}
