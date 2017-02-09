package com.yoon.scanner;

import android.app.Application;
import android.app.ProgressDialog;

/**
 * Created by Administrator on 2016-07-26.
 */
public class MyApplication extends Application {
    public static String username=null;
    public static String album=null;
    public static ProgressDialog progressDialogforactive;
    public static Thread threadforactive;
    //MyApplication.username = jObject.get("username").toString();
    //public static String user_nick;
    //public static String albums;
    public static String serverIP = "http://192.168.0.11:63327";
    //public static String serverIP = "http://192.168.19.129:63327";
    //112.161.86.23:8080

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

