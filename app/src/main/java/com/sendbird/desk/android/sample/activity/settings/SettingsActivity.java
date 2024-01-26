package com.sendbird.desk.android.sample.activity.settings;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.snackbar.Snackbar;
import com.sendbird.android.SendbirdChat;
import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.app.Event;
import com.sendbird.desk.android.sample.utils.PrefUtils;

public class SettingsActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> licenseLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    });

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
            actionBar.setHomeAsUpIndicator(ResourcesCompat.getDrawable(getResources(), ta.getResourceId(0, R.drawable.btn_back), null));
            ta.recycle();
        }

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        final SwitchCompat pushSwitch = (SwitchCompat) findViewById(R.id.switch_push_notification);
        pushSwitch.setChecked(PrefUtils.isPushNotificationEnabled());
        pushSwitch.setOnClickListener(v -> {
            String token = PrefUtils.getPushToken();

            if (pushSwitch.isChecked()) {
                if (token != null && token.length() > 0) {
                    progressBar.setVisibility(View.VISIBLE);

                    // Registers device token.
                    SendbirdChat.registerPushToken(token, (pushTokenRegistrationStatus, e) -> {
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
                    SendbirdChat.unregisterPushToken(token, e -> {
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
                    });
                }
            }
        });

        findViewById(R.id.linear_layout_license).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, LicenseActivity.class);
            licenseLauncher.launch(intent);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
