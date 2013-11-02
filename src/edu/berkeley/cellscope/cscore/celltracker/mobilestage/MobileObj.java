package edu.berkeley.cellscope.cscore.celltracker.mobilestage;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import edu.berkeley.cellscope.cscore.celltracker.MathUtils;

/**
 * Data for an object being tracked using the movable stage.
 *
 */
public class MobileObj {
	private List<PathData> path; //absolute position of the object
	private Size size;			//width/height dimensions
	private Point relLocation;		//locatin fo the center within the current fov
	private Point absLocation;		//absolute location
	private boolean absLocKnown; //true if location has been calculated and hasn't changed
	private MobileFov fov; 		//current fov that the object was seen in
	private Mat image;			//last-updated image of the object
	
	public MobileObj(Rect region, MobileFov field) {
		path = new ArrayList<PathData>();
		size = region.size();
		relLocation = region.tl();
		absLocation = new Point();
		fov = field;
		image = fov.getRegion(region);
		path.add(new PathData(getAbsoluteLocation(), 0));
		
	}
	
	public Point getAbsoluteLocation() {
		if (absLocKnown) {
			return absLocation;
		}
		Point fovLoc = fov.getAbsoluteLocation();
		MathUtils.set(absLocation, relLocation);
		MathUtils.add(absLocation, fovLoc);
		absLocKnown = true;
		return absLocation;
	}
	
	public Point getRelativeLocation() {
		return relLocation;
	}
	
	public boolean currentFov(MobileFov fov) {
		return this.fov == fov;
	}
	
//	public boolean update(MobileFov newFov, double frac) {
//		
//	}
}
