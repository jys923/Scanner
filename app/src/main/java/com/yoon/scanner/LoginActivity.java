package com.yoon.scanner;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;
import com.yoon.scanner.kakao.KakaoSignupActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    private SessionCallback callback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);



        callback = new SessionCallback();                  // 이 두개의 함수 중요함
        Session.getCurrentSession().addCallback(callback);

        final AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.edit);
        ArrayAdapter adapter = new ArrayAdapter(this,
                android.R.layout.simple_dropdown_item_1line, COUNTRIES);
        textView.setAdapter(adapter);


        Spinner spinner = (Spinner)findViewById(R.id.spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MyApplication.serverIP= (String) adapterView.getItemAtPosition(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                MyApplication.serverIP= (String) adapterView.getItemAtPosition(0);
            }
        });
        final EditText username = (EditText) findViewById(R.id.username);
        final EditText password = (EditText) findViewById(R.id.password);

        //저장경로+파일명
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/YoonScanner/");
        if (!file.exists()) {
            file.mkdirs();//폴더생성
        }
        /**
         * 회원가입
         *
         * **/
        Button join = (Button) findViewById(R.id.join);
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, JoinActivity.class);
                startActivity(intent);
            }
        });
        /**
         * 로그인
         *
         * **/
        Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!(textView.getText().toString().trim()).isEmpty()){
                    MyApplication.serverIP="http://"+textView.getText().toString();
                }

                final Thread thLogin = new Thread(new Runnable() {
                    StringBuilder jsonHtml = new StringBuilder();

                    @Override
                    public void run() {
                        try {
                            //MyApplication.serverIP = "http://112.161.86.23:8080";
                            String url = MyApplication.serverIP+"/scanner_Login.php?username=" + username.getText();
                            Log.d("aa",url);
                            //String url = "http://112.161.86.168:8080/scanner_Login.php?username="+username.getText();
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
                         * 받고
                         * **/
                        //show(jsonHtml.toString());
                        Log.d("아이디없음", jsonHtml.toString().trim());
                        if (!jsonHtml.toString().trim().equals("false")) {
                            try {
                                JSONObject jObject = new JSONObject(jsonHtml.toString());
//                            String getusername =jObject.get("username").toString();
                                if (password.getText().toString().equals(jObject.get("password").toString())) {
                                    Log.d("123456", "로그인 성공");
                                    MyApplication.username = jObject.get("username").toString();
                                    Log.d("123456", MyApplication.username);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "로그인 성공", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    //서비스 시작
                                    Intent Service = new Intent(LoginActivity.this, SyncService.class);
                                    Service.putExtra("username", username.getText().toString());
                                    startService(Service);
                                    //다음엑티비티이동
                                    Intent intent = new Intent(LoginActivity.this, AlbumsActivity.class);
                                    //intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    startActivity(intent);
                                } else {
                                    Log.d("123456", "비번틀림");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "비번틀림", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
//                            String getpassword=jObject.get("Password").toString();
                                Log.d("123456", jObject.get("username").toString() + "," + jObject.get("password").toString() + "," + password.getText());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "아이디없음", Toast.LENGTH_SHORT).show();
                                }
                            });
                            Log.d("123456", "아이디없음");
                        }
                    }
                });
                thLogin.start();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getCurrentSession().removeCallback(callback);
    }
    private class SessionCallback implements ISessionCallback {
        @Override
        public void onSessionOpened() {
            redirectSignupActivity();  // 세션 연결성공 시 redirectSignupActivity() 호출
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if(exception != null) {
                Logger.e(exception);
            }
            setContentView(R.layout.activity_login); // 세션 연결이 실패했을때
        }                                            // 로그인화면을 다시 불러옴

    }
    protected void redirectSignupActivity() {       //세션 연결 성공 시 SignupActivity로 넘김
        final Intent intent = new Intent(this, KakaoSignupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    static final String[] COUNTRIES = new String[]{
//            "http://192.168.0.11:63327",
//            "http://192.168.19.129:63327",
//            "http://112.161.86.23:63327",
//            "http://112.161.86.147:63327"
            "192.168.0.11:63327",
            "192.168.19.129:63327",
            "112.161.86.23:63327",
            "112.161.86.147:63327",
            "112.161.86.162:63327"
    };
}
