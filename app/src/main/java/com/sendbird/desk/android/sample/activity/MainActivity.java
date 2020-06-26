package com.sendbird.desk.android.sample.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBtnConnect.getText().equals("Connect")) {
                    SharedPreferences.Editor editor = mPref.edit();
                    editor.putString("user_id", mUserId);
                    editor.putString("user_name", mUserName);
                    editor.commit();

                    connect();
                } else {
                    disconnect();
                }
            }
        });

        mBtnShowInbox = (Button) findViewById(R.id.btn_inbox);
        mBtnShowInbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInbox();
            }
        });

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        ((TextView) findViewById(R.id.txt_version)).setText("SendBird Desk SDK v" + SendBirdDesk.getSdkVersion());

        setState(State.DISCONNECTED);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN) {
            setState(State.CONNECTED);
            getOpenTicketCount();
        } else {
            setState(State.DISCONNECTED);
        }

        SendBird.addConnectionHandler("connection handler", new SendBird.ConnectionHandler() {
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
        SendBird.removeConnectionHandler("connection handler");
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

        DeskConnectionManager.login(userId, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                if (e != null) {
                    setState(State.DISCONNECTED);
                    Toast.makeText(getApplicationContext(), e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                SendBird.updateCurrentUserInfo(mUserName, "", new SendBird.UserInfoUpdateHandler() {
                    @Override
                    public void onUpdated(SendBirdException e) {
                        setState(State.CONNECTED);

                        if (e != null) {
                            Toast.makeText(getApplicationContext(), e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        PrefUtils.setUserId(mUserId);
                        getOpenTicketCount();
                        DeskManager.updatePushToken(FirebaseInstanceId.getInstance().getToken());
                    }
                });
            }
        });
    }

    private void disconnect() {
        DeskConnectionManager.logout(new SendBird.DisconnectHandler() {
            @Override
            public void onDisconnected() {
                setState(State.DISCONNECTED);
            }
        });
    }

    private void showInbox() {
        Intent intent = new Intent(mContext, InboxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void getOpenTicketCount() {
        Ticket.getOpenCount(new Ticket.GetOpenCountHandler() {
            @Override
            public void onResult(int count, SendBirdException e) {
                if (e != null) {
                    Toast.makeText(getApplicationContext(), e.getCode() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                mBtnShowInbox.setText("Inbox (" + count + ")");
            }
        });
    }

    private boolean isValidEmail(CharSequence email) {
        return email != null && email.length() > 0 && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
