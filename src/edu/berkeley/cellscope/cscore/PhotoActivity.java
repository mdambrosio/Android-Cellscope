package edu.berkeley.cellscope.cscore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class PhotoActivity extends Activity {
	
	ZoomableImageView view;
	String path;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        view = (ZoomableImageView)findViewById(R.id.image_view);
        Intent intent = getIntent();
        path = intent.getStringExtra(PhotoLibraryActivity.PATH_INFO);
        view.setImage(path);
		//img = BitmapFactory.decodeFile(path);
		//view.setImageBitmap(img);
         /*      
        Bitmap raw = BitmapFactory.decodeFile(path);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        img = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), matrix, true);
        view.setImageBitmap(img);
        raw.recycle();
        
		img = BitmapFactory.decodeFile(path);
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
        getMenuInflater().inflate(R.menu.activity_image, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent returnIntent = new Intent();
    	setResult(RESULT_OK, returnIntent); 
    	finish();
    	return true;
    }
    
}
