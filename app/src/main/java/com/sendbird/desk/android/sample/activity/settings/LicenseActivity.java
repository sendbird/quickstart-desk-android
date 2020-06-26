package com.sendbird.desk.android.sample.activity.settings;

import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sendbird.desk.android.sample.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class LicenseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.desk_license);
            actionBar.setDisplayHomeAsUpEnabled(true);

            TypedArray ta = obtainStyledAttributes(new int[]{R.attr.deskNavigationIcon});
            actionBar.setHomeAsUpIndicator(getResources().getDrawable(ta.getResourceId(0, R.drawable.btn_back)));
            ta.recycle();
        }

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected String doInBackground(Void... params) {
                return loadLicenseTxtFile();
            }

            @Override
            protected void onPostExecute(String s) {
                progressBar.setVisibility(View.INVISIBLE);
                ((TextView)findViewById(R.id.text_view_license_details)).setText(s);
            }
        }.execute();
    }

    private String loadLicenseTxtFile() {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("LICENSE.txt")));

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
