package edu.berkeley.cellscope.cscore.cameraui;

public interface ManualExposure extends TouchControllable {
	public int getMinExposure();
	public int getMaxExposure();
	public void adjustExposure(int amount);
}
