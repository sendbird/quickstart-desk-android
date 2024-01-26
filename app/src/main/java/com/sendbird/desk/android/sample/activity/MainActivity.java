package com.sendbird.desk.android.sample.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.sendbird.android.ConnectionState;
import com.sendbird.android.SendbirdChat;
import com.sendbird.android.handler.CompletionHandler;
import com.sendbird.android.handler.ConnectionHandler;
import com.sendbird.android.params.UserUpdateParams;
import com.sendbird.desk.android.SendBirdDesk;
import com.sendbird.desk.android.Ticket;
import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.activity.inbox.InboxActivity;
import com.sendbird.desk.android.sample.desk.DeskConnectionManager;
import com.sendbird.desk.android.sample.desk.DeskManager;
import com.sendbird.desk.android.sample.utils.PrefUtils;
import com.sendbird.desk.android.sample.utils.SoftInputUtils;

public class MainActivity extends AppCompatActivity {

    private Context mContext;

    private String mUserId;
    private String mUserName;

    private EditText mEditTextEmail;
    private EditText mEditTextUserName;

    private Button mBtnConnect;
    private Button mBtnShowInbox;

    private ProgressBar mProgressBar;

    private SharedPreferences mPref;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});
    private final ActivityResultLauncher<Intent> appSettingLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {});

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        mPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        mUserId = mPref.getString("user_id", "");
        mUserName = mPref.getString("user_name", "");

        mEditTextEmail = ((EditText) findViewById(R.id.etxt_user_id));
        mEditTextEmail.setText(mUserId);
        mEditTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mUserId = s.toString();
            }
        });

        mEditTextUserName = ((EditText) findViewById(R.id.etxt_user_name));
        mEditTextUserName.setText(mUserName);
        mEditTextUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mUserName = s.toString();
            }
        });

        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnConnect.setOnClickListener(view -> {
            if (mBtnConnect.getText().equals("Connect")) {
                SharedPreferences.Editor editor = mPref.edit();
                editor.putString("user_id", mUserId);
                editor.putString("user_name", mUserName);
                editor.commit();

                connect();
            } else {
                disconnect();
            }
        });

        mBtnShowInbox = (Button) findViewById(R.id.btn_inbox);
        mBtnShowInbox.setOnClickListener(view -> showInbox());

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        ((TextView) findViewById(R.id.txt_version)).setText("SendBird Desk SDK v" + SendBirdDesk.getSdkVersion());

        setState(State.DISCONNECTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String permission = Manifest.permission.POST_NOTIFICATIONS;
            if (ContextCompat.checkSelfPermission(this, permission) != PermissionChecker.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                   showPermissionRationalePopup();
                } else {
                    requestPermissionLauncher.launch(permission);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SendbirdChat.getConnectionState() == ConnectionState.OPEN) {
            setState(State.CONNECTED);
            getOpenTicketCount();
        } else {
            setState(State.DISCONNECTED);
        }

        SendbirdChat.addConnectionHandler("connection handler", new ConnectionHandler() {
            @Override
            public void onDisconnected(@NonNull String s) {

            }

            @Override
            public void onConnected(@NonNull String s) {

            }

            @Override
            public void onReconnectStarted() {
                setState(State.CONNECTING);
            }

            @Override
            public void onReconnectSucceeded() {
                setState(State.CONNECTED);
                getOpenTicketCount();
            }

            @Override
            public void onReconnectFailed() {
                setState(State.DISCONNECTED);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SendbirdChat.removeConnectionHandler("connection handler");
    }

    private enum State {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    private void setState(State state) {
        switch (state) {
            case DISCONNECTED:
                mEditTextEmail.setEnabled(true);
                mEditTextUserName.setEnabled(true);
                mBtnConnect.setText("Connect");
                mBtnConnect.setEnabled(true);
                mBtnShowInbox.setEnabled(false);
                mBtnShowInbox.setText("Inbox");
                mProgressBar.setVisibility(View.INVISIBLE);
                break;

            case CONNECTING:
                mBtnConnect.setText("Connecting...");
                mBtnConnect.setEnabled(false);
                mProgressBar.setVisibility(View.VISIBLE);
                break;

            case CONNECTED:
                mEditTextEmail.setEnabled(false);
                mEditTextUserName.setEnabled(false);
                mBtnConnect.setText("Disconnect");
                mBtnConnect.setEnabled(true);
                mBtnShowInbox.setEnabled(true);
                mProgressBar.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void connect() {
        SoftInputUtils.hideSoftKeyboard(mBtnConnect);

        if (mUserId == null || mUserId.replace(" ", "").length() == 0 || !isValidEmail(mUserId.replace(" ", ""))) {
            Toast.makeText(getApplicationContext(), "Please set valid email.", Toast.LENGTH_LONG).show();
            return;
        }

        if (mUserName == null || mUserName.replace(" ", "").length() == 0) {
            Toast.makeText(getApplicationContext(), "Please set valid user name.", Toast.LENGTH_LONG).show();
            return;
        }

        setState(State.CONNECTING);

        final String userId = mUserId;

        DeskConnectionManager.login(userId, (user, e) -> {
            if (e != null) {
                setState(State.DISCONNECTED);
                Toast.makeText(getApplicationContext(), e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            UserUpdateParams params = new UserUpdateParams();
            params.setNickname(mUserName);
            SendbirdChat.updateCurrentUserInfo(params, (CompletionHandler) e1 -> {
                setState(State.CONNECTED);

                if (e1 != null) {
                    Toast.makeText(getApplicationContext(), e1.getCode() + ": " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                PrefUtils.setUserId(mUserId);
                getOpenTicketCount();
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }

                    DeskManager.updatePushToken(task.getResult());
                });
            });
        });
    }

    private void disconnect() {
        DeskConnectionManager.logout(() -> setState(State.DISCONNECTED));
    }

    private void showInbox() {
        Intent intent = new Intent(mContext, InboxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void getOpenTicketCount() {
        Ticket.getOpenCount((count, e) -> {
            if (e != null) {
                Toast.makeText(getApplicationContext(), e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            mBtnShowInbox.setText("Inbox (" + count + ")");
        });
    }

    private boolean isValidEmail(CharSequence email) {
        return email != null && email.length() > 0 && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showPermissionRationalePopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required")
                .setMessage("You need to allow this permission to use push notifications.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setData(Uri.parse("package:$packageName"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    appSettingLauncher.launch(intent);
                })
                .create()
                .show();
    }
}
