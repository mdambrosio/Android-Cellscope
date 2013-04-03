package edu.berkeley.cellscope.cscore;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

public class ImageLoader {
	
	public static Bitmap placeHolderBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ALPHA_8);
	public static void loadVideoThumbnail(ImageView imageView, ArrayList<File> files, int position, int size, BitmapCache cache) {
		if (cancelPotentialWork(position, files, imageView)) {
	        final VideoBitmapWorkerTask task = new VideoBitmapWorkerTask(imageView, files, size, cache, position);
	        final AsyncDrawable asyncDrawable =
	                new AsyncDrawable(placeHolderBitmap, task);
	        imageView.setImageDrawable(asyncDrawable);
	        task.execute(position);
	    }
	}
	
	public static void loadPhotoThumbnail(ImageView imageView, ArrayList<File> files, int position, int size, BitmapCache cache) {
		System.out.println("Queueing " + position);
		if (cancelPotentialWork(position, files, imageView)) {
	        final BitmapWorkerTask task = new BitmapWorkerTask(imageView, files, size, cache, position);
	        final AsyncDrawable asyncDrawable = new AsyncDrawable(placeHolderBitmap, task);
	        imageView.setImageDrawable(asyncDrawable);
	        task.execute();
	    }
	}
	
	public static boolean cancelPotentialWork(int position, ArrayList<File> files, ImageView imageView) {
	    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

	    if (bitmapWorkerTask != null) {
	        if (files != bitmapWorkerTask.files || position != bitmapWorkerTask.position) {
	            // Cancel previous task
	            bitmapWorkerTask.cancel(true);
	        } else {
	            // The same work is already in progress
	            return false;
	        }
	    }
	    // No task associated with the ImageView, or an existing task was cancelled
	    return true;
	}
	
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
       if (imageView != null) {
           final Drawable drawable = imageView.getDrawable();
           if (drawable instanceof AsyncDrawable) {
               final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
               return asyncDrawable.getBitmapWorkerTask();
           }
        }
        return null;
    }
	
	private static Bitmap decodeSampledBitmapFromImageFile(String path, int reqSize) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		// First decode with inJustDecodeBounds=true to check dimensions
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize);
		// Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    Bitmap thumbnail = BitmapFactory.decodeFile(path, options);
	   /* if (thumbnail.getWidth() > thumbnail.getHeight()) {
	        Matrix matrix = new Matrix();
	        matrix.postRotate(90);
	        Bitmap raw = thumbnail;
	        thumbnail = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), matrix, true);
	        raw.recycle();
	        raw = null;
	    }*/
	    return thumbnail;
		//return ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(path), reqSize, reqSize, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
	}
	
	private static Bitmap decodeSampledBitmapFromVideoFile(String path, int reqSize) {
		Bitmap original = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
		if (original == null)
			return placeHolderBitmap;
		Bitmap scaled = Bitmap.createScaledBitmap(original, reqSize, reqSize, false);
		original.recycle();
		return scaled;
	}
	
	//Calculates scaled down  image size
	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	        if (width > height) {
	            inSampleSize = Math.round((float)height / (float)reqHeight);
	        } else {
	            inSampleSize = Math.round((float)width / (float)reqWidth);
	        }
	    }
	    return inSampleSize;
	}
	
	static class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
		protected final BitmapCache cache;
		protected final WeakReference<ImageView> imageViewReference;
		protected final ArrayList<File> files;
		protected final int size;
	    protected int position = 0;

	    public BitmapWorkerTask(ImageView imageView, ArrayList<File> fileList, int size, BitmapCache imageCache, int pos) {
	    	cache = imageCache;
	        // Use a WeakReference to ensure the ImageView can be garbage collected
	        imageViewReference = new WeakReference<ImageView>(imageView);
	        files = fileList;
	        this.size = size;
	        this.position = pos;
	    }
	    
	    public void execute() {
	    	this.execute(position);
	    }

	    // Decode image in background.
	    @Override
	    protected Bitmap doInBackground(Integer... params) {

	    	System.out.println("Executing " + position);
	    	int position = params[0];
	        Bitmap bmp = cache.getBitmap(position);
	        if (bmp == null) {
	        	bmp = decodeSampledBitmapFromImageFile(files.get(position).getPath(), size);
	        	cache.addBitmap(position, bmp);
	        }
	    	System.out.println("Execution done... " + position);
	        return bmp;
	    }

	    // Once complete, see if ImageView is still around and set bitmap.
	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	    	System.out.println("Completing " + position);
	    	 if (isCancelled()) {
	             bitmap = null;
	         }

	         if (imageViewReference != null && bitmap != null) {
	             final ImageView imageView = imageViewReference.get();
	             final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
	             if (this == bitmapWorkerTask && imageView != null) {
	                 imageView.setImageBitmap(bitmap);
	             }
	         }
	    }
	}
	
	static class VideoBitmapWorkerTask extends BitmapWorkerTask {

		public VideoBitmapWorkerTask(ImageView imageView, ArrayList<File> fileList, int size, BitmapCache imageCache, int position) {
			super(imageView, fileList, size, imageCache, position);
		}
		
		protected Bitmap doInBackground(Integer... params) {
	        position = params[0];
	        Bitmap bmp = cache.getBitmap(position);
	        if (bmp == null) {
	        	bmp = decodeSampledBitmapFromVideoFile(files.get(position).getPath(), size);
	        	cache.addBitmap(position, bmp);
	        }
	        return bmp;
	    }

	}
	
	static class AsyncDrawable extends BitmapDrawable {
	    private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

	    @SuppressWarnings("deprecation")
		public AsyncDrawable(Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
	        super(bitmap);
	        bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
	    }

	    public BitmapWorkerTask getBitmapWorkerTask() {
	        return bitmapWorkerTaskReference.get();
	    }
	}
}
