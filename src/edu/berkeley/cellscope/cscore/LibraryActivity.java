package edu.berkeley.cellscope.cscore;

import java.io.File;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class LibraryActivity extends ListActivity implements OnItemClickListener {
	ListView view;
	
	public static final String IMAGE_PATH = "path";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        ArrayAdapter<File> adapter = new ArrayAdapter<File>(this, R.layout.activity_library, R.id.text1, PhotoSurface.mediaStorageDir.listFiles()); 
        // Bind to our new adapter.
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_library, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		System.out.println();
		Intent intent = new Intent(this, ImageActivity.class);
		intent.putExtra(IMAGE_PATH, PhotoSurface.mediaStorageDir.listFiles()[position].getPath());
		startActivity(intent);
	}
}
