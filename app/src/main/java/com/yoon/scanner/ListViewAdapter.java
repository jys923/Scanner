package com.yoon.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ListViewAdapter extends BaseAdapter {
    // Adapter에 추가된 데이터를 저장하기 위한 ArrayList
    private ArrayList<ListViewItem> listViewItemList = new ArrayList<ListViewItem>();

    // ListViewAdapter의 생성자
    public ListViewAdapter() {
    }

    // Adapter에 사용되는 데이터의 개수를 리턴. : 필수 구현
    @Override
    public int getCount() {
        return listViewItemList.size();
    }

    // position에 위치한 데이터를 화면에 출력하는데 사용될 View를 리턴. : 필수 구현
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();
        // "listview_item" Layout을 inflate하여 convertView 참조 획득.
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_item, parent, false);
        }
        // 화면에 표시될 View(Layout이 inflate된)으로부터 위젯에 대한 참조 획득
        ImageView album_image = (ImageView) convertView.findViewById(R.id.album_image);
        TextView album_title = (TextView) convertView.findViewById(R.id.album_title);
        TextView album_date = (TextView) convertView.findViewById(R.id.album_date);
        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        ListViewItem listViewItem = listViewItemList.get(position);
        // 아이템 내 각 위젯에 데이터 반영
        album_image.setImageBitmap(listViewItem.getIcon());
        album_title.setText(listViewItem.getTitle());
        album_date.setText(listViewItem.getDesc());
        return convertView;
    }

    // 지정한 위치(position)에 있는 데이터와 관계된 아이템(row)의 ID를 리턴. : 필수 구현
    @Override
    public long getItemId(int position) {
        return position;
    }

    // 지정한 위치(position)에 있는 데이터 리턴 : 필수 구현
    @Override
    public Object getItem(int position) {
        return listViewItemList.get(position);
    }

    // 아이템 데이터 추가를 위한 함수. 개발자가 원하는대로 작성 가능.
//    public void addItem(Drawable icon, String title, String desc) {
//        ListViewItem item = new ListViewItem();
//        //item.setIcon(icon);
//        item.setTitle(title);
//        item.setDesc(desc);
//        listViewItemList.add(item);
//    }
    public void addItem(String title) {
        ListViewItem item = new ListViewItem();
        //item.setIcon(icon);
        item.setTitle(title);
        long now = System.currentTimeMillis();
        // 현재 시간을 저장 한다.
        Date date = new Date(now);
        // 시간 포맷으로 만든다.
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strNow = sdfNow.format(date);
        item.setDesc(strNow);
        listViewItemList.add(item);
    }
    public void addItem(String album_title,String Photo,String sqltime) {
        ListViewItem item = new ListViewItem();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap myBitmap = BitmapFactory.decodeFile(Photo,options);
        //myBitmap.getHeight();
        item.setIcon(myBitmap);
        item.setTitle(album_title);
        item.setDesc(sqltime);
        listViewItemList.add(item);
    }
    public void delItem(int index) {
        listViewItemList.remove(index);
    }
    public void setClear() {
        listViewItemList.clear();
    }
}
