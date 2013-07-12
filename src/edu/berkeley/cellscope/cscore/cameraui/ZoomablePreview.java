package edu.berkeley.cellscope.cscore.cameraui;

public interface ZoomablePreview extends TouchControllable {

	public double getMaxZoom();
	public void zoom(int amount);
}
