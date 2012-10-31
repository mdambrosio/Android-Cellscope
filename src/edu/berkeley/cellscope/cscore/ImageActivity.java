package edu.berkeley.cellscope.cscore;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;

public class ImageActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        ImageView view = (ImageView)findViewById(R.id.image);
        Intent intent = getIntent();
        String path = intent.getStringExtra(LibraryActivity.IMAGE_PATH);
        File file = new File(path);
        Uri imageUri = Uri.fromFile(file);
        view.setImageURI(imageUri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_image, menu);
        return true;
    }

    
}
