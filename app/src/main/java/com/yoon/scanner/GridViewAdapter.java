package com.yoon.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016-07-27.
 */
public class GridViewAdapter extends BaseAdapter {
    ArrayList<GridViewItem> gridViewItemList = new ArrayList<GridViewItem>();

    public GridViewAdapter() {
    }

    @Override
    public int getCount() {
        return gridViewItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return gridViewItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();
        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.gridview_item, parent, false);
        }
        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        //TextView titleTextView = (TextView) convertView.findViewById(R.id.textView1);
        //TextView descTextView = (TextView) convertView.findViewById(R.id.textView2);
        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        //ListViewItem listViewItem = listViewItemList.get(position);
        GridViewItem gridViewItem = gridViewItemList.get(position);
        // 아이템 내 각 위젯에 데이터 반영
        //iconImageView.setImageURI();
        //Bitmap myBitmap = BitmapFactory.decodeFile(gridViewItem.getTitle());
        imageView.setImageBitmap(gridViewItem.getIcon());
        //imageView.setLayoutParams(gridViewItem.getIcon());
        //titleTextView.setText(gridViewItem.getTitle());
        //descTextView.setText(gridViewItem.getDesc());
        return convertView;
    }

    // 아이템 데이터 추가를 위한 함수. 개발자가 원하는대로 작성 가능.
    public void addItem(Drawable icon, String title, String desc) {
        GridViewItem gridViewItem = new GridViewItem();
        //gridViewItem.setIcon(icon);
        gridViewItem.setTitle(title);
        gridViewItem.setDesc(desc);
        gridViewItemList.add(gridViewItem);
    }
    // 아이템 데이터 추가를 위한 함수. 개발자가 원하는대로 작성 가능.
    public void addItem(String title) {
        GridViewItem gridViewItem = new GridViewItem();
        gridViewItem.setTitle(title);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap myBitmap = BitmapFactory.decodeFile(title,options);
//        Bitmap myBitmap = BitmapFactory.decodeFile(title);
//        int viewHeight = 10;
//
//        float width = myBitmap.getWidth();
//        float height = myBitmap.getHeight();
//        if(height > viewHeight)
//        {
//            float percente = (float)(height / 100);
//            float scale = (float)(viewHeight / percente);
//            width *= (scale / 100);
//            height *= (scale / 100);
//        }
        //gridViewItem.setIcon(Bitmap.createScaledBitmap(myBitmap,120,160, true));
        gridViewItem.setIcon(myBitmap);

        gridViewItemList.add(gridViewItem);
    }
    public void setClear() {
        gridViewItemList.clear();
    }
    public void removeItem(int position) {
        gridViewItemList.remove(position);
    }
}
