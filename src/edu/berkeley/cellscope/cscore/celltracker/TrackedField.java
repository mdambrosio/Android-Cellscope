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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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

	private File file;
	private String header;
	private List<String> output;
	private static final int INITIAL_DELAY = 500;
	private static final long NULL_TIME = 0;

	public TrackedField(Mat img, Point fovCenter, int fovRadius) {
		nextFrame = img;
		currentField = new Mat();
		img.copyTo(currentField);
		center = fovCenter;
		radius = fovRadius;
		display = new Mat(currentField.size(), currentField.type());
		img.copyTo(display);
		processImage(currentField);
		objects = new ArrayList<TrackedObject>();
		lockDisplay = new Object();
		lockUpdate = new Object();
		times = new ArrayList<Long>();
		startTime = NULL_TIME;
		output = new ArrayList<String>();
	}
	
	private static void processImage(Mat mat) {
		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
		Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX, -1);
		Scalar sum = Core.sumElems(mat);
		int count = Core.countNonZero(mat);
		double ave = sum.val[0] / count;
		Core.subtract(mat, new Scalar(ave), mat);
		Imgproc.equalizeHist(mat, mat);
		Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX, -1);
		sum = Core.sumElems(mat);
		count = Core.countNonZero(mat);
		ave = sum.val[0] / count;
		Core.subtract(mat, new Scalar(ave), mat);
		Core.normalize(mat, mat, 0, 255, Core.NORM_MINMAX, -1);
		Imgproc.equalizeHist(mat, mat);
	}
	
	public void addObject(Rect region) {
		synchronized(lockDisplay) {
			synchronized(lockUpdate) {
				if (tracking || !MathUtils.circleContainsRect(region, center, radius))
					return;
				cropRectToMat(region, currentField);
				TrackedObject object = new TrackedObject(region, currentField);
				objects.add(object);
				if (!times.isEmpty())
					object.addNullPath(times.size());
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
		if (updateThread != null)
			updateThread.shutdown();
		updateThread = null;
	}
	
	public List<Rect> getBoundingBoxes() {
		synchronized(lockUpdate) {
			List<Rect> list = new ArrayList<Rect>();
			for (TrackedObject o: objects)
				if (!o.isDisabled())
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
			processImage(currentField);
			long time = System.currentTimeMillis();
			long total = 0;
			for (TrackedObject o: objects) {
				if (!o.isDisabled()) {
					o.update(currentField);
					long oldTime = time;
					time = System.currentTimeMillis();
					total += (time - oldTime);
				//	System.out.println("\tUpdated one object in " + (time - oldTime) + "; match " + o.tMatch);
				}
			}
			resolveIssues();
			confirmUpdate();
			long oldTime = time;
			time = System.currentTimeMillis();
			total += (time - oldTime);
			//System.out.println("\tResolved conflicts in " + (time - oldTime));
			time = System.currentTimeMillis();
			//System.out.println("Update complete in " + total + " with " + objects.size() + " object(s)");
			if (tracking) {
				long newtime = nextTime - startTime;
				if (callback != null)
					callback.trackingUpdateComplete(newField);
				
				if (file != null) {
					String write = newtime + ",";
					for (TrackedObject o: objects) {
						if (o.lastPathPoint() != null)
							write += (o.lastPathPoint().x + "," + o.lastPathPoint().y + ",");
						else
							write += ("?,?,");
					}
					output.add(write);
				}
				
			}
		}
	}
	
	private void resolveIssues() {
		int size = objects.size();
		for (int i = 0; i < size; i ++) {
			TrackedObject first = objects.get(i);
			if (first.isDisabled())
				continue;
			if (!first.newPosInFov(center, radius)) {
				first.invalidateUpdate();
			}
			if (!first.followed())
				continue;
			for (int j = i + 1; j < size; j ++) {
				TrackedObject second = objects.get(j);
				if (second.isDisabled())
					continue;
				if (!second.followed() || !first.followed())
					continue;
				if (!first.overlapViolation(second))
					continue;
				boolean violation = first.trackingViolation(second);
				if (!violation) {
					System.out.println("invalidating update: overlap failure");
					first.invalidateUpdate();
				}
				else {
					System.out.println("invalidating update: overlap failure");
					second.invalidateUpdate();
				}
			}
		}
	}
	
	private void confirmUpdate() {
		for (TrackedObject o: objects) {
			if (o.followed() && !o.isDisabled())
				o.confirmUpdate();
		}
	}
	
	//Return an image with object locations and paths overlaid.
	public Mat display() {
		synchronized(lockDisplay) {
			nextFrame.copyTo(display);
			Core.circle(display, center, radius, Colors.WHITE);
			for (TrackedObject o: objects) {
				//if (o.roi != null) {
				//	Core.rectangle(display, o.roi.tl(), o.roi.br(), GREEN);
				//}
				if (!o.isDisabled())
					o.drawInfo(display);
			}
			return display;
		}
	}
	
	public void startTracking() {
		synchronized(lockUpdate) {
			if (tracking)
				return;
			tracking = true;
			if (startTime == NULL_TIME)
				startTime = System.currentTimeMillis();
			for (TrackedObject o: objects)
				if (!o.isDisabled())
					o.setTracking(true);
		}
	}
	
	public void stopTracking() {
		synchronized(lockUpdate) {
			if (!tracking)
				return;
			tracking = false;
			for (TrackedObject o: objects)
				o.setTracking(false);
			if (file != null) {
				dumpOutputToFile();
			}
		}
	}
	
	public boolean isTracking() {
		synchronized(lockUpdate) {
			return tracking;
		}
	}
	public void dumpOutputToFile() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(header);
			writer.newLine();
			writer.newLine();
			String tableColumns = "time,";
			int size = objects.size();
			for (int i = 0; i < size; i ++)
				tableColumns += "x" + i + ",y" + i + ",";
			writer.write(tableColumns);
			writer.newLine();
			for (String s: output) {
				writer.write(s);
				writer.newLine();
			}
			writer.newLine();
			writer.write("id,width,height");
			writer.newLine();
			for (int i = 0; i < size; i ++) {
				Size dim = objects.get(i).size;
				writer.write(i +"," + dim.width + "," + dim.height+",");
				writer.newLine();
			}
			writer.newLine();
			writer.write("fov center," + center.x + "," + center.y + ",");
			writer.newLine();
			writer.write("fov radius," + radius + ",");
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void resetData() {
		synchronized(lockDisplay) {
			synchronized(lockUpdate) {
				startTime = NULL_TIME;
				times.clear();
				output.clear();
				objects.clear();
				haltUpdateThread();
			}
		}
	}
	
	public void setCallback(TrackedCallback c) {
		synchronized(lockUpdate) {
			callback = c;
		}
	}
	
	public void setOutputFile(File f, String title) {
		file = f;
		header = title;
	}
	
	public Rect selectObject(Point point) {
		synchronized(lockDisplay) {
			synchronized(lockUpdate) {
				int size = objects.size();
				for (int i = 0; i < size; i ++) {
					TrackedObject obj = objects.get(i);
					if (obj.boundingBox.contains(point)) {
						obj.disable();
						return obj.boundingBox;
					}
				}
				return null;
			}
		}
	}
	
	//Reduce the size of a rectangle to fit the matrix.
	//Useful for Mat.submat(), when the rectangle has potential to go out of bounds.
	public static void cropRectToMat(Rect rect, Mat mat) {
		MathUtils.cropRectToRegion(rect, mat.width(), mat.height());
	}
}
