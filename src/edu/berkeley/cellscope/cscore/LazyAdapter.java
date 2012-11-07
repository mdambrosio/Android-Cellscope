package edu.berkeley.cellscope.cscore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class LazyAdapter extends BaseAdapter {
	private Activity activity;
    private ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;
    public ImageLoader imageLoader; 
 
    public LazyAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
        activity = a;
        data=d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader=new ImageLoader(activity.getApplicationContext());
    }
 
    public int getCount() {
        return data.size();
    }
 
    public Object getItem(int position) {
        return position;
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        if(convertView==null)
        	//vi = (View)activity.findViewById(R.id.list);
            vi = inflater.inflate(R.layout.list_row, null);
 
        TextView title = (TextView)vi.findViewById(R.id.path); // title
        ImageView thumb_image=(ImageView)vi.findViewById(R.id.list_image); // thumb image
 
        HashMap<String, String> pic_image = new HashMap<String, String>();
        pic_image = data.get(position);
        //String pic_image = PhotoSurface.mediaStorageDir.listFiles()[position].getPath();
 
        // Setting all values in listview
        File path = PhotoSurface.mediaStorageDir.listFiles()[position];
        
        title.setText(pic_image.get(LibraryActivity.KEY_TITLE));

        Uri imageUri = Uri.fromFile(path);
        thumb_image.setImageURI(imageUri);
        //imageLoader.DisplayImage(pic_image.get(PhotoSurface.mediaStorageDir.listFiles()[position].getPath()), thumb_image);
        return vi;
    }

}
