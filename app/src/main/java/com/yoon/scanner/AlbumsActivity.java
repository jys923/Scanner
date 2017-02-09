package com.yoon.scanner;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AlbumsActivity extends AppCompatActivity {
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;
    ListViewAdapter adapter;
    ProgressDialog dialog;
    StringBuilder jsonHtml = new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_albums);
        final ListView listview;
        //adapter = null;
        // Adapter 생성
        adapter = new ListViewAdapter();
        // 리스트뷰 참조 및 Adapter달기
        listview = (ListView) findViewById(R.id.listView);
        listview.setAdapter(adapter);
        final EditText album_editText = (EditText) findViewById(R.id.album_editText);
        Button album_add_btn = (Button) findViewById(R.id.album_add_btn);
        album_add_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //adapter.addItem(ContextCompat.getDrawable(/*getApplicationContext()*/getBaseContext(), android.R.drawable.ic_menu_preferences), album_editText.getText().toString(), new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(System.currentTimeMillis())));
                if ((album_editText.getText().toString().trim()).isEmpty()) {
                    Toast.makeText(AlbumsActivity.this, "엘범이름 적어", Toast.LENGTH_SHORT).show();
                } else {
                    adapter.addItem(album_editText.getText().toString());
                    adapter.notifyDataSetChanged();
                    album_editText.setText("");
                }
            }
        });

        /**그냥클릭
         * **/
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Toast.makeText(AlbumsActivity.this ,((ListViewItem)adapter.getItem(i)).getTitle(), Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(AlbumsActivity.this, MainActivity.class);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        //MyApplication.progressDialogforactive = ProgressDialog.show(AlbumsActivity.this, "", "Loading file...", true);
//                    }
//                });
                MyApplication.progressDialogforactive = ProgressDialog.show(AlbumsActivity.this, "", "Loading file...", true);
                db.close();
                helper.close();
                Intent intent = new Intent(AlbumsActivity.this, PhotosActivity.class);
                MyApplication.album = ((ListViewItem) adapter.getItem(i)).getTitle();
                intent.putExtra("album", ((ListViewItem) adapter.getItem(i)).getTitle());
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        /**롱클릭
         * **/
        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AlbumsActivity.this);
                builder.setTitle("Title");
//                builder.setIcon(R.drawable.icon);
                builder.setMessage("Message");
                builder.setPositiveButton("엘범.zip",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //스레드로 디비에서 엘범에 해당파일 조회
                                //집파일 생성
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ArrayList<String> stringArrayList = new ArrayList<String>();
                                        Cursor cursor = db.rawQuery("SELECT * FROM albums WHERE album='"+((ListViewItem) adapter.getItem(i)).getTitle()+"' and username='" + MyApplication.username + "' order by sqltime;", null);
                                        if (cursor != null) {
                                            while (cursor.moveToNext()) {
                                                stringArrayList.add(cursor.getString(cursor.getColumnIndex("photo")));
                                            }
                                        }
                                        // Create a buffer for reading the files
                                        int size = 1024;
                                        byte[] buf = new byte[size];
                                        // Create the ZIP file
                                        try {
                                            String outDirname = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/albums/";
                                            final String Filename =((ListViewItem) adapter.getItem(i)).getTitle();
                                            String outFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/albums/" + Filename + ".zip";
                                            File file = new File(outDirname);
                                            //상위 디렉토리가 존재하지 않을 경우 생성
                                            if (!file.exists()) {
                                                file.mkdirs();//폴더생성
                                            }
//                                    String outFilename = "D:/outfile.zip";
                                            ///storage/emulated/0/YoonScanner/ysc_1470995370117_warp.jpg
                                            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFilename)));
                                            // Compress the files
                                            for (int i = 0; i < stringArrayList.size(); i++) {
                                                FileInputStream fs = new FileInputStream(stringArrayList.get(i));
                                                BufferedInputStream in = new BufferedInputStream(fs, size);
                                                // Add ZIP entry to output stream.
                                                String s = stringArrayList.get(i);
                                                String s1 = s.substring(s.lastIndexOf("/")+1,s.length());
                                                out.putNextEntry(new ZipEntry(s1)); // Zip 파일에 경로를 정하여 저장할수 있다.
                                                // Transfer bytes from the file to the ZIP file
                                                int len;
                                                while ((len = in.read(buf, 0, size)) > 0) {
                                                    out.write(buf, 0, len);
                                                }
                                                // Complete the entry
                                                out.closeEntry();
                                                in.close();
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(AlbumsActivity.this,Filename + ".zip \n파일생성끝 ", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                            // Complete the ZIP file
                                            out.close();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                            }
                        });
                builder.setNegativeButton("엘범삭제",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //내용
                                //로컬
                                //파일삭제
                                File file;
                                final String album=((ListViewItem) adapter.getItem(i)).getTitle();
                                String username=MyApplication.username;

                                Cursor cursor = db.rawQuery("SELECT * FROM albums WHERE album='"+album+"' and username='" + MyApplication.username + "' order by sqltime;", null);
                                if (cursor != null) {
                                    while (cursor.moveToNext()) {
                                        file = new File(cursor.getString(cursor.getColumnIndex("photo")));
                                        //상위 디렉토리가 존재하지 않을 경우 생성
                                        if (file.exists()) {
                                            file.delete();
                                        }
                                    }
                                }
                                //디비삭제
                                db.execSQL("DELETE FROM albums WHERE username = '" + username + "'and album='"+album+"';");
                                //서버
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String url = MyApplication.serverIP + "/scanner_Sync_Del_Album.php?username=" + MyApplication.username + "&album=" + album;
                                            Log.d("url",url);
                                            URL phpUrl = new URL(url);
                                            HttpURLConnection conn = (HttpURLConnection) phpUrl.openConnection();
                                            if (conn != null) {
                                                conn.setConnectTimeout(10000);
                                                conn.setUseCaches(false);
                                                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                                                    while (true) {
                                                        String line = br.readLine();
                                                        if (line == null)
                                                            break;
                                                        jsonHtml.append(line + "\n");
                                                    }
                                                    br.close();
                                                }
                                                conn.disconnect();
                                            }
//                                            if ((jsonHtml.toString().trim().equals("0"))) {
//                                                int Idx = jsonHtml.toString().lastIndexOf("<br/>");/*<br/>*/
//                                                String jsonHtml2 = jsonHtml.toString().substring(Idx + 5, jsonHtml.toString().length());
//                                                JSONArray jsonArray = new JSONArray(jsonHtml2);
//                                                for (int i = 0; i < jsonArray.length(); i++) {
//                                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
////                                                    fileurl = jsonObject.getString("filename");
//                                                }
//                                            }
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    adapter.delItem(i);
                                                    adapter.notifyDataSetChanged();
                                                    Toast.makeText(AlbumsActivity.this,album+"엘범안에\n 사진 "+jsonHtml.toString().trim()+"개 지웠다", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                thread.start();
                            }
                        });
                builder.setNeutralButton("파일보내기",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //내용
                                String outFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/albums/" + ((ListViewItem) adapter.getItem(i)).getTitle() + ".zip";
                                //리스트뷰 별로 폴더 생성할까?
                                File file = new File(outFilename);
                                //상위 디렉토리가 존재하지 않을 경우 생성
                                if (file.exists()) {
                                    //공유
                                    Intent intent = new Intent(Intent.ACTION_SEND);
                                    intent.putExtra(Intent.EXTRA_SUBJECT, "제목");
                                    Uri uri = Uri.fromFile(new File(outFilename));
                                    intent.putExtra(Intent.EXTRA_STREAM, uri);
//                                    intent.setType("zip/*");
                                    intent.setType("application/zip");
                                    startActivity(Intent.createChooser(intent, "이 사진을 공유합니다."));
                                } else {
                                    Toast.makeText(AlbumsActivity.this, "공유할 파일이없다\n zip파일생성부터해", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                AlertDialog alertDialog = builder.create();    // 알림창 객체 생성
                alertDialog.show();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        //adapter = new ListViewAdapter();
        //디비불러오기
        adapter.setClear();
        helper = new MySQLiteOpenHelper(AlbumsActivity.this, // 현재 화면의 context
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
//        Cursor cursor = db.rawQuery("SELECT *\n" +
//                "FROM albums\n" +
//                "WHERE _id IN \n" +
//                "(SELECT MAX(_id) FROM albums GROUP BY album) \n" +
//                "order by album;", null);
        Cursor cursor = db.rawQuery("SELECT * FROM albums WHERE _id IN (SELECT MAX(_id) FROM albums where username='" + MyApplication.username + "' GROUP BY album)order by album;", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String all = cursor.getString(cursor.getColumnIndex("album"))
                        + "," + cursor.getString(cursor.getColumnIndex("photo"))
                        + "," + cursor.getString(cursor.getColumnIndex("sqltime"));
                //String photo = cursor.getString(2);
                Log.d("데이터베이스", all);
                adapter.addItem(cursor.getString(cursor.getColumnIndex("album")), cursor.getString(cursor.getColumnIndex("photo")), cursor.getString(cursor.getColumnIndex("sqltime")));
                //adapter.addItem(cursor.getString(cursor.getColumnIndex("photo")));
            }
        }
        //Log.d("디비전체",helper.PrintData());
        adapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        //뒤로가기 버튼 막기 로그아웃
        AlertDialog.Builder dialog = new AlertDialog.Builder(AlbumsActivity.this);
        dialog.setTitle("로그아웃");
        dialog.setMessage(MyApplication.username + "님\n나갈래?");
        dialog.setNegativeButton("아니",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        //dialog.dismiss();
                    }
                });
        dialog.setPositiveButton("나간다",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        db.close();
                        helper.close();
                        moveTaskToBack(true);
                        finish();
                        startActivity(new Intent(AlbumsActivity.this, LoginActivity.class));
                    }
                });
        dialog.show();
//        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        //dialog.dismiss();
        super.onStop();
    }
}
