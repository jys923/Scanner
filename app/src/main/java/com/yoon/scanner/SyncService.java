package com.yoon.scanner;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class SyncService extends IntentService {
    String username;
    String spliter = "yoonscanner";
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;
    ArrayList<String> local_DB = new ArrayList<>();
    ArrayList<String> server_DB = new ArrayList<>();
    ArrayList<String> local_DB2 = new ArrayList<>();
    ArrayList<String> server_DB2 = new ArrayList<>();
    ArrayList<String> server_DB_modify_date = new ArrayList<>();
    ArrayList<String> local_DB_modify_date = new ArrayList<>();
    //    ArrayList<_DB> local_DB = new ArrayList<>();
//    ArrayList<_DB> server_DB = new ArrayList<>();
//    ArrayList<_DB> local_DB2 = new ArrayList<>();
//    ArrayList<_DB> server_DB2 = new ArrayList<>();
    StringBuilder jsonHtml = new StringBuilder();
    //_DB al_db = new _DB();
    ArrayList<String> stringArrayList = new ArrayList<>();
    private Handler mHandler = new Handler();

    public SyncService() {
        super("name");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Service Example", "Service Started.. ");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("서비스", "1");
        username = intent.getStringExtra("username");
        //username = MyApplication.username;
        //내 디비 조회

        helper = new MySQLiteOpenHelper(SyncService.this, // 현재 화면의 context
                "albums.db", // 파일명
                null, // 커서 팩토리
                1); // 버전 번호
        db = helper.getWritableDatabase();
        Log.d("PrintData()",helper.PrintData());
        Cursor cursor = db.rawQuery(
                "SELECT photo,album,sqltime " +
                        "FROM albums " +
                        "Where username='" + MyApplication.username + "' " +
                        "order by photo;", null);
        if (cursor != null) {
            //_DB al_db = new _DB();
            while (cursor.moveToNext()) {
                //String photo = cursor.getString(2);
                String photo = cursor.getString(cursor.getColumnIndex("photo"));
                photo = photo.substring(photo.lastIndexOf("/") + 1, photo.length());
                String album = cursor.getString(cursor.getColumnIndex("album"));
                String sqltime = cursor.getString(cursor.getColumnIndex("sqltime"));
                //al_db.setPhoto(photo);
                //al_db.setAlbum(album);
                //Log.d("제이슨 어떻게 들어오냐?4", photo);
                //Log.d("로컬데이터베이스", photo+",,,"+album);
                //local_DB.add(photo);
                //local_DB.add(new _DB(album,photo));
                local_DB.add(album + spliter + photo);
                local_DB_modify_date.add(sqltime);
            }
            //printArraryList(local_DB, "local_DB0");
        }
        //서버에서 디비 받기
        //jsonHtml = new StringBuilder();
        Thread server_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = MyApplication.serverIP + "/scanner_Sync_Select.php?username=" + username;
                    Log.d("데이터베이스", url);
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
                //제이슨 받아서 어레이리스트 삽입
                //if (!jsonHtml.toString().isEmpty()) {
                Log.d("제이슨 결과", jsonHtml.toString());
                if (!(jsonHtml.toString().equals("null"))) {
                    try {
                        //JSONObject jObject = new JSONObject(jsonHtml.toString());
                        //jObject.getString("fileurl");
                        //Log.d("제이슨 어떻게 들어오냐?1",jObject.getString("fileurl"));
                        //JSONArray jsonArray = jObject.getJSONArray("fileurl");
                        JSONArray jsonArray = new JSONArray(jsonHtml.toString());
                        //Log.d("제이슨 어떻게 들어오냐?2", String.valueOf(jsonArray.length()));
                        //_DB al_db = new _DB();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
//                            imgurl = jsonObject.getString("fileurl");
//                            txt1 = jsonObject.getString("txt1");
//                            txt2 = jsonObject.getString("txt2");
                            String fileurl = jsonObject.getString("fileurl");
                            //fileurl.lastIndexOf("/");
                            fileurl = fileurl.substring(fileurl.lastIndexOf("/") + 1, fileurl.length());
                            //al_db.setPhoto(fileurl);
                            String album = jsonObject.getString("album");
                            String modify_date = jsonObject.getString("modify_date");
                            //al_db.setAlbum(album);
                            //Log.d("제이슨 어떻게 들어오냐?3", fileurl);
//                            server_DB.add(new _DB(album,fileurl));
                            server_DB.add(album + spliter + fileurl);
                            server_DB_modify_date.add(modify_date);

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    printArraryList(server_DB, "server_DB0");
                    printArraryList(local_DB, "local_DB");
                }
            }
        });
        //서버 로칼 비교
        Thread local_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();//절대경로 추출
                sdPath += "/YoonScanner";//폴더이름
                //리스트뷰 별로 폴더 생성할까?
                File file = new File(sdPath);
                //상위 디렉토리가 존재하지 않을 경우 생성
                if (!file.exists()) {
                    file.mkdirs();//폴더생성
                }
                //printArraryList(server_DB,"server_DB0");
                //printArraryList(local_DB,"local_DB0");
                server_DB2 = (ArrayList<String>) server_DB.clone();
                local_DB2 = (ArrayList<String>) local_DB.clone();
//                server_DB.retainAll(local_DB);//비교하면 앞에거 여기서 서버 내용달라짐
                //Boolean aBoolean=server_DB2.retainAll(local_DB2);
                //Log.d("비교11111", String.valueOf(aBoolean));
                //if (!server_DB2.retainAll(local_DB2)) {

                    //중복제거
                    server_DB2.removeAll(local_DB);
                    local_DB2.removeAll(server_DB);
                    printArraryList(server_DB2, "server_DB2");
                    printArraryList(local_DB2, "local_DB2");
                /**동기화 시작 조건**/
                if (server_DB2.size()!=0 || local_DB2.size()!=0 ) {
                    mHandler.post(new ToastRunnable("동기화~~~시작"));
                    Log.d("파일생성", String.valueOf(server_DB2.isEmpty()));//비었으면 true 나옴 사이즈 0이면
                    if (!server_DB2.isEmpty()) {
                        Log.d("파일생성", "파일생성1");
                        //서버에 더많음
                        //서버를 지움(X)-나중 물어보기?
                        //서버에서 받아오기(O)
                        //물어보기
                        //일단 받아오기
                        /**서버에서파일 다운**/
                        for (int i = 0; i < server_DB2.size(); i++) {
                            String str = server_DB2.get(i);
                            String[] values = str.split(spliter);
                            String album = values[0];
                            String Photo = values[1];
                            String s = MyApplication.serverIP + "/uploads/" + Photo;
                            //Log.d("서버파일받기 주소맞나",s);
                            try {
                                Log.d("서버파일받기", "파일 있을때");
                                URL url = new URL(s);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setDoInput(true);
                                conn.connect();
                                InputStream is = conn.getInputStream();
                                Bitmap bitmap = BitmapFactory.decodeStream(is);//비트맵으로 받아옴
                                conn.disconnect();
                                String temp = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + Photo;
                                Log.d("서버파일받기", temp);
                                File file2 = new File(temp);
                                if (!file2.exists()) {
                                    //파일생성
                                    FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + Photo);
                                    //압축해서 실제 파일만들기
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                    out.close();
                                    /**로컬디비에 추가**/
                                    //엘범 없음
                                }
                                Log.d("서버에서 받은파일 디비저장", "INSERT INTO albums (album, photo) VALUES ('" + album + "', '" + temp + "');");
                                db.execSQL("INSERT INTO albums (album, photo,username,sqltime) VALUES ('" + album + "', '" + temp + "', '" + MyApplication.username + "', '" + server_DB_modify_date.get(i) + "');");
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    /****/
//                    if (false) {
                    if (!local_DB2.isEmpty()) {
                        Log.d("파일생성", "파일생성");
                        //로컬에 더많음
                        //로컬을 업로드
                        //로컬을 삭제
                        //물어보기
                        //일단 삭제
                        /**파일삭제**/
                        for (int i = 0; i < local_DB2.size(); i++) {
                            String str = local_DB2.get(i);
                            String[] values = str.split(spliter);
                            String album = values[0];
                            String Photo = values[1];
                            String s = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/" + Photo;
                            Log.d("파일삭제", s);
                            File bitmapFile = new File(s);
                            if (bitmapFile.exists()) {
//                                Log.d("삭제대신", s);
                                bitmapFile.delete();
                                /**디비삭제**/
//                                Log.d("삭제대신2", "DELETE FROM albums WHERE photo = '" + s + "';");
//                                db.execSQL("DELETE FROM albums WHERE photo = '" + s + " ' AND album = ' " + album + "';");
//                                Log.d("삭제대신3", "DELETE FROM albums WHERE photo = '" + s + " ' AND album = ' " + album + "';");
                            }
                            db.execSQL("DELETE FROM albums WHERE photo = '" + s + "';");
                        }
                        mHandler.post(new ToastRunnable("동기화끝"));
                    }
                } else {
                    //토스트 동기화필요없음
                }
                /**초기화**/
                local_DB.clear();
                server_DB.clear();
                local_DB2.clear();
                server_DB2.clear();
                /**스레드에서 사용중 스레드에서 닫아야지**/
                db.close();
                helper.close();
                //mHandler.post(new ToastRunnable("동기화끝"));
            }
        });
        server_thread.start();
        try {
            server_thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        local_thread.start();
        try {
            local_thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        builder.setAutoCancel(true);
        //Notification.Builder에게 Notification 제목, 내용, 이미지 등을 설정//////////////////////////////////////
        builder.setSmallIcon(android.R.drawable.ic_dialog_email);//상태표시줄에 보이는 아이콘 모양
        //builder.setTicker("동기화 끝났다"); //알림이 발생될 때 잠시 보이는 글씨
        //상태바를 드래그하여 아래로 내리면 보이는 알림창(확장 상태바)의 아이콘 모양 지정
        //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_input_add));
        builder.setContentTitle("yoon scanner");    //알림창에서의 제목
        builder.setContentText("동기화 완료");   //알림창에서의 글씨
        ///////////////////////////////////////////////////////////////////////////////////////////////////////
        intent = new Intent(this, AlbumsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();   //Notification 객체 생성
        manager.notify(1, notification);             //NotificationManager가 알림(Notification)을 표시
    }

    public void printArraryList(ArrayList<String> arraryList, String name) {
        Log.d(name + "size", String.valueOf(arraryList.size()));
        for (int i = 0; i < arraryList.size(); i++) {
            //arraryList.get(i);
            Log.d(name, arraryList.get(i));
        }
    }

    public void printArraryList2(ArrayList<_DB> arraryList, String name) {
        Log.d(name + "size", String.valueOf(arraryList.size()));
        for (int i = 0; i < arraryList.size(); i++) {
            //arraryList.get(i);
            Log.d(name, arraryList.get(i).getAlbum() + "," + arraryList.get(i).getPhoto());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("Service Example", "Service Destroyed.. ");
//        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
//        builder.setAutoCancel(true);
//        //Notification.Builder에게 Notification 제목, 내용, 이미지 등을 설정//////////////////////////////////////
//        builder.setSmallIcon(android.R.drawable.ic_dialog_email);//상태표시줄에 보이는 아이콘 모양
//        //builder.setTicker("동기화 끝났다"); //알림이 발생될 때 잠시 보이는 글씨
//        //상태바를 드래그하여 아래로 내리면 보이는 알림창(확장 상태바)의 아이콘 모양 지정
//        //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_input_add));
//        builder.setContentTitle("yoon scanner");    //알림창에서의 제목
//        builder.setContentText("동기화 끝났다");   //알림창에서의 글씨
//        ///////////////////////////////////////////////////////////////////////////////////////////////////////
//
//        Intent intent = new Intent(this, AlbumsActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
//        builder.setContentIntent(pendingIntent);
//        Notification notification = builder.build();   //Notification 객체 생성
//        manager.notify(1, notification);             //NotificationManager가 알림(Notification)을 표시
    }

    public class _DB {
        private String album;
        private String photo;

        public _DB() {
        }

        public _DB(String album, String photo) {
            this.album = album;
            this.photo = photo;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }

        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }
    }

    public class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }
    }
}
