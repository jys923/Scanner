package com.yoon.scanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JoinActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        final EditText username = (EditText) findViewById(R.id.username);
        final EditText password = (EditText) findViewById(R.id.password);
        /**
         * 취소
         *
         * **/
        Button cancle = (Button) findViewById(R.id.cancle);
        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(JoinActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        /**
         * 회원가입
         *
         * **/
        Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (username.getText().toString().getBytes().length <= 0 || password.getText().toString().getBytes().length <= 0) {//빈값이 넘어올때의 처리
                    Toast.makeText(JoinActivity.this, "값을 입력하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    final Thread thLogin = new Thread(new Runnable() {
                        StringBuilder jsonHtml = new StringBuilder();
                        @Override
                        public void run() {
                            try {
                                String url =  MyApplication.serverIP+"/scanner_Submit.php?username=" + username.getText() + "&password=" + password.getText();
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
                            /**
                             * 보내고
                             * -------------
                             * 받고
                             * **/
                            //show(jsonHtml.toString());
                            if (jsonHtml.toString().trim().equals("true")) {
                                Log.d("123456", "가입 성공");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "가입 성공", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                //다음엑티비티이동
                                Intent intent = new Intent(JoinActivity.this, LoginActivity.class);
                                startActivity(intent);
                            } else {
                                Log.d("가입실패-아이디중복", jsonHtml.toString() + "," + String.valueOf(jsonHtml));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "가입실패-아이디중복", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                    thLogin.start();
                }
            }
        });
    }
}
