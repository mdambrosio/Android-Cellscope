package edu.berkeley.cellscope.cscore.celltracker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
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
	private List<Long> times;
	private long startTime, nextTime;
	private boolean tracking;
	private TrackedCallback callback;
	private final Object lockUpdate, lockDisplay;
	
	private ScheduledExecutorService updateThread;

	private File output;
	private BufferedWriter writer;
	private static final int INITIAL_DELAY = 500;

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
		times = new ArrayList<Long>();
	}
	
	public void addObject(Rect region) {
		synchronized(lockDisplay) {
			synchronized(lockUpdate) {
				if (tracking || !MathUtils.circleContainsRect(region, center, radius))
					return;
				cropRectToMat(region, currentField);
				objects.add(new TrackedObject(region, currentField));
			}
		}
	}
	
	public void initiateUpdateThread(final int interval) {
		updateThread = Executors.newSingleThreadScheduledExecutor();
		Runnable updater = new Runnable() {
			public void run() {
				update(nextFrame);
			}
		};
		updateThread.scheduleWithFixedDelay(updater, INITIAL_DELAY, interval, TimeUnit.MILLISECONDS);
	}
	
	public void haltUpdateThread() {
		updateThread.shutdown();
		updateThread = null;
	}
	
	public List<Rect> getBoundingBoxes() {
		synchronized(lockUpdate) {
			List<Rect> list = new ArrayList<Rect>();
			for (TrackedObject o: objects)
				list.add(o.boundingBox);
			return list;
		}
	}
	
	public void queueFrame(Mat frame) {
		synchronized(lockDisplay) {
			synchronized(lockUpdate) {
				nextFrame = frame;
				nextTime = System.currentTimeMillis();
			}
		}
	}
	public void update(Mat newField) {
		synchronized(lockUpdate) {
			//newField.copyTo(display);
			newField.copyTo(currentField);
			Imgproc.cvtColor(currentField, currentField, Imgproc.COLOR_BGR2GRAY);
			long time = System.currentTimeMillis();
			long total = 0;
			for (TrackedObject o: objects) {;
				o.update(currentField);
				long oldTime = time;
				time = System.currentTimeMillis();
				total += (time - oldTime);
				System.out.println("\tUpdated one object in " + (time - oldTime) + "; match " + o.tMatch);
			}
			
			resolveConflicts();
			confirmUpdate();
			long oldTime = time;
			time = System.currentTimeMillis();
			total += (time - oldTime);
			//System.out.println("\tResolved conflicts in " + (time - oldTime));
			time = System.currentTimeMillis();
			System.out.println("Update complete in " + total + " with " + objects.size() + " object(s)");
			if (tracking) {
				long newtime = nextTime - startTime;
				if (callback != null)
					callback.trackingUpdateComplete(newField);
				if (writer != null && output != null && output.exists()) {
					try {
						writer.append(newtime + ",");
						for (TrackedObject o: objects) {
							if (o.position != null)
								writer.append(o.position.x + "," + o.position.y + ",");
							else
								writer.append("?,?,");
						}
						writer.newLine();
					} catch (IOException e) {
						e.printStackTrace();
						writer = null;
						output = null;
					}
				}
				
			}
		}
	}
	
	private void resolveConflicts() {
		int size = objects.size();
		for (int i = 0; i < size; i ++) {
			TrackedObject first = objects.get(i);
			if (!first.followed)
				continue;
			for (int j = i + 1; j < size; j ++) {
				TrackedObject second = objects.get(j);
				if (!second.followed || !first.followed)
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
			if (!o.followed)
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
			Core.circle(display, center, radius, Colors.GREEN);
			for (TrackedObject o: objects) {
				//if (o.roi != null) {
				//	Core.rectangle(display, o.roi.tl(), o.roi.br(), GREEN);
				//}
				o.drawInfo(display);
			}
			return display;
		}
	}
	
	public void startTracking() {
		synchronized(lockUpdate) {
			tracking = true;
			startTime = System.currentTimeMillis();
			for (TrackedObject o: objects)
				o.setTracking(true);
		}
	}
	
	public void stopTracking() {
		synchronized(lockUpdate) {
			tracking = false;
			for (TrackedObject o: objects)
				o.setTracking(false);
			if (writer != null && output != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				output = null;
				writer = null;
			}
		}
	}
	
	public void reset() {
		synchronized(lockDisplay) {
			synchronized(lockUpdate) {
				times.clear();
				for (TrackedObject o: objects)
					o.reset();
			}
		}
	}
	
	public void setCallback(TrackedCallback c) {
		synchronized(lockUpdate) {
			callback = c;
		}
	}
	
	public void setOutputFile(File f, String title) {
		writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f));
			output = f;
			writer.write(title);
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			writer = null;
			output = null;
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
