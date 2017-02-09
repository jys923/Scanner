package com.yoon.scanner;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.yoon.scanner.ndk.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
//import java.io.*;

public class MainActivity extends AppCompatActivity {

    int flag = 0;//0찍기만
    //1-업로드,카드
    //2-저장
    TextView messageText;
    Button btn = null;
    Button uploadButton;
    Button saveButton;
    Button cardButton;
    Button jniButton;
    Button jni_saveButton;
    int serverResponseCode = 0;
    ProgressDialog dialog = null;
    String upLoadServerUri = null;
    Bitmap bitmap;
    String _fileName = null;
    String _fileEx = null;
    String saveFileName = null;
    String temp = null;
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;
    String jni_warp_result_filepath;
    /**********
     * File Path
     *************/
    final String uploadFilePath = "storage/emulated/0/";//경로를 모르겠으면, 갤러리 어플리케이션 가서 메뉴->상세 정보
    final String uploadFileName = "test.jpg"; //전송하고자하는 파일 이름
    ImageView iv = null;
    Uri mImageCaptureUri = null;
    String album = null;
    StringBuilder jsonHtml = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        album = intent.getExtras().getString("album");
        setup();
        /**
         * jni
         * **/
        jniButton = (Button) findViewById(R.id.jni);
        jniButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                /**와핑결과파일출력용
                 * **/
                if (flag == 1) {
                    //File fileName = new File("" + mImageCaptureUri);
                    //Log.d("util.warp()", mImageCaptureUri.getPath());
                    dialog = ProgressDialog.show(MainActivity.this, "", "warping file...", true);
                    Thread warp_thread=new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String filepath = mImageCaptureUri.getPath();
                            Util util = new Util();
                            jni_warp_result_filepath = util.warp(filepath);
                            File bitmapFile = new File(filepath);
                            if (bitmapFile.exists()) {
                                bitmapFile.delete();
                            }
                        }
                    });
                    warp_thread.start();
                    try {
                        warp_thread.join();
                        dialog.dismiss();
                        /**와핑결과 보여주기**/
                        iv.setImageURI(Uri.fromFile(new File(jni_warp_result_filepath)));
                        flag = 5;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //Log.d("util.warp()",s);
                }else {
                    Toast.makeText(MainActivity.this, "사진먼저찍어", Toast.LENGTH_SHORT).show();
                }
            }
        });
        jni_saveButton = (Button) findViewById(R.id.jni_save);
        jni_saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                upLoadServerUri = MyApplication.serverIP + "/scanner_UploadToServer_Jni.php";//서버컴퓨터의 ip주소
                if (flag == 5) {
                    //flag = 0;
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("와핑이 맘에 드냐?");
                    //dialog.setView(etEdit);
                    dialog.setPositiveButton("사진다시찍을래", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dialog.cancel();
                            /**파일삭제**/
                            String s = jni_warp_result_filepath;
                            File bitmapFile = new File(s);
                            if (bitmapFile.exists()) {
                                bitmapFile.delete();
                            }
                            flag = 0;
                            MainActivity.this.finish();
                            Intent intent = new Intent(MainActivity.this, MainActivity.class);
                            intent.putExtra("album", album);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            //iv.setImageBitmap(null);
                            startActivity(intent);
                        }
                    });
                    // Cancel 버튼 이벤트
                    dialog.setNegativeButton("엘범으로", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dialog.cancel();
                            new Thread(new Runnable() {
//                            StringBuilder jsonHtml = new StringBuilder();
                                @Override
                                public void run() {
                                    try {
                                        /**저장누르면 로컬디비**/
                                        //Uri uri= Uri.fromFile(new File(jni_warp_result_filepath));
                                        String fileName = new File("" + jni_warp_result_filepath).getName();
                                        //Log.d("1", fileName.getName());
                                        helper = new MySQLiteOpenHelper(MainActivity.this, // 현재 화면의 context
                                                "albums.db", // 파일명
                                                null, // 커서 팩토리
                                                1); // 버전 번호
                                        db = helper.getWritableDatabase(); // 읽고 쓸수 있는 DB
                                        db.execSQL("INSERT INTO albums (album,photo,username) VALUES ('" + album + "','" + jni_warp_result_filepath + "', '" + MyApplication.username + "');");
                                        /**서버업로드**/
                                        Log.d("String.valueOf(Uri.fromFile(new File(jni_warp_result_filepath)))",String.valueOf(Uri.fromFile(new File(jni_warp_result_filepath))));
                                        uploadFile(Uri.fromFile(new File(jni_warp_result_filepath)).getPath());
//                                        dsfdsf
                                        /**서버 디비 저장**/
                                        String url = MyApplication.serverIP + "/scanner_Sync.php?username=" + MyApplication.username + "&fileurl=" +"http://192.168.0.11:63327/uploads/"+fileName + "&album=" + album;
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
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    db.close();
                                    helper.close();
                                }
                            }).start();
                            Intent intent = new Intent(MainActivity.this, AlbumsActivity.class);
                            //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                        }
                    });
                    dialog.setNeutralButton("사진더찍을래", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        /**저장누르면 로컬디비**/
                                        //Uri uri= Uri.fromFile(new File(jni_warp_result_filepath));
                                        String fileName = new File("" + jni_warp_result_filepath).getName();
                                        //Log.d("1", fileName.getName());
                                        helper = new MySQLiteOpenHelper(MainActivity.this, // 현재 화면의 context
                                                "albums.db", // 파일명
                                                null, // 커서 팩토리
                                                1); // 버전 번호
                                        db = helper.getWritableDatabase(); // 읽고 쓸수 있는 DB
                                        db.execSQL("INSERT INTO albums (album,photo,username) VALUES ('" + album + "','" + jni_warp_result_filepath + "', '" + MyApplication.username + "');");
                                        /**서버업로드**/
                                        uploadFile(Uri.fromFile(new File(jni_warp_result_filepath)).getPath());
                                        /**서버 디비 저장**/
                                        String url = MyApplication.serverIP + "/scanner_Sync.php?username=" + MyApplication.username + "&fileurl=" +"http://192.168.0.11:63327/uploads/"+fileName + "&album=" + album;
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
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                            flag = 0;
                            MainActivity.this.finish();
                            Intent intent = new Intent(MainActivity.this, MainActivity.class);
                            intent.putExtra("album", album);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            //iv.setImageBitmap(null);
                            startActivity(intent);
                        }
                    });
                    dialog.show();
                } else {
                    Toast.makeText(MainActivity.this, "와핑파일 먼저 받아", Toast.LENGTH_SHORT).show();
                }
            }
        });
        /**
         * 선택박스3개짜리
         * **/
        saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag == 2) {
//                    flag = 0;
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("와핑이 맘에 드냐?");
                    //dialog.setView(etEdit);
                    dialog.setPositiveButton("사진다시찍을래", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dialog.cancel();
                            /**파일삭제**/
                            String s = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + saveFileName;
                            File bitmapFile = new File(s);
                            if (bitmapFile.exists()) {
                                bitmapFile.delete();
                            }
                            /**파일삭제디비에서도**/
                            db.execSQL("DELETE FROM albums WHERE photo = '" + s + "';");
                            Log.d("디비확인", "DELETE FROM albums WHERE photo = '" + s + "';");
                            Log.d("디비확인", s);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        delFile(saveFileName);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                            flag = 0;
                            MainActivity.this.finish();
                            Intent intent = new Intent(MainActivity.this, MainActivity.class);
                            intent.putExtra("album", album);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            //iv.setImageBitmap(null);
                            startActivity(intent);
                        }
                    });
                    // Cancel 버튼 이벤트
                    dialog.setNegativeButton("엘범으로", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //dialog.cancel();
                            new Thread(new Runnable() {
//                            StringBuilder jsonHtml = new StringBuilder();

                                @Override
                                public void run() {
                                    try {
                                        String url = MyApplication.serverIP + "/scanner_Sync.php?username=" + MyApplication.username + "&fileurl=" + temp + "&album=" + album;
//                                    String url = MyApplication.serverIP + "/scanner_Sync.php?username=" + MyApplication.username + "&fileurl=" + temp;
                                        //String url = "http://112.161.86.168:8080/scanner_Submit.php?username=" + username.getText() + "&password=" + password.getText();
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
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    db.close();
                                    helper.close();
                                }
                            }).start();
                            Intent intent = new Intent(MainActivity.this, AlbumsActivity.class);
                            //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                        }
                    });
                    dialog.setNeutralButton("사진더찍을래", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            new Thread(new Runnable() {
//                            StringBuilder jsonHtml = new StringBuilder();

                                @Override
                                public void run() {
                                    try {
                                        String url = MyApplication.serverIP + "/scanner_Sync.php?username=" + MyApplication.username + "&fileurl=" + temp + "&album=" + album;
//                                    String url = MyApplication.serverIP + "/scanner_Sync.php?username=" + MyApplication.username + "&fileurl=" + temp;
                                        //String url = "http://112.161.86.168:8080/scanner_Submit.php?username=" + username.getText() + "&password=" + password.getText();
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
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                            flag = 0;
                            MainActivity.this.finish();
                            Intent intent = new Intent(MainActivity.this, MainActivity.class);
                            intent.putExtra("album", album);
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            //iv.setImageBitmap(null);
                            startActivity(intent);
                        }
                    });
                    dialog.show();
                } else {
                    Toast.makeText(MainActivity.this, "와핑파일 먼저 받아", Toast.LENGTH_SHORT).show();
                }
            }
        });
        /**
         * 명함모드
         * 사진찍고
         * 버튼누르면 이미지와핑한주소+주소록에 저장할 내용 json으로 보내줌
         * 여기에서 데이타 받고 인텐트로 주소록으로 보냄
         * **/
        cardButton = (Button) findViewById(R.id.cardButton);
        cardButton.setOnClickListener(new OnClickListener() {
            String fileurl;
            String name;
            String phone;
            String home;
            String addr;

            @Override
            public void onClick(View view) {
                if (flag == 1) {
                    flag = 0;
                    upLoadServerUri = MyApplication.serverIP + "/scanner_UploadToServer_Card.php";//서버컴퓨터의 ip주소
                    dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    messageText.setText("uploading started.....");
                                }
                            });
                            Log.d("1", "01");
                            Log.d("upLoadServerUri", upLoadServerUri);
                            File fileName = new File("" + mImageCaptureUri);
                            Log.d("1", fileName.getName());
                            /**와핑결과파일출력용
                             * **/
                            uploadFile(mImageCaptureUri.getPath());
                            /**임시파일삭제**/
                            File bitmapFile = new File(mImageCaptureUri.getPath());
                            if (bitmapFile.exists()) {
                                bitmapFile.delete();
                            }
                            /**임시파일삭제**/
                            /**받은 제인 파일 url 정보로 분리
                             * jsonHtml 따로쓰자-마직막 널값문제
                             * **/
                            if (!(jsonHtml.toString().equals("null"))) {
                                try {
                                    int Idx = jsonHtml.toString().lastIndexOf("<br/>");/*<br/>*/
                                    String jsonHtml2 = jsonHtml.toString().substring(Idx + 5, jsonHtml.toString().length());
                                    Log.d("ㅁ느엄", jsonHtml2);
                                    JSONArray jsonArray = new JSONArray(jsonHtml2);
                                    //사실 한줄임
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                                        fileurl = jsonObject.getString("filename");
                                        name = jsonObject.getString("name");
                                        phone = jsonObject.getString("phone");
                                        home = jsonObject.getString("home");
                                        addr = jsonObject.getString("addr");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            /**
                             * 와핑파일 받아오기
                             *
                             * **/
                            try {
//                            temp = " http://192.168.0.11:63327/uploads/(120DPI)alertIcon.png";
                                temp = MyApplication.serverIP + fileurl.substring(1, fileurl.length());
                                saveFileName = _fileName + "_warp." + _fileEx;
                                Log.d("inputcard", temp);
                                URL url = new URL(temp);
                                //"http://112.161.86.168:8080/uploads/"+new File("" + mImageCaptureUri).getName()
                                //  아래 코드는 웹에서 이미지를 가져온 뒤
                                //  이미지 뷰에 지정할 Bitmap을 생성하는 과정
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setDoInput(true);
                                conn.connect();
                                InputStream is = conn.getInputStream();
                                bitmap = BitmapFactory.decodeStream(is);
                                /**비트맵 sd 저장**/
                                int Idx = fileurl.lastIndexOf("/");
                                saveFileName = fileurl.substring(Idx + 1, fileurl.length());
                                String photoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + saveFileName;
                                FileOutputStream out = new FileOutputStream(photoPath);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.close();
                                /**비트맵 sd 저장**/
                                Log.d("/**비트맵 sd 저장**/", photoPath);
                                Uri fromFile = Uri.fromFile(new File(photoPath));
                                Log.d("/**비트맵 sd 저장**/", fromFile.toString());
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //iv.setImageBitmap(bitmap);
                                        //iv.setImageResource(0);
                                        iv.setImageBitmap(null);
                                    }
                                });
                                try {
                                    delFile(saveFileName);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                /**인텐트로 전부다 주소록으로 보내기
                                 * **/
                                /**
                                 * 비트맵에 저장
                                 * 용량맞을때까지 줄이기 못함
                                 * **/
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inSampleSize = 4;
                                Bitmap myBitmap = BitmapFactory.decodeFile(photoPath);
                                Log.d("/**비트맵 sd 저장**/2", String.valueOf(myBitmap.getByteCount()));
//                            while(myBitmap.getByteCount()>40000){
//                                myBitmap = BitmapFactory.decodeFile(photoPath,options);
//                                Log.d("/**비트맵 sd 저장**/1", String.valueOf(myBitmap.getByteCount()));
//                            }
                                myBitmap = BitmapFactory.decodeFile(photoPath, options);
                                Log.d("/**비트맵 sd 저장**/2", String.valueOf(myBitmap.getByteCount()));
                                ByteArrayOutputStream stream = new ByteArrayOutputStream(myBitmap.getWidth() * myBitmap.getHeight());
                                myBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                byte[] byteArray = stream.toByteArray();
                                /**임시파일삭제**/
                                File bitmapFile2 = new File(fromFile.getPath());
                                Log.d("bitmapFile2.exists()", String.valueOf(bitmapFile2.exists()));
                                if (bitmapFile2.exists()) {
                                    bitmapFile2.delete();
                                }
//                            byteArray.length;
                                //intent.putExtra("bm", (Bitmap)b);
                                //Intent contactIntent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT);
//                            Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT, ContactsContract.Contacts.CONTENT_URI);
                                //intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
//                        Bundle bundle = new Bundle();
//                        bundle.putString(ContactsContract.Intents.Insert.NAME, name);
//                        bundle.putString(ContactsContract.Intents.Insert.PHONE, phone);
//                        //bundle.putString(ContactsContract.Intents.Insert.PHONE_TYPE, "01077777777");
//                        bundle.putString(ContactsContract.Intents.Insert.SECONDARY_PHONE, home);
//                        bundle.putString(ContactsContract.Intents.Insert.POSTAL, addr);
//                        //bundle.pu
//                        intent.putExtras(bundle);
//                        //intent.putExtra(ContactsContract.Intents.ATTACH_IMAGE,bitmap);
                                /****/
//                            Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
//                            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
//                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, phone);
//                            //bundle.putString(ContactsContract.Intents.Insert.PHONE_TYPE, "01077777777");
//                            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, home);
//                            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, addr);
//                            //intent.putExtra(ContactsContract.Intents.ATTACH_IMAGE,byteArray);
//                            Uri uri = Uri.parse("file:///storage/emulated/0/YoonScanner/ysc_1470291459182_warp.jpg");
//                            intent.putExtra(ContactsContract.Contacts.Photo.DISPLAY_PHOTO,uri);
//
//                            startActivity(intent);
                                /**
                                 * 작은이미지로 하면 됨
                                 * **/
                                ArrayList<ContentValues> data = new ArrayList<ContentValues>();
                                ContentValues row = new ContentValues();
                                row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
                                row.put(ContactsContract.CommonDataKinds.Photo.PHOTO, byteArray);
                                data.add(row);
                                /****/
//                            ContentValues row1 = new ContentValues();
//                            row1.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
//                            row1.put(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,name);
//                            data.add(row1);
//                            ContentValues row1 = new ContentValues();
//                            row1.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
//                            row1.put(ContactsContract.CommonDataKinds.Organization.COMPANY, "company");
//                            row1.put(ContactsContract.CommonDataKinds.Organization.TITLE, "position");
//                            data.add(row1);
//
//                            ContentValues row2 = new ContentValues();
//                            row2.put(ContactsContract.Contacts.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
//                            row2.put(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_HOME);
//                            row2.put(ContactsContract.CommonDataKinds.Website.URL, "website");
//                            data.add(row2);
//
                                Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
                                Bundle bundle = new Bundle();
                                bundle.putString(ContactsContract.Intents.Insert.NAME, name);
                                bundle.putString(ContactsContract.Intents.Insert.PHONE, phone);
                                bundle.putString(ContactsContract.Intents.Insert.SECONDARY_PHONE, home);
                                bundle.putString(ContactsContract.Intents.Insert.POSTAL, addr);
                                intent.putExtras(bundle);
                                intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
//                            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
//                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, phone);
//                            //bundle.putString(ContactsContract.Intents.Insert.PHONE_TYPE, "01077777777");
//                            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, home);
//                            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, addr);
                                startActivity(intent);
                            } catch (IOException ex) {
                            }
                            dialog.dismiss();
                        }
                    });
                    //String saveFileName;
                    thread.start(); //업로드 스레드 실행
                } else {
                    Toast.makeText(MainActivity.this, "사진안찍었다.사진먼저찍어", Toast.LENGTH_SHORT).show();
                }
            }
        });
        /**
         * up 모드
         * 이미지저장**/
        uploadButton = (Button) findViewById(R.id.uploadButton);
        messageText = (TextView) findViewById(R.id.messageText);
        //messageText.setText("Uploading file path :- '/mnt/sdcard/" + uploadFileName + "'");
        //messageText.setText("Uploading file path :- '/mnt/sdcard/" + String.valueOf(mImageCaptureUri) + "'");
        /************* Php script path ****************/
        //upLoadServerUri = "http://180.64.63.139/UploadToServer.php";//서버컴퓨터의 ip주소
        //upLoadServerUri = "http://112.161.86.168:8080/UploadToServer.php";//서버컴퓨터의 ip주소
        uploadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == 1) {
                    flag = 2;
                    upLoadServerUri = MyApplication.serverIP + "/scanner_UploadToServer.php";//서버컴퓨터의 ip주소
                     dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
//                            runOnUiThread(new Runnable() {
//                                public void run() {
//                                    messageText.setText("uploading started.....");
//                                }
//                            });
                            File fileName = new File("" + mImageCaptureUri);
                            int Idx = fileName.getName().lastIndexOf(".");
                            _fileName = fileName.getName().substring(0, Idx);
                            _fileEx = fileName.getName().substring(Idx + 1, fileName.getName().length());
                            uploadFile(mImageCaptureUri.getPath());
                            /**임시파일삭제**/
                            File bitmapFile = new File(mImageCaptureUri.getPath());
                            if (bitmapFile.exists()) {
                                bitmapFile.delete();
                            }
                            try {
//                                URL url = new URL("http://cfs11.tistory.com/original/35/tistory/2008/11/16/13/00/491f9ac66153b"); // URL 주소를 이용해서 URL 객체 생성
                                //String temp = "http://112.161.86.168:8080/uploads/" + _fileName + "_warp." + _fileEx;
                                temp = MyApplication.serverIP + "/uploads/" + _fileName + "_warp." + _fileEx;
                                saveFileName = _fileName + "_warp." + _fileEx;
                                Log.d("input", temp);
                                URL url = new URL(temp);
                                //"http://112.161.86.168:8080/uploads/"+new File("" + mImageCaptureUri).getName()
                                //  아래 코드는 웹에서 이미지를 가져온 뒤
                                //  이미지 뷰에 지정할 Bitmap을 생성하는 과정
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setDoInput(true);
                                conn.connect();
                                InputStream is = conn.getInputStream();
                                bitmap = BitmapFactory.decodeStream(is);
                            } catch (IOException ex) {
                            }
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    iv.setImageBitmap(bitmap);
                                }
                            });
                            try {
                                String photoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + saveFileName;
                                FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + saveFileName);
                                Log.d("끝내자~~", Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + saveFileName);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.close();
                                /**
                                 * 사진 데이터 디비 저장
                                 * **/
                                helper = new MySQLiteOpenHelper(MainActivity.this, // 현재 화면의 context
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
                                db.execSQL("INSERT INTO albums (album, photo,username) VALUES ('" + album + "','" + photoPath + "', '" + MyApplication.username + "');");
                                Cursor cursor = db.rawQuery("SELECT album,photo FROM ALBUMS", null);
                                if (cursor != null) {
                                    while (cursor.moveToNext()) {
                                        String all = cursor.getString(cursor.getColumnIndex("album"))
                                                + "," + cursor.getString(cursor.getColumnIndex("photo"));
                                        //String photo = cursor.getString(2);
                                        Log.d("데이터베이스", all);
                                    }
                                }
                                //db.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            /**임시파일삭제**/
                            Log.d("1", "01");
                            dialog.dismiss();
                        }
                    });
                    //String saveFileName;
                    thread.start(); //업로드 스레드 실행
                } else {
                    Toast.makeText(MainActivity.this, "사진안찍었다.사진먼저찍어", Toast.LENGTH_SHORT).show();
                }
//                    try {
//                        thread.join();
////                        dialog.dismiss();
//                        Thread mThread = new Thread() {
//                            @Override
//                            public void run() {
////                                try {
//////                                URL url = new URL("http://cfs11.tistory.com/original/35/tistory/2008/11/16/13/00/491f9ac66153b"); // URL 주소를 이용해서 URL 객체 생성
////                                    //String temp = "http://112.161.86.168:8080/uploads/" + _fileName + "_warp." + _fileEx;
////                                    temp = MyApplication.serverIP + "/uploads/" + _fileName + "_warp." + _fileEx;
////                                    saveFileName = _fileName + "_warp." + _fileEx;
////                                    Log.d("input", temp);
////                                    URL url = new URL(temp);
////                                    //"http://112.161.86.168:8080/uploads/"+new File("" + mImageCaptureUri).getName()
////                                    //  아래 코드는 웹에서 이미지를 가져온 뒤
////                                    //  이미지 뷰에 지정할 Bitmap을 생성하는 과정
////                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
////                                    conn.setDoInput(true);
////                                    conn.connect();
////                                    InputStream is = conn.getInputStream();
////                                    bitmap = BitmapFactory.decodeStream(is);
////                                } catch (IOException ex) {
////                                }
//                            }
//                        };
//                        Log.d("1", "1");
//                        mThread.start(); // 웹에서 이미지를 가져오는 작업 스레드 실행.
//                        try {
//                            //  메인 스레드는 작업 스레드가 이미지 작업을 가져올 때까지
//                            //  대기해야 하므로 작업스레드의 join() 메소드를 호출해서
//                            //  메인 스레드가 작업 스레드가 종료될 까지 기다리도록 합니다.
//                            Log.d("1", "2");
//                            mThread.join();
//                            Log.d("1", "3");
//                            //  이제 작업 스레드에서 이미지를 불러오는 작업을 완료했기에
//                            //  UI 작업을 할 수 있는 메인스레드에서 이미지뷰에 이미지를 지정합니다.
////                            iv.setImageBitmap(bitmap);
//                            //new File("" + mImageCaptureUri);
//                            //사진 가져왔음 저장할까요?
//                            /**사진저장
//                             *
//                             * Bitmap  bitmap;
//
//                             public void Bitmap createBitmap(ImageView imageview) {
//                             if (bitmap != null) {
//                             bitmap.recycle();
//                             bitmap = null;
//                             }
//                             bitmap = Bitmap.createBitmap(imageview.getDrawingCache());
//                             // Your Code of bitmap Follows here
//                             }
//                             *
//                             *
//                             * **/
////                            try {
////                                String photoPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + saveFileName;
////                                FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + saveFileName);
////                                Log.d("끝내자~~", Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + saveFileName);
////                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
////                                out.close();
////                                /**
////                                 * 사진 데이터 디비 저장
////                                 * **/
////                                helper = new MySQLiteOpenHelper(MainActivity.this, // 현재 화면의 context
////                                        "albums.db", // 파일명
////                                        null, // 커서 팩토리
////                                        1); // 버전 번호
////                                try {
////                                /*데이터베이스 객체를 얻어오는 다른 간단한 방법
////                                db = openOrCreateDatabase(dbName,  // 데이터베이스파일 이름
////                                        Context.MODE_PRIVATE, // 파일 모드
////                                        null);    // 커서 팩토리
////                                String sql = "create table mytable(id integer primary key autoincrement, name text);";
////                                db.execSQL(sql);*/
////                                    db = helper.getWritableDatabase(); // 읽고 쓸수 있는 DB
////                                    //db = helper.getReadableDatabase(); // 읽기 전용 DB select문
////                                } catch (SQLiteException e) {
////                                    e.printStackTrace();
////                                    Log.d("데이터베이스", "데이터베이스를 얻어올 수 없음");
////                                    finish(); // 액티비티 종료
////                                }
////                                // 1. 데이터 저장
////                                db.execSQL("INSERT INTO albums (album, photo,username) VALUES ('" + album + "','" + photoPath + "', '" + MyApplication.username + "');");
////                                Cursor cursor = db.rawQuery("SELECT album,photo FROM ALBUMS", null);
////                                if (cursor != null) {
////                                    while (cursor.moveToNext()) {
////                                        String all = cursor.getString(cursor.getColumnIndex("album"))
////                                                + "," + cursor.getString(cursor.getColumnIndex("photo"));
////                                        //String photo = cursor.getString(2);
////                                        Log.d("데이터베이스", all);
////                                    }
////                                }
////                                //db.close();
////                            } catch (FileNotFoundException e) {
////                                e.printStackTrace();
////                            } catch (IOException e) {
////                                e.printStackTrace();
////                            }
//                            /**사진저장**/
//                            Log.d("1", String.valueOf(bitmap));
//                        } catch (InterruptedException e) {
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    Toast.makeText(MainActivity.this, "사진안찍었다.사진먼저찍어", Toast.LENGTH_SHORT).show();
//                }
            }
        });
    }

    private void setup() {
        btn = (Button) findViewById(R.id.btn);
        iv = (ImageView) findViewById(R.id.iv);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag == 0) {
                    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    String url = "ysc_" + String.valueOf(System.currentTimeMillis()) + ".jpg";//파일명 정함
                    //String url = "test2.jpg";
                    String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();//절대경로 추출
                    sdPath += "/YoonScanner";//폴더이름
                    //리스트뷰 별로 폴더 생성할까?
                    File file = new File(sdPath);
                    //상위 디렉토리가 존재하지 않을 경우 생성
                    if (!file.exists()) {
                        file.mkdirs();//폴더생성
                    }
                    //mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), url));
                    mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/", url));
                    //파일생성
                    Log.d("사진위치3", String.valueOf(mImageCaptureUri));
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                    intent.putExtra("mImageCaptureUri", mImageCaptureUri.toString());
                    Log.d("사진위치100", intent.getExtras().getString("mImageCaptureUri"));
                    startActivityForResult(intent, 0);
                } else {
                    Toast.makeText(MainActivity.this, "이미사진찍었다", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == 0) {
            flag = 1;
            Log.d("사진위치12", +requestCode + "," + resultCode);
            Log.d("사진위치", "URI=>" + data.getData());
            //Log.d("사진위치100", data.getExtras().getString("mImageCaptureUri"));
            //iv.setImageURI(data.getData());
            //iv.setImageURI(Uri.parse(data.getExtras().getString("mImageCaptureUri")));
            iv.setImageURI(mImageCaptureUri);//file:///storage/emulated/0/tmp_1468987222872.jpg
            //MediaStore.Images.Media.getBitmap(getContentResolver(),)
        }
    }

    public void delFile(String sourcefilename) throws IOException {
        String url = MyApplication.serverIP + "/scanner_Del.php?filename=" + sourcefilename;
        StringBuilder jsonHtml = new StringBuilder();
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
        //return 1;//1성공 0실패
    }

    public int uploadFile(String sourceFileUri) {
        String fileName = sourceFileUri;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);
        if (!sourceFile.isFile()) {//파일아닐때 예외처리
//            dialog.dismiss();
            Log.e("uploadFile", "Source File not exist :"
                    + uploadFilePath + "" + uploadFileName);
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    messageText.setText("Source File not exist :"
//                            + uploadFilePath + "" + uploadFileName);
//                }
//            });
            return 0;
        } else {
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);
                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);
                if (serverResponseCode == 200) {
//                    runOnUiThread(new Runnable() {
//                        public void run() {
//                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
//                                    + uploadFileName;
//                            messageText.setText(msg);
//                            Toast.makeText(MainActivity.this, "File Upload Complete.",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    });
                    /*
                    *
                    * */
                    //StringBuilder jsonHtml = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    while (true) {
                        String line = br.readLine();
                        if (line == null)
                            break;
                        jsonHtml.append(line + "\n");
                    }
                    Log.d("결과받기", jsonHtml.toString());
                    br.close();
                }
                //conn.disconnect();
                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();
//                dialog.dismiss();
                //conn.disconnect();
            } catch (MalformedURLException ex) {
//                dialog.dismiss();
                ex.printStackTrace();
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        messageText.setText("MalformedURLException Exception : check script url.");
//                        Toast.makeText(MainActivity.this, "MalformedURLException",
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                //  dialog.dismiss();
                e.printStackTrace();
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        messageText.setText("Got Exception : see logcat ");
//                        Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
                Log.e("Upload Exception", "Exception : " + e.getMessage(), e);
                //Log.e("Upload file to server Exception", "Exception : "+ e.getMessage(), e);
            }
//            dialog.dismiss();
            return serverResponseCode;
        } // End else block
    }
}