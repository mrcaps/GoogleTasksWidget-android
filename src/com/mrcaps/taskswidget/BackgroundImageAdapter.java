package com.mrcaps.taskswidget;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

public class BackgroundImageAdapter extends BaseAdapter {
    int mGalleryItemBackground;
    private Context mContext;

    private Integer[] mImageIds = {
        R.drawable.bg_dark,
        R.drawable.bg_light,
        R.drawable.bg_note
    };
    private Integer[] mStyleIds = {
    	R.layout.widget_tasks_dark,
    	R.layout.widget_tasks_light,
    	R.layout.widget_tasks_note
    };

    public BackgroundImageAdapter(Context c) {
        mContext = c;
        TypedArray a = c.obtainStyledAttributes(R.styleable.MyGalleryStyle);
        mGalleryItemBackground = a.getResourceId(
                R.styleable.MyGalleryStyle_android_galleryItemBackground, 0);
        a.recycle();
    }

    public int getCount() {
        return mImageIds.length;
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return mStyleIds[position];
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView i = new ImageView(mContext);

        i.setImageResource(mImageIds[position]);
        i.setLayoutParams(new Gallery.LayoutParams(100, 100));
        i.setScaleType(ImageView.ScaleType.FIT_XY);
        i.setBackgroundResource(mGalleryItemBackground);

        return i;
    }
}