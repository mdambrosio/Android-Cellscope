package edu.berkeley.cellscope.cscore;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImageActivity extends Activity {
	
	private Bitmap img;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        final ImageView view = (ImageView)findViewById(R.id.image);
        Intent intent = getIntent();
        String path = intent.getStringExtra(PhotoLibraryActivity.PATH_INFO);
       
        ExifInterface exif = null;
		try {
			exif = new ExifInterface(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
               
        Bitmap raw = BitmapFactory.decodeFile(path);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        img = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), matrix, true);
        view.setImageBitmap(img);
        raw.recycle();
        
		/*img = BitmapFactory.decodeFile(path);
		view.setImageBitmap(img);
        view.setScaleType(ScaleType.MATRIX);   //required
        Matrix matrix=new Matrix();
        Rect dimen = view.getDrawable().getBounds();
        matrix.postRotate(90);
        view.setImageMatrix(matrix);
        System.out.println(view.getDrawable().getIntrinsicWidth() + " " + view.getDrawable().getIntrinsicHeight());
        System.out.println(view.getDrawable().getMinimumWidth() + " " + view.getDrawable().getMinimumHeight());
        System.out.println(dimen.width() +  " "  + dimen.height());
        System.out.println(view.getLayoutParams().height + " " + view.getLayoutParams().width);
        */
        /*File file = new File(path);
        Uri imageUri = Uri.fromFile(file);
 
        
        //view.setImageDrawable(Drawable.createFromPath(path));
        
        matrix.postRotate(90, 480, 680);
       // System.out.println(view.getDrawable().getBounds().width()/2 + " "+ view.getDrawable().getBounds().height()/2);
        view.setImageMatrix(matrix);
        
        view.setImageURI(imageUri);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	
        return true;
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	img.recycle();
    	img = null;
    }

    
}
