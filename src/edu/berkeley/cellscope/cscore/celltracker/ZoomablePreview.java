package edu.berkeley.cellscope.cscore.celltracker;

public interface ZoomablePreview {

	public double getDiagonal();
	public double getMaxZoom();
	public void zoom(int amount);
}
