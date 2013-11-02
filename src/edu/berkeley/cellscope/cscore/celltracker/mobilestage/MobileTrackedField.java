package edu.berkeley.cellscope.cscore.celltracker.mobilestage;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import edu.berkeley.cellscope.cscore.celltracker.MathUtils;
import edu.berkeley.cellscope.cscore.celltracker.RealtimeImageProcessor;

/**
 * Track objects across several fovs.
 * The travelling-salesman problem is solved using a greedy algorithm.
 * 1. 	Image of starting fov and initial set of objects
 * 2. 	Move to the closest unchecked object
 * 3. 	Record new fov
 * 4. 	Calculate new fov's offset from old fov (current object's fov)
 * 5. 	Calculate the expected positions of the old fov's objects
 * 		in the new fov
 * 6.	Ignore objects that are not expected to be found in the new fov.
 * 		Ignore objects that are expected to be close to the edge of the new fov,
 * 			in case the object has moved offscren.
 * 		Ignore objects that have already been found.
 * 7. 	Of remaining objects, locate their positions in the new fov.
 * 		Marked these objects as found.
 * 8.	Move to the next closest unfound object and repeat steps 3-8.
 * 9.	When all objects are found, mark all objects as not found and begin again
 * 			at step 2.
 */
public class MobileTrackedField {
	private List<MobileObj> objects;	//objects currently being tracked
	private List<MobileObj> found, notFound; //lists objects that are found and not found
	private List<MobileObj> toProcess;	//list of objects to process
	private boolean active; 			//true if actively tracking objecsts
	private MobileFov currentFov;
	private Point fovCenter;
	private double radius;
	
	private static final double REQUIRED_DISTANCE = 0.7;
	
	public MobileTrackedField() {
		objects = new ArrayList<MobileObj>(); 
		found = new ArrayList<MobileObj>();
		notFound = new ArrayList<MobileObj>();
		toProcess = new ArrayList<MobileObj>();
		active = false;
	}
	
	public void setStartingFov(Mat img, Point center, long time) {
		Point start = new Point(0, 0);
		currentFov = new MobileFov(center, radius, img, start, time);
	}
	
	public void addStartingObject(Rect roi) {
		if (!active && currentFov != null) {
			MobileObj obj = new MobileObj(roi, currentFov);
			objects.add(obj);
			notFound.add(obj);
		}
	}
	
	public void addStartingObject(List<Rect> roi) {
		for (Rect r: roi) {
			addStartingObject(r);
		}
	}
	
	/**
	 * Travelling salesman problem: given the position of the current fov,
	 * calculate the position of the next object to move to.
	 * Greedy: finds the closest object not yet found to move to.
	 */
	private Point nextTargetLocation() {
		//Switch the found and notFound lists to reset if
		//all objects have already been found.
		if (notFound.isEmpty()) {
			List<MobileObj> tmp = notFound;
			notFound = found;
			found = tmp;
		}
		
		Point currentLoc = currentFov.getAbsoluteLocation();
		double bestDistSqr = -1;
		Point bestTargetLoc = null;
		for (MobileObj obj: objects) {
			Point objLoc = obj.getAbsoluteLocation();
			double distSqr = MathUtils.distSqr(currentLoc, objLoc);
			if (bestDistSqr == -1 || distSqr < bestDistSqr) {
				bestTargetLoc = objLoc;
				bestDistSqr = distSqr;
			}
		}
		return bestTargetLoc;
	}
	
	public List<MobileObj> expectedObjects(Point location) {
		toProcess.clear();
		Point tmp = new Point();
		for (MobileObj obj: objects) {
			if (!obj.currentFov(currentFov))
				continue;
			Point objPos = obj.getRelativeLocation();
			Point offset = currentFov.getOffset(location);
//			MathUtils.set(tmp, position);
//			MathUtils.add(tmp, )
//			if (currentFov.pointWithinBounds(position, REQUIRED_DISTANCE))
//				toProcess.add(obj);
		}
		return toProcess;
	}
/*
	public void processFrame(Mat mat) {
	}

	public boolean isRunning() {
	}

	public void start() {
	}

	public void stop() {
	}

	public void displayFrame(Mat mat) {
	}

	public void continueRunning() {
	}*/
}
