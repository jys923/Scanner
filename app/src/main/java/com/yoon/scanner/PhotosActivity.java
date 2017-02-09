package com.yoon.scanner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PhotosActivity extends AppCompatActivity {
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;
    GridViewAdapter adapter;
    String album = null;
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                dialog = ProgressDialog.show(PhotosActivity.this, "", "Loading file...", true);
//            }
//        });
        final GridView gridView;
        adapter = new GridViewAdapter();
        gridView = (GridView) findViewById(R.id.gridView);
        gridView.setAdapter(adapter);
        final Intent intent1 = getIntent();
        album = intent1.getExtras().getString("album");
        TextView album_title=(TextView) findViewById(R.id.album_title);
        album_title.setText(album);
        ImageButton button = (ImageButton) findViewById(R.id.photo_add_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PhotosActivity.this, MainActivity.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("album", album);
                startActivity(intent);
            }
        });
        /**그냥 클릭**/
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
//                Toast.makeText(getApplicationContext(), (((GridViewItem) adapter.getItem(i)).getTitle()), Toast.LENGTH_SHORT).show();
                /****/
                Dialog dialog = new Dialog(PhotosActivity.this);
                dialog.setContentView(R.layout.activity_photos_custom_dialog);
                dialog.setTitle("크게보기");
                ImageView imageView = (ImageView)dialog.findViewById(R.id.imageView);
                String fileName = ((GridViewItem) adapter.getItem(i)).getTitle();
                Bitmap myBitmap = BitmapFactory.decodeFile(fileName);
                imageView.setImageBitmap(myBitmap);
                TextView textView = (TextView) dialog.findViewById(R.id.textView);
                textView.setText(((GridViewItem) adapter.getItem(i)).getTitle());
                Button share_btn = (Button)dialog.findViewById(R.id.share_btn);
                share_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.putExtra(Intent.EXTRA_SUBJECT, "제목");
                        Uri uri = Uri.fromFile(new File(((GridViewItem) adapter.getItem(i)).getTitle()));
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.setType("image/*");
                        startActivity(Intent.createChooser(intent, "이 사진을 공유합니다."));
                    }
                });
                Button del_btn = (Button)dialog.findViewById(R.id.del_btn);
                del_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //지운다 물어보고
                        final String s = (((GridViewItem) adapter.getItem(i)).getTitle());
                        AlertDialog.Builder dialog = new AlertDialog.Builder(PhotosActivity.this);
                        dialog.setTitle("삭제");
                        dialog.setMessage(s + "-지운다?");
                        dialog.setNegativeButton("지우지마",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        //dialog.dismiss();
                                    }
                                });
                        dialog.setPositiveButton("지워라",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        File bitmapFile = new File(s);
                                        if (bitmapFile.exists()) {
                                            bitmapFile.delete();
                                        }
                                        /**로컬디비삭제**/
                                        db.execSQL("DELETE FROM albums WHERE photo = '" + s + "';");
                                        //db.execSQL("DELETE FROM albums WHERE photo = '" + s + "' AND album = '" + album + "'; ");
                                        Toast.makeText(PhotosActivity.this, (((GridViewItem) adapter.getItem(i)).getTitle()) + "로컬-지웠다", Toast.LENGTH_SHORT).show();
                                        /**서버디비지우기**/
                                        Thread thLogin = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    String temp = s;
                                                    String temp2 = null;
                                                    temp2 = MyApplication.serverIP + "/uploads/" + temp.substring(s.lastIndexOf("/") + 1, s.length());
                                                    Log.d("서버결과01", temp2);
                                                    String url = MyApplication.serverIP + "/scanner_Sync_Del.php?username=" + MyApplication.username + "&fileurl=" + temp2;
                                                    Log.d("서버결과01", url);
                                                    URL phpUrl = new URL(url);
                                                    HttpURLConnection conn = (HttpURLConnection) phpUrl.openConnection();
                                                    if (conn != null) {
                                                        conn.setConnectTimeout(10000);
                                                        conn.setUseCaches(false);
                                                        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                                                            while (true) {
                                                                String line = br.readLine();
                                                                //Log.d("서버결과01",line);
                                                                if (line == null) {
                                                                    break;
                                                                } else if (line.trim().equals("0")) {
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Toast.makeText(PhotosActivity.this, s + "-서버에서 이미 지워짐", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                                } else if (line.trim().equals("1")) {
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Toast.makeText(PhotosActivity.this, s + "-서버에서 지워짐", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                            br.close();
                                                        }
                                                        conn.disconnect();
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
//                                        Toast.makeText(getApplicationContext(), (((GridViewItem) adapter.getItem(i)).getTitle()) + "서버-지웠다", Toast.LENGTH_SHORT).show();
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        adapter.removeItem(i);
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                });
                                            }
                                        });
                                        thLogin.start();
//                                try {
//                                    thLogin.join();
//                                    adapter.notifyDataSetChanged();
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
                                        /**서버끝~~**/
                                    }
                                });
                        AlertDialog alertDialog = dialog.create();    // 알림창 객체 생성
                        alertDialog.show();    // 알림창 띄우기
                    }
                });
                dialog.show();

            }
        });
        /**롱 클릭**/
//        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
//                //지운다 물어보고
//                final String s = (((GridViewItem) adapter.getItem(i)).getTitle());
//                AlertDialog.Builder dialog = new AlertDialog.Builder(PhotosActivity.this);
//                dialog.setTitle("삭제");
//                dialog.setMessage(s + "-지운다?");
//                dialog.setNegativeButton("지우지마",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                dialog.cancel();
//                                //dialog.dismiss();
//                            }
//                        });
//                dialog.setPositiveButton("지워라",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int id) {
//                                File bitmapFile = new File(s);
//                                if (bitmapFile.exists()) {
//                                    bitmapFile.delete();
//                                }
//                                /**로컬디비삭제**/
//                                db.execSQL("DELETE FROM albums WHERE photo = '" + s + "';");
//                                //db.execSQL("DELETE FROM albums WHERE photo = '" + s + "' AND album = '" + album + "'; ");
//                                Toast.makeText(PhotosActivity.this, (((GridViewItem) adapter.getItem(i)).getTitle()) + "로컬-지웠다", Toast.LENGTH_SHORT).show();
//                                /**서버디비지우기**/
//                                Thread thLogin = new Thread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        try {
//                                            String temp = s;
//                                            String temp2 = null;
//                                            temp2 = MyApplication.serverIP + "/uploads/" + temp.substring(s.lastIndexOf("/") + 1, s.length());
//                                            Log.d("서버결과01", temp2);
//                                            String url = MyApplication.serverIP + "/scanner_Sync_Del.php?username=" + MyApplication.username + "&fileurl=" + temp2;
//                                            Log.d("서버결과01", url);
//                                            URL phpUrl = new URL(url);
//                                            HttpURLConnection conn = (HttpURLConnection) phpUrl.openConnection();
//                                            if (conn != null) {
//                                                conn.setConnectTimeout(10000);
//                                                conn.setUseCaches(false);
//                                                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                                                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
//                                                    while (true) {
//                                                        String line = br.readLine();
//                                                        //Log.d("서버결과01",line);
//                                                        if (line == null) {
//                                                            break;
//                                                        } else if (line.trim().equals("0")) {
//                                                            runOnUiThread(new Runnable() {
//                                                                @Override
//                                                                public void run() {
//                                                                    Toast.makeText(PhotosActivity.this, s + "-서버에서 이미 지워짐", Toast.LENGTH_SHORT).show();
//                                                                }
//                                                            });
//                                                        } else if (line.trim().equals("1")) {
//                                                            runOnUiThread(new Runnable() {
//                                                                @Override
//                                                                public void run() {
//                                                                    Toast.makeText(PhotosActivity.this, s + "-서버에서 지워짐", Toast.LENGTH_SHORT).show();
//                                                                }
//                                                            });
//                                                        }
//                                                    }
//                                                    br.close();
//                                                }
//                                                conn.disconnect();
//                                            }
//                                        } catch (Exception e) {
//                                            e.printStackTrace();
//                                        }
////                                        Toast.makeText(getApplicationContext(), (((GridViewItem) adapter.getItem(i)).getTitle()) + "서버-지웠다", Toast.LENGTH_SHORT).show();
//                                        runOnUiThread(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                adapter.removeItem(i);
//                                                adapter.notifyDataSetChanged();
//                                            }
//                                        });
//                                    }
//                                });
//                                thLogin.start();
////                                try {
////                                    thLogin.join();
////                                    adapter.notifyDataSetChanged();
////                                } catch (InterruptedException e) {
////                                    e.printStackTrace();
////                                }
//                                /**서버끝~~**/
//                            }
//                        });
//                AlertDialog alertDialog = dialog.create();    // 알림창 객체 생성
//                alertDialog.show();    // 알림창 띄우기
//                return false;
//            }
//        });
    }

    @Override
    protected void onResume() {
        /**
         * 디비 조회해서 adapter.addItem(ContextCompat.getDrawable(this, R.drawable.ic_account_box_black_36dp),
         "Box", "Account Box Black 36dp") ;
         * **/
        adapter.setClear();
        helper = new MySQLiteOpenHelper(PhotosActivity.this, // 현재 화면의 context
                "albums.db", // 파일명
                null, // 커서 팩토리
                1); // 버전 번호
        try {
                                /*데이터베이스 객체를 얻어오는 다른 간단한 방법
                                db = openOrCreateDatabase(dbName,  // 데이터베이스파일 이름
                                        Context.MODE_PRIVATE, // 파일 모드
                                        null);    // 커서 팩토리
                                String sql = "create table mytable(id integer primary key autoincrement, name text);";
                                db.execSQL(sql);*/
            db = helper.getWritableDatabase(); // 읽고 쓸수 있는 DB
            //db = helper.getReadableDatabase(); // 읽기 전용 DB select문
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.d("데이터베이스", "데이터베이스를 얻어올 수 없음");
            finish(); // 액티비티 종료
        }
        // 1. 데이터 저장
        //db.execSQL("INSERT INTO albums (album, photo) VALUES ('"+album+"','"+photoPath+"');");
        //Cursor cursor = db.rawQuery("SELECT album,photo FROM ALBUMS WHERE album='" + album + "'", null);
        Cursor cursor = db.rawQuery("SELECT album,photo FROM ALBUMS WHERE album='" + album + "' and username='" + MyApplication.username + "';", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String all = cursor.getString(cursor.getColumnIndex("album"))
                        + "," + cursor.getString(cursor.getColumnIndex("photo"));
                //String photo = cursor.getString(2);
                Log.d("데이터베이스", all);
                adapter.addItem(cursor.getString(cursor.getColumnIndex("photo")));
            }
        }
        MyApplication.progressDialogforactive.dismiss();
        super.onResume();
    }
}
