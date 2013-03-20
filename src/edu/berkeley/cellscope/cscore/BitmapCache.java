package edu.berkeley.cellscope.cscore;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

public class BitmapCache {
	private volatile ArrayList<Integer> positions;
	private volatile ArrayList<Bitmap> bitmaps;
	int maxCacheSize;
	int cacheSize;
	public BitmapCache(Context context) {
		positions = new ArrayList<Integer>(0);
		bitmaps = new ArrayList<Bitmap>(0);
		// Get memory class of this device, exceeding this amount will throw an
	    // OutOfMemory exception.
	    final int memClass = ((ActivityManager) context.getSystemService(
	            Context.ACTIVITY_SERVICE)).getMemoryClass();

	    // Use 1/8th of the available memory for this memory cache.
	    maxCacheSize = 1024 * 1024 * memClass / 8;

	}
	
	public void addBitmap(int position, Bitmap bitmap) {
		int byteCount = getBitmapByteCount(bitmap);
		if (cacheSize + byteCount >= maxCacheSize && !positions.isEmpty() && !bitmaps.isEmpty()) {
			positions.remove(0);
			Bitmap removed = bitmaps.remove(0);
			cacheSize += byteCount;
			cacheSize -= getBitmapByteCount(removed);
			//removed.recycle();
			//System.out.println("cache removed");
		}
		positions.add(position);
		bitmaps.add(bitmap);
	}
	
	public Bitmap getBitmap(int position) {
		int index = positions.indexOf(position);
		if (index == -1)
			return null;
		return bitmaps.get(index);
	}
	
	public static int getBitmapByteCount(Bitmap bitmap) {
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, bao);
		byte[] ba = bao.toByteArray();
		int size = ba.length;
		return size;
	}
}
