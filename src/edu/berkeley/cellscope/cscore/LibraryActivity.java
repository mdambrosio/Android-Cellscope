package edu.berkeley.cellscope.cscore;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class LibraryActivity extends Activity implements OnItemClickListener {
	ListView view;
	
	public static final String IMAGE_PATH = "path";
	public static final String KEY_TITLE = "title";
	
	ListView list;
	LazyAdapter adapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        
        ArrayList<HashMap<String, String>> new_list = new ArrayList<HashMap<String, String>>();
        //HashMap<String, String> map = new HashMap<String, String>();
        //ArrayList<File> adapter = new ArrayList<File>();
        for (int i = 0; i < PhotoSurface.mediaStorageDir.listFiles().length; i++) {
        	HashMap<String, String> map = new HashMap<String, String>();
        	String element = PhotoSurface.mediaStorageDir.listFiles()[i].getPath();
        	map.put(KEY_TITLE, element);
        	map.put(IMAGE_PATH, element);
        	
        	new_list.add(map);
        }
        //ArrayAdapter<File> adapter = new ArrayAdapter<File>(this, R.layout.activity_library, R.id.path, PhotoSurface.mediaStorageDir.listFiles()); 
        // Bind to our new adapter.
        
        list = (ListView)findViewById(R.id.list);
        adapter = new LazyAdapter(this, new_list);
        list.setAdapter(adapter);
        //setListAdapter(adapter);
        
        list.setOnItemClickListener(this);
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
