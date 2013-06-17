package edu.berkeley.cellscope.cscore.celltracker;

import edu.berkeley.cellscope.cscore.BluetoothActivity;

public interface PannableStage {
    static final int xRightMotor = BluetoothActivity.xRightMotor;
    static final int xLeftMotor = BluetoothActivity.xLeftMotor;
    static final int yBackMotor = BluetoothActivity.yBackMotor;
    static final int yForwardMotor = BluetoothActivity.yForwardMotor;
    static final int zUpMotor = BluetoothActivity.zUpMotor;
    static final int zDownMotor = BluetoothActivity.zDownMotor;
    static final int stopMotor = 0;
	static final double PAN_THRESHOLD = 25;
	static final double Z_CONTROL_ZONE = 0.2;
	
	
	public void panStage(int newStage);
	public boolean panAvailable();
	public double getDiagonal();
	public double getMaxZoom();
	public void zoom(int amount);
}
