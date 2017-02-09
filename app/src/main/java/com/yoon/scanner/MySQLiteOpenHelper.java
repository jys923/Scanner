package com.yoon.scanner;
/**
 * Created by Administrator on 2016-07-27.
 */

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
//*sql을 사용하기 위한 제반 클래스
//        *SQLiteOpenHelper는 사용에 도움을 주는 클래스이다.
//        *데이터베이스를 생성하거나 업그레이드 하는기능 또는,오픈하려면 SQLiteOpenHelper 객체를 사용한다.
//        *SQLiteOpenHelper 클래스를 상속받아 구현하려면
//        *
//        *1)생성 메소드:상위 클래스의 생성 메소드를 호출,Activity 등의 Context 인스턴스와
//        *데이터베이스의 이름,커서 팩토리(보통 Null 지정)등을 지정하고,
//        *데이터베이스 스키마 버전을 알려주는 숫자값을 넘겨 준다.
//        *2)onCreate()메소드:SQLiteDatabase를 넘겨 받으며,데이블을 생성하고 초기 데이터를
//        *추가하기에 적당한 위치이다.
//        *3)onUpgrade()메소드:SQLiteDatabase 인스턴스를 넘겨 받으며,현재 스키마 버전과
//        *최신 스키마 버전 번호도 받는다.
//        *
//        *위의 세가지 기능을 사용해야한다.
//

public class MySQLiteOpenHelper extends SQLiteOpenHelper {
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;

    public MySQLiteOpenHelper(Context context, String name,
                              CursorFactory factory, int version) {
        super(context, name, factory, version);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        // SQL 쿼리문은 다음과 같은 형태로도 실행 할 수도 있다.
        // SQLiteOpenHelper 가 최초 실행 되었을 때
//        String sql = "create table albums (" +
//                "_id integer primary key autoincrement, " +
//                "album text, " +
//                "photo text);";
        String sql = "create table albums (" +
                "_id integer primary key autoincrement," +
                "username text," +
                "album text," +
                "sqltime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL," +
                "photo text);";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // db = 적용할 db, old/new 구 버전/신버전
        // TODO Auto-generated method stub
//        *db 버전이 업그레이드 되었을 때 실행되는 메소드
//                * 이 부분은 사용에 조심해야 하는 일이 많이 있다.버전이 1 인 사용자가 2 로 바뀌면
//        *한번의 수정만 하면 되지만 버전이 3 으로 되면 1 인 사용자가 2, 3 을 거쳐야 하고
//                * 2 인 사용자는 3 까지만 거치면 된다.이렇게 증가할 수록 수정할 일이 많아지므로
//        *적절히 사용해야 하며 가능하면 최초 설계 시에 완벽을 기하는 것이 가장 좋을 것이다.
//                * 테스트에서는 기존의 데이터를 모두 지우고 다시 만드는 형태로 하겠다.
//
        String sql = "drop table if exists albums";
        db.execSQL(sql);
        onCreate(db); // 테이블을 지웠으므로 다시 테이블을 만들어주는 과정
    }

    public String PrintData() {
        db = getReadableDatabase();
        String str = "";
        Cursor cursor = db.rawQuery("select * from albums", null);
        while (cursor.moveToNext()) {
            str += cursor.getInt(0)
                    + " : username "
                    + cursor.getString(1)
                    + ", album = "
                    + cursor.getString(2)
                    + ", date = "
                    + cursor.getString(3)
                    + ", photo = "
                    + cursor.getString(4)
//                    + ", modify_date = "
//                    + cursor.getString(4)
//                    + ", create_date = "
//                    + cursor.getString(5)
                    + "\n";
        }
        return str;
//                String sql = "create table albums (" +
//                "_id integer primary key autoincrement," +
//                "username text,album text,photo text," +
//                "modify_date TIMESTAMP ," +
//                "create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";
    }
    // insert

    /*public void insert(String name, int age, String address) {
        //db = helper.getWritableDatabase(); // db 객체를 얻어온다. 쓰기 가능
        ContentValues values = new ContentValues();
        // db.insert의 매개변수인 values가 ContentValues 변수이므로 그에 맞춤
        // 데이터의 삽입은 put을 이용한다.
        values.put("name", name);
        values.put("age", age);
        values.put("address", address);
        db.insert("student", null, values); // 테이블/널컬럼핵/데이터(널컬럼핵=디폴트)
        // tip : 마우스를 db.insert에 올려보면 매개변수가 어떤 것이 와야 하는지 알 수 있다.
    }
    // update

    public void update(String name, int age) {
        //db = helper.getWritableDatabase(); //db 객체를 얻어온다. 쓰기가능
        ContentValues values = new ContentValues();
        values.put("age", age);    //age 값을 수정
        db.update("student", values, "name=?", new String[]{name});

        *//*

         * new String[] {name} 이런 간략화 형태가 자바에서 가능하다

         * 당연하지만, 별도로 String[] asdf = {name} 후 사용하는 것도 동일한 결과가 나온다.

         *//*



        *//*

         * public int update (String table,

         * ContentValues values, String whereClause, String[] whereArgs)

         *//*
    }
    // delete

    public void delete(String name) {
//        db = helper.getWritableDatabase();
        db.delete("student", "name=?", new String[]{name});
        Log.i("db", name + "정상적으로 삭제 되었습니다.");
    }
    // select

    public void select() {
        // 1) db의 데이터를 읽어와서, 2) 결과 저장, 3)해당 데이터를 꺼내 사용
//        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
        Cursor c = db.query("student", null, null, null, null, null, null);



        *//*

         * 위 결과는 select * from student 가 된다. Cursor는 DB결과를 저장한다. public Cursor

         * query (String table, String[] columns, String selection, String[]

         * selectionArgs, String groupBy, String having, String orderBy)

         *//*
        while (c.moveToNext()) {
            // c의 int가져와라 ( c의 컬럼 중 id) 인 것의 형태이다.
            int _id = c.getInt(c.getColumnIndex("_id"));
            String name = c.getString(c.getColumnIndex("name"));
            int age = c.getInt(c.getColumnIndex("age"));
            String address = c.getString(c.getColumnIndex("address"));
            Log.i("db", "id: " + _id + ", name : " + name + ", age : " + age
                    + ", address : " + address);
        }
    }*/
}