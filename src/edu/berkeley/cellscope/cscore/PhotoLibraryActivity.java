package edu.berkeley.cellscope.cscore;

import java.io.File;
import java.util.ArrayList;

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

public class PhotoLibraryActivity extends Activity implements OnItemClickListener {
	ListView view;
	ListView list;
	PhotoLibraryAdapter adapter;
	BitmapCache cache;
	static File directory = CameraActivity.mediaStorageDir;
	public static String PATH_INFO = "path";
	int selectedItem;
	private ArrayList<File> fileList;
    private ArrayList<String> fileNames; 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        cache = new BitmapCache(this);
        
        File[] files = directory.listFiles();
        int len = files.length;
        fileList = new ArrayList<File>(len);
        fileNames = new ArrayList<String>(len);
        //ArrayList<File> adapter = new ArrayList<File>();
        for (File f: files) {
        	fileList.add(f);
        	fileNames.add(f.getName());
        }
        //ArrayAdapter<File> adapter = new ArrayAdapter<File>(this, R.layout.activity_library, R.id.path, PhotoSurface.photoStorageDir.listFiles()); 
        // Bind to our new adapter.
        
        list = (ListView)findViewById(R.id.list);
        adapter = new PhotoLibraryAdapter(this, fileList, fileNames, cache);
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
		selectedItem = position;
		Intent intent = new Intent(this, PhotoActivity.class);
		intent.putExtra(PATH_INFO, directory.listFiles()[position].getPath());
		startActivityForResult(intent, 1);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if(resultCode == RESULT_OK){//file was deleted
				System.out.println("RESULT OK");
				File f = fileList.remove(selectedItem);
				f.delete();
				fileNames.remove(selectedItem);
				cache.removeItem(selectedItem);
				adapter.notifyDataSetChanged();
			}
		}
	}

}
