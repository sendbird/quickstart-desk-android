package com.sendbird.desk.android.sample.activity;

import static com.sendbird.desk.android.sample.app.MyApplication.getInitState;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.sendbird.desk.android.sample.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        getInitState().observe(this, initState -> {
            if (initState == null) {
                return;
            }
            switch (initState) {
                case FAILED:
                case SUCCEED:
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                case MIGRATING:
                case NONE:
                    break;
            }
        });
    }
}
