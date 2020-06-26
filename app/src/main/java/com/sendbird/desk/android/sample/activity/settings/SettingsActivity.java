package com.sendbird.desk.android.sample.activity.settings;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.app.Event;
import com.sendbird.desk.android.sample.utils.PrefUtils;

public class SettingsActivity extends AppCompatActivity {

    static final int REQUEST_EXIT = 0xf0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.desk_settings);
            actionBar.setDisplayHomeAsUpEnabled(true);

            TypedArray ta = obtainStyledAttributes(new int[]{R.attr.deskNavigationIcon});
            actionBar.setHomeAsUpIndicator(getResources().getDrawable(ta.getResourceId(0, R.drawable.btn_back)));
            ta.recycle();
        }

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        final SwitchCompat pushSwitch = (SwitchCompat) findViewById(R.id.switch_push_notification);
        pushSwitch.setChecked(PrefUtils.isPushNotificationEnabled());
        pushSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String token = PrefUtils.getPushToken();

                if (pushSwitch.isChecked()) {
                    if (token != null && token.length() > 0) {
                        progressBar.setVisibility(View.VISIBLE);

                        // Registers device token.
                        SendBird.registerPushTokenForCurrentUser(token, new SendBird.RegisterPushTokenWithStatusHandler() {
                            @Override
                            public void onRegistered(SendBird.PushTokenRegistrationStatus pushTokenRegistrationStatus, SendBirdException e) {
                                progressBar.setVisibility(View.INVISIBLE);
                                if (e != null) {
                                    // Sets switch as disabled again.
                                    pushSwitch.setChecked(false);
                                    Snackbar.make(
                                            findViewById(R.id.coordinator_layout),
                                            R.string.desk_push_notification_on_err,
                                            Snackbar.LENGTH_SHORT
                                    ).show();

                                    PrefUtils.setPushNotification(false);
                                    return;
                                }

                                PrefUtils.setPushNotification(true);
                                Event.onEvent(Event.EventListener.SETTINGS_PUSH_ON, null);
                            }
                        });
                    } else {
                        // Disables switch immediately if token is invalid.
                        pushSwitch.setChecked(false);
                        Snackbar.make(
                                findViewById(R.id.coordinator_layout),
                                R.string.desk_push_notification_on_err,
                                Snackbar.LENGTH_SHORT
                        ).show();

                        PrefUtils.setPushNotification(false);
                    }
                } else {
                    if (token != null && token.length() > 0) {
                        progressBar.setVisibility(View.VISIBLE);

                        // Unregisters device token.
                        SendBird.unregisterPushTokenForCurrentUser(token, new SendBird.UnregisterPushTokenHandler() {
                            @Override
                            public void onUnregistered(SendBirdException e) {
                                progressBar.setVisibility(View.INVISIBLE);

                                if (e != null) {
                                    pushSwitch.setChecked(true);
                                    Snackbar.make(
                                            findViewById(R.id.coordinator_layout),
                                            R.string.desk_push_notification_off_err,
                                            Snackbar.LENGTH_SHORT
                                    ).show();

                                    PrefUtils.setPushNotification(true);
                                    return;
                                }

                                PrefUtils.setPushNotification(false);
                                Event.onEvent(Event.EventListener.SETTINGS_PUSH_OFF, null);
                            }
                        });
                    }
                }
            }
        });

        findViewById(R.id.linear_layout_license).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, LicenseActivity.class);
                startActivityForResult(intent, REQUEST_EXIT);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Event.onEvent(Event.EventListener.SETTINGS_ENTER, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Event.onEvent(Event.EventListener.SETTINGS_EXIT, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EXIT) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK);
                finish();
            }
        }
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
