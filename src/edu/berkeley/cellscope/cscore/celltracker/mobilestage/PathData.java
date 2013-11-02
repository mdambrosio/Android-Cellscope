package edu.berkeley.cellscope.cscore.celltracker.mobilestage;

import org.opencv.core.Point;

/**
 * Holds location and time data of a single point on the line
 */
public class PathData {
	public Point loc;
	public long time;	//System time, in millis
	
	public PathData(Point p, long t) {
		loc = new Point(p.x, p.y);
		time = t;
	}
	
	public PathData(Point p) {
		this(p, System.currentTimeMillis());
	}
	
}
