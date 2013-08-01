package edu.berkeley.cellscope.cscore.cameraui;

public interface ZoomablePreview extends TouchControllable {

	public int getMaxZoom();
	public void zoom(int amount);
}
