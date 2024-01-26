package com.sendbird.desk.android.sample.activity.chat;

import static com.sendbird.desk.android.sample.desk.DeskManager.CONNECTION_HANDLER_ID_CHAT;
import static com.sendbird.desk.android.sample.desk.DeskManager.TICKET_HANDLER_ID_CHAT;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.sendbird.android.SendbirdChat;
import com.sendbird.android.channel.BaseChannel;
import com.sendbird.android.channel.GroupChannel;
import com.sendbird.android.channel.MessageTypeFilter;
import com.sendbird.android.exception.SendbirdException;
import com.sendbird.android.handler.FileMessageHandler;
import com.sendbird.android.message.AdminMessage;
import com.sendbird.android.message.BaseMessage;
import com.sendbird.android.message.FileMessage;
import com.sendbird.android.message.Thumbnail;
import com.sendbird.android.message.ThumbnailSize;
import com.sendbird.android.message.UserMessage;
import com.sendbird.android.params.FileMessageCreateParams;
import com.sendbird.android.params.MessageListParams;
import com.sendbird.android.user.Sender;
import com.sendbird.android.user.User;
import com.sendbird.desk.android.FAQData;
import com.sendbird.desk.android.SendBirdDesk;
import com.sendbird.desk.android.Ticket;
import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.activity.inbox.InboxActivity;
import com.sendbird.desk.android.sample.app.Event;
import com.sendbird.desk.android.sample.desk.DeskAdminMessage;
import com.sendbird.desk.android.sample.desk.DeskConnectionManager;
import com.sendbird.desk.android.sample.desk.DeskManager;
import com.sendbird.desk.android.sample.desk.DeskUserRichMessage;
import com.sendbird.desk.android.sample.desk.UrlPreviewInfo;
import com.sendbird.desk.android.sample.utils.DateUtils;
import com.sendbird.desk.android.sample.utils.FileUtils;
import com.sendbird.desk.android.sample.utils.PrefUtils;
import com.sendbird.desk.android.sample.utils.image.ImageUtils;
import com.sendbird.desk.android.sample.utils.web.BetterLinkMovementMethod;
import com.sendbird.desk.android.sample.utils.web.WebUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    public static final String EXTRA_USER_NAME = "EXTRA_USER_NAME";
    public static final String EXTRA_CHANNEL_URL = "EXTRA_CHANNEL_URL";

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE_UPLOAD = 0xf0;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE_DOWNLOAD = 0xf1;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE_CAMERA = 0xf2;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE_UPLOAD_VIDEO = 0xf3;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE_RECORD_VIDEO = 0xf4;

    private static final int FILE_TYPE_ALL = 0;
    private static final int FILE_TYPE_IMAGE = 1;
    private static final int FILE_TYPE_VIDEO = 2;

    private View mView;

    private ProgressBar mProgressBar;

    private ListView mListView;
    private MessageListAdapter mListAdapter;
    private boolean mIsScrolling = false;

    private ViewGroup mLayoutReply;
    private EditText mEditTxtReply;
    private ImageButton mBtnAttach;
    private ImageButton mBtnSend;
    private ProgressBar mUploadingProgressBar;

    private InputMethodManager mInputMethodManager;
    private BottomSheetDialog mBottomSheetDialog;

    private Ticket mTicket;
    private String mChannelUrl;

    private GroupChannel mChannel;

    private long mMinMessageTimestamp;
    private boolean mLoading;
    private boolean mHasPrev;

    private Uri mTempPhotoUri;
    private Uri mTempVideoUri;
    private Uri mTempFileUri;

    private BaseMessage mUrlPreviewTempMessage;

    private String mTitle;
    private String mUserName;

    // Registers a photo picker activity launcher in single-select mode.
    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                // If user has successfully chosen the image, show a dialog to confirm upload.
                if (uri == null) {
                    return;
                }

                Hashtable<String, Object> info = FileUtils.getFileInfo(ChatActivity.this, uri);
                if (info != null) {
                    String mime = (String)info.get("mime");
                    if (mime != null && mime.toLowerCase().contains("gif")) {
                        sendFileMessage(uri, FILE_TYPE_ALL);
                        return;
                    }
                }

                sendFileMessage(uri, FILE_TYPE_IMAGE);
            });

    // Registers a video picker activity launcher in single-select mode.
    private final ActivityResultLauncher<PickVisualMediaRequest> pickVideo =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) {
                    return;
                }

                sendFileMessage(uri, FILE_TYPE_VIDEO);
            });

    private final ActivityResultLauncher<Intent> takeCameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        int resultCode = result.getResultCode();

        if (resultCode != RESULT_OK) return;

        sendFileMessage(mTempPhotoUri, FILE_TYPE_IMAGE);
    });

    private final ActivityResultLauncher<Intent> takeVideoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        int resultCode = result.getResultCode();

        if (resultCode != RESULT_OK) return;

        sendFileMessage(mTempVideoUri, FILE_TYPE_VIDEO);
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mInputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        initBottomSheetDialog();

        // Starts from startChat.
        mTitle = getIntent().getStringExtra(EXTRA_TITLE);
        mUserName = getIntent().getStringExtra(EXTRA_USER_NAME);

        // Starts from inbox or push. Title is also passed by EXTRA_TITLE.
        mChannelUrl = getIntent().getStringExtra(EXTRA_CHANNEL_URL);

        if (savedInstanceState != null) {
            mChannelUrl = savedInstanceState.getString(EXTRA_CHANNEL_URL);
        }

        // Sets up toolbar.
        setToolbar();

        // Sets up UI.
        setUI();
    }

    private void setToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (mTitle != null) {
                actionBar.setTitle(mTitle);
            } else if (mTicket != null) {
                actionBar.setTitle(mTicket.getTitle());
            } else {
                actionBar.setTitle("");
            }

            actionBar.setDisplayHomeAsUpEnabled(true);

            TypedArray ta = obtainStyledAttributes(new int[]{R.attr.deskInboxIcon});
            actionBar.setHomeAsUpIndicator(ResourcesCompat.getDrawable(getResources(), ta.getResourceId(0, R.drawable.btn_inbox), null));
            ta.recycle();
        }
    }

    private void setUI() {
        mView = findViewById(R.id.layout_root);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mListView = (ListView) findViewById(R.id.list_view);
        mLayoutReply = (ViewGroup) findViewById(R.id.layout_reply);
        mEditTxtReply = (EditText) findViewById(R.id.etxt_reply);
        mBtnAttach = (ImageButton) findViewById(R.id.btn_attach);
        mBtnSend = (ImageButton) findViewById(R.id.btn_send);
        mUploadingProgressBar = (ProgressBar) findViewById(R.id.uploading_progress_bar);

        mProgressBar.setVisibility(View.VISIBLE);
        mLayoutReply.setVisibility(View.GONE);
        mUploadingProgressBar.setVisibility(View.INVISIBLE);
        mBtnSend.setVisibility(View.INVISIBLE);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    mIsScrolling = false;
                    if (view.getFirstVisiblePosition() == 0 && view.getChildCount() > 0 && view.getChildAt(0).getTop() == 0) {
                        loadPreviousMessages(false, null);
                    }
                } else {
                    mIsScrolling = true;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        mEditTxtReply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Shows or hides send button and sends or stops typing command.
                mBtnSend.setEnabled(s.length() > 0);
                if (mBtnSend.isEnabled()) {
                    mBtnSend.setVisibility(View.VISIBLE);
                } else {
                    mBtnSend.setVisibility(View.INVISIBLE);
                }

                if (mChannel != null) {
                    if (s.length() == 1) {
                        mChannel.startTyping();
                    } else if (s.length() <= 0) {
                        mChannel.endTyping();
                    }
                }
            }
        });
        mEditTxtReply.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                // Sends message.
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    String userInput = mEditTxtReply.getText().toString();

                    if (userInput.length() == 0) {
                        return true;
                    }

                    sendUserMessage(userInput);
                    mEditTxtReply.setText("");

                    if (mChannel != null) {
                        mChannel.endTyping();
                    }
                }
                return true; // Do not hide keyboard.
            }
            return false;
        });

        mBtnSend.setOnClickListener(v -> {
            // Sends message.
            String userInput = mEditTxtReply.getText().toString();

            if (userInput.length() == 0) {
                return;
            }

            sendUserMessage(userInput);
            mEditTxtReply.setText("");

            if (mChannel != null) {
                mChannel.endTyping();
            }
        });

        mBtnAttach.setOnClickListener(v -> mBottomSheetDialog.show());
    }

    private void initBottomSheetDialog() {
        mBottomSheetDialog = new BottomSheetDialog(ChatActivity.this);
        View view = getLayoutInflater().inflate(R.layout.fragment_content_chooser, null);

        (view.findViewById(R.id.layout_gallery)).setOnClickListener(v -> {
            requestMedia();
            mBottomSheetDialog.dismiss();
        });

        (view.findViewById(R.id.layout_camera)).setOnClickListener(v -> {
            requestCamera();
            mBottomSheetDialog.dismiss();
        });

        (view.findViewById(R.id.layout_upload_video)).setOnClickListener(v -> {
            requestToUploadVideo();
            mBottomSheetDialog.dismiss();
        });

        (view.findViewById(R.id.layout_take_video)).setOnClickListener(v -> {
            requestToRecordVideo();
            mBottomSheetDialog.dismiss();
        });
        mBottomSheetDialog.setContentView(view);
    }

    void closeSoftKeyboard() {
        mInputMethodManager.hideSoftInputFromWindow(mEditTxtReply.getWindowToken(), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mTicket != null) {
            Map<String, String> data = new HashMap<>();
            data.put("title", mTicket.getTitle());
            data.put("status", mTicket.getStatus2());
            data.put("ticket_id", String.valueOf(mTicket.getId()));
            Event.onEvent(Event.EventListener.CHAT_ENTER, data);
        }

        DeskManager.getInstance().handlePushNotification(false);

        DeskManager.addTicketHandler(TICKET_HANDLER_ID_CHAT, new DeskManager.TicketHandler() {
            @Override
            public void onMessageReceived(@NonNull BaseChannel baseChannel, @NonNull BaseMessage baseMessage) {
                if (mChannelUrl != null && mChannelUrl.equals(baseChannel.getUrl())) {
                    // If the first message comes (This must be welcome or away message).
                    if (mListAdapter == null) {
                        mListAdapter = new MessageListAdapter(ChatActivity.this);
                        mListView.setAdapter(mListAdapter);

                        mProgressBar.setVisibility(View.INVISIBLE);
                        mLayoutReply.setVisibility(View.VISIBLE);
                    }

                    if (mChannel != null) {
                        mChannel.markAsRead(null);
                    }

                    if (DeskAdminMessage.is(baseMessage)) {
                        // If ticket close event is triggered, disables user interaction on reply view.
                        if (DeskAdminMessage.isCloseType(baseMessage)) {
                            mLayoutReply.setVisibility(View.GONE);
                        }
                    } else {
                        // Shows non-system event message only.
                        mListAdapter.appendMessage(baseMessage);
                        mListAdapter.notifyDataSetChanged();
                    }
                } else {
                    if (PrefUtils.isPushNotificationEnabled()) {
                        // Push is handled on ChatActivity for non-my channel messages.
                        DeskManager.showPushNotification(ChatActivity.this, baseChannel, baseMessage);
                    }
                }
            }

            @Override
            public void onMessageUpdated(@NonNull BaseChannel channel, @NonNull BaseMessage message) {
                if (channel.getUrl().equals(mChannelUrl)) {
                    mListAdapter.replaceMessage(message);
                    mListAdapter.notifyDataSetChanged();
                }
            }
        });

        DeskConnectionManager.addConnectionManagementHandler(CONNECTION_HANDLER_ID_CHAT, reconnect -> refresh());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Event.onEvent(Event.EventListener.CHAT_EXIT, null);
        DeskManager.getInstance().handlePushNotification(true);

        DeskManager.removeTicketHandler(TICKET_HANDLER_ID_CHAT);
        DeskConnectionManager.removeConnectionManagementHandler(CONNECTION_HANDLER_ID_CHAT);

        if (mChannel != null) {
            mChannel.endTyping();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, InboxActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(EXTRA_CHANNEL_URL, mChannelUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE_UPLOAD:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestMedia();
                }
                break;

            case PERMISSION_WRITE_EXTERNAL_STORAGE_DOWNLOAD:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mFileMessageToDownload == null) return;
                    showDownloadConfirmDialog(mFileMessageToDownload);
                    mFileMessageToDownload = null;
                }
                break;

            case PERMISSION_WRITE_EXTERNAL_STORAGE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestCamera();
                }
                break;

            case PERMISSION_WRITE_EXTERNAL_STORAGE_UPLOAD_VIDEO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestToUploadVideo();
                }
                break;

            case PERMISSION_WRITE_EXTERNAL_STORAGE_RECORD_VIDEO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestToRecordVideo();
                }
                break;
        }
    }

    private void createTicket(String ticketTitle, String userName) {
        Ticket.create(ticketTitle, userName, (ticket, e) -> {
            if (e != null) {
                mProgressBar.setVisibility(View.INVISIBLE);
                Snackbar.make(mView, R.string.desk_failed_opening_ticket, Snackbar.LENGTH_SHORT).show();
                return;
            }

            mTicket = ticket;
            mChannel = ticket.getChannel();
            mChannelUrl = ticket.getChannel().getUrl();

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(mTicket.getTitle());
            }

            if (mTicket != null) {
                Map<String, String> data = new HashMap<>();
                data.put("title", mTicket.getTitle());
                data.put("status", mTicket.getStatus2());
                data.put("ticket_id", String.valueOf(mTicket.getId()));
                Event.onEvent(Event.EventListener.CHAT_ENTER, data);
            }
        });
    }

    private void refresh() {
        if (mTicket == null && mChannelUrl == null) {
            // Starts from startChat
            createTicket(mTitle, mUserName);
        } else if (mTicket != null) {
            mTicket.refresh((ticket, e) -> {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }

                doRefresh();
            });
        } else {
            Ticket.getByChannelUrl(mChannelUrl, (ticket, e) -> {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }

                mTicket = ticket;
                mChannel = ticket.getChannel();
                mChannelUrl = ticket.getChannel().getUrl();

                mListAdapter = new MessageListAdapter(ChatActivity.this);
                mListView.setAdapter(mListAdapter);

                doRefresh();
            });
        }
    }

    private void doRefresh() {
        if (mTicket != null) {
            if (DeskManager.isTicketClosed(mTicket)) {
                mLayoutReply.setVisibility(View.GONE);
            } else {
                mLayoutReply.setVisibility(View.VISIBLE);
            }
        }

        if (mChannel != null) {
            mChannel.markAsRead(null);
        }

        loadPreviousMessages(true, e -> {
            if (e != null) {
                e.printStackTrace();
            }
        });
    }

    interface LoadPreviousMessagesHandler {
        void onResult(SendbirdException e);
    }

    private void loadPreviousMessages(final boolean refresh, final LoadPreviousMessagesHandler handler) {
        if (refresh) {
            mMinMessageTimestamp = Long.MAX_VALUE;
            mLoading = false;
            mHasPrev = true;
        }

        if (mLoading || !mHasPrev) {
            return;
        }

        mLoading = true;

        if (mChannel != null) {
            MessageListParams params = new MessageListParams();
            params.setPreviousResultSize(30);
            params.setInclusive(false);
            params.setReverse(false);
            params.setMessageTypeFilter(MessageTypeFilter.ALL);
            mChannel.getMessagesByTimestamp(mMinMessageTimestamp, params, (list, e) -> {
                mLoading = false;
                mProgressBar.setVisibility(View.INVISIBLE);

                if (e != null) {
                    Snackbar.make(mView, R.string.desk_failed_loading_messages, Snackbar.LENGTH_SHORT).show();

                    if (handler != null) {
                        handler.onResult(e);
                    }
                    return;
                }

                if (list == null) return;

                if (list.size() == 0) {
                    mHasPrev = false;
                }

                if (refresh) {
                    for (BaseMessage message : mListAdapter.mMessageList) {
                        if (mListAdapter.isTempMessage(message) || mListAdapter.isFailedMessage(message)) {
                            list.add(message);
                        }
                    }

                    mListAdapter.clear();
                }

                int count = 0;
                for (int i = list.size() - 1; i >= 0; i--) {
                    BaseMessage message = list.get(i);
                    if (DeskAdminMessage.is(message)) {
                        continue;
                    }
                    mListAdapter.insertMessage(list.get(i));
                    count++;
                }
                mListAdapter.notifyDataSetChanged();

                if (count > 0) {
                    mMinMessageTimestamp = list.get(0).getCreatedAt();
                    mListView.setSelection(count - 1);
                }

                if (handler != null) {
                    handler.onResult(null);
                }
            });
        }
    }

    private void scrollToBottom() {
        try {
            mListView.post(() -> mListView.setSelection(mListAdapter.getCount() - 1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addTempMessage(BaseMessage tempMessage) {
        mListAdapter.addMessage(tempMessage);
        mListAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void updateUserMessageWithUrl(final BaseMessage message, final String text, String url) {
        if (mChannel != null) {
            mUrlPreviewTempMessage = message;

            DeskUserRichMessage.updateUserMessageWithUrl(mChannel, message, text, url, (userMessage, e) -> {
                mUrlPreviewTempMessage = null;

                if (e != null) {
                    mListAdapter.notifyDataSetChanged();
                    return;
                }

                mListAdapter.replaceMessage(userMessage);
                mListAdapter.notifyDataSetChanged();

                mChannel.markAsRead(null);
            });
        }
    }

    private void sendUserMessage(final String text) {
        if (mChannel != null) {
            UserMessage tempMessage = mChannel.sendUserMessage(text, (userMessage, e) -> {
                if (e != null) {
                    if (userMessage != null) {
                        mListAdapter.markMessageFailed(userMessage.getRequestId());
                    }
                    return;
                }

                Map<String, String> data = new HashMap<>();
                if (userMessage != null) {
                    data.put("message", userMessage.getMessage());
                }
                Event.onEvent(Event.EventListener.CHAT_SEND_USER_MESSAGE, data);

                mListAdapter.markMessageSent(userMessage);

                List<String> urls = WebUtils.extractUrls(text);
                if (urls.size() > 0) {
                    updateUserMessageWithUrl(userMessage, text, urls.get(0));
                }
            });
            addTempMessage(tempMessage);
        }
    }

    private void sendFileMessage(Uri uri, int fileType) {
        if (mChannel != null) {
            Hashtable<String, Object> info = FileUtils.getFileInfo(ChatActivity.this, uri);

            if (info == null) {
                Snackbar.make(mView, R.string.desk_failed_extracting_file_info, Snackbar.LENGTH_SHORT).show();
                return;
            }

            String path = (String) info.get("path");
            String mime = (String) info.get("mime");
            File file;
            String name;
            int size = 0;

            if (path == null || path.length() == 0) {
                if (mTempFileUri != null) {
                    uri = mTempFileUri;
                    path = uri.getPath();
                }
            }

            if (fileType == FILE_TYPE_IMAGE && (mime != null && !mime.startsWith("image"))) {
                mime = "image/jpeg";
            } else if (fileType == FILE_TYPE_VIDEO && (mime != null && !mime.startsWith("video"))) {
                mime = "video/mp4"; // "application/octet-stream"
            } else if (fileType == FILE_TYPE_ALL) {
                if (path != null) {
                    if (path.endsWith(".mp4")) {
                        mime = "video/mp4";
                    } else if (path.endsWith(".jpg")) {
                        mime = "image/jpg";
                    } else if (path.endsWith(".gif")) {
                        mime = "image/gif";
                    } else if (path.endsWith(".png")) {
                        mime = "image/png";
                    }
                }
            }

            if (path == null) {
                Snackbar.make(mView, R.string.desk_failed_extracting_file_info, Snackbar.LENGTH_SHORT).show();
            } else {
                showUploadProgress(true);

                try {
                    if (mime != null && mime.toLowerCase().contains("gif")) {
                        file = new File(path);
                        name = file.getName();
                        Object obj = info.get("size");
                        if (obj != null) size = (int) obj;
                    } else if (mime != null && mime.startsWith("image")) {
                        file = File.createTempFile("desk", ".jpg");
                        name = file.getName();
                        Bitmap bitmap = getResizedBitmap(uri);
                        if (bitmap != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, new BufferedOutputStream(new FileOutputStream(file)));
                        }
                        size = (int) file.length();

                        ExifInterface oriExif = new ExifInterface(path);
                        String oriOrientation = oriExif.getAttribute(ExifInterface.TAG_ORIENTATION);

                        ExifInterface newExif = new ExifInterface(file.getAbsolutePath());
                        newExif.setAttribute(ExifInterface.TAG_ORIENTATION, oriOrientation);
                        newExif.saveAttributes();
                    } else if (mime != null && mime.startsWith("video")) {
                        file = new File(path);
                        name = file.getName();
                        Object obj = info.get("size");
                        if (obj != null) size = (int) obj;
                    } else {
                        file = new File(path);
                        name = file.getName();
                        Object obj = info.get("size");
                        if (obj != null) size = (int) obj;
                    }

                    // Specify two dimensions of thumbnails to generate
                    List<ThumbnailSize> thumbnailSizes = new ArrayList<>();
                    thumbnailSizes.add(new ThumbnailSize(240, 240));
                    thumbnailSizes.add(new ThumbnailSize(320, 320));

                    // Send image with thumbnails in the specified dimensions
                    FileMessageCreateParams params = new FileMessageCreateParams();
                    params.setFile(file);
                    params.setFileName(name);
                    params.setMimeType(mime);
                    params.setFileSize(size);
                    params.setThumbnailSizes(thumbnailSizes);
                    FileMessage tempMessage = mChannel.sendFileMessage(params, (FileMessageHandler) (fileMessage, e) -> {
                        showUploadProgress(false);
                        if (e != null) {
                            if (fileMessage != null) mListAdapter.markMessageFailed(fileMessage.getRequestId());
                            return;
                        }

                        if (fileMessage == null) return;
                        Map<String, String> data = new HashMap<>();
                        data.put("file_name", fileMessage.getName());
                        data.put("file_size", String.valueOf(fileMessage.getSize()));
                        data.put("mime_type", fileMessage.getType());
                        Event.onEvent(Event.EventListener.CHAT_ATTACH_FILE, data);

                        mListAdapter.markMessageSent(fileMessage);
                    });

                    if (tempMessage != null) {
                        mListAdapter.addTempFileMessageInfo(tempMessage, uri);
                        addTempMessage(tempMessage);
                    }
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                    showUploadProgress(false);
                    Snackbar.make(mView, R.string.desk_failed_uploading_file, Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Nullable
    private Bitmap getResizedBitmap(Uri uri) throws IOException {
        InputStream input;
        input = getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        if (input != null) input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int originalSize = Math.max(onlyBoundsOptions.outHeight, onlyBoundsOptions.outWidth);

        double ratio = (originalSize > 1280) ? ((double) originalSize / 1280) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true; //optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//
        input = getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        if (input != null) input.close();
        return bitmap;
    }

    private int getPowerOfTwoForSampleRatio(double ratio) {
        int k = Integer.highestOneBit((int) Math.floor(ratio));
        if (k == 0) return 1;
        else return k;
    }

    private void showUploadProgress(boolean tf) {
        if (tf) {
            mUploadingProgressBar.setVisibility(View.VISIBLE);
            mBtnAttach.setVisibility(View.INVISIBLE);
            mBtnAttach.setEnabled(false);
            mBtnSend.setEnabled(false);
            mEditTxtReply.setEnabled(false);
        } else {
            mUploadingProgressBar.setVisibility(View.INVISIBLE);
            mBtnAttach.setVisibility(View.VISIBLE);
            mBtnAttach.setEnabled(true);
            mBtnSend.setEnabled(true);
            mEditTxtReply.setEnabled(true);
        }
    }

    private void requestMedia() {
        pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void requestCamera() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions(PERMISSION_WRITE_EXTERNAL_STORAGE_CAMERA);
        } else {
            mTempPhotoUri = getTempFileUri(true, false);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mTempPhotoUri);
            takeCameraLauncher.launch(intent);
        }
    }

    private void requestToUploadVideo() {
        // Launch the photo picker and let the user choose only videos.
        pickVideo.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly.INSTANCE)
                .build());
    }

    private void requestToRecordVideo() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions(PERMISSION_WRITE_EXTERNAL_STORAGE_RECORD_VIDEO);
        } else {
            mTempVideoUri = getTempFileUri(false, false);

            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mTempVideoUri);
            takeVideoLauncher.launch(intent);
        }
    }

    private Uri getTempFileUri(boolean isPhoto, boolean doNotUseFileProvider) {
        Uri uri = null;
        try {
            File tempFile;
            if (isPhoto) {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                tempFile = File.createTempFile("desk_" + System.currentTimeMillis(), ".jpg", path);
            } else {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                tempFile = File.createTempFile("desk_" + System.currentTimeMillis(), ".mp4", path);
            }

            if (Build.VERSION.SDK_INT >= 24 && !doNotUseFileProvider) {
                uri = FileProvider.getUriForFile(this, "com.sendbird.desk.android.provider", tempFile);
                mTempFileUri = Uri.fromFile(tempFile);
            } else {
                uri = Uri.fromFile(tempFile);
                mTempFileUri = uri;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri;
    }

    private void requestStoragePermissions(final int code) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Snackbar.make(mView, R.string.desk_storage_access_request, Snackbar.LENGTH_LONG)
                    .setAction(R.string.desk_ok, view -> ActivityCompat.requestPermissions(
                            ChatActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            code
                    ))
                    .show();
        } else {
            // Permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(
                    ChatActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    code
            );
        }
    }

    private void onUserMessageClicked(UserMessage message) {
        if (mListAdapter.isFailedMessage(message)) {
            retryFailedMessage(message);
        }
    }

    private void onUserMessageUrlPreviewClicked(UserMessage userMessage) {
        if (DeskUserRichMessage.isUrlPreviewType(userMessage)) {
            try {
                UrlPreviewInfo info = DeskUserRichMessage.getUrlPreviewInfo(userMessage);
                startWebActivity(info.getUrl());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onFileMessageClicked(FileMessage message) {
        if (mListAdapter.isFailedMessage(message)) {
            retryFailedMessage(message);
        } else if (!mListAdapter.isTempMessage(message)) {
            int fileType = getFileType(message);
            if (fileType == FILE_TYPE_IMAGE) {
                Intent i = new Intent(ChatActivity.this, PhotoViewerActivity.class);
                i.putExtra("url", message.getUrl());
                i.putExtra("type", message.getType());
                i.putExtra("name", message.getName());
                startActivity(i);
            } else if (fileType == FILE_TYPE_VIDEO) {
                Intent i = new Intent(ChatActivity.this, MediaPlayerActivity.class);
                i.putExtra("url", message.getUrl());
                i.putExtra("type", message.getType());
                i.putExtra("name", message.getName());
                startActivity(i);
            } else {
                showDownloadConfirmDialog(message);
            }
        }
    }

    private void retryFailedMessage(final BaseMessage message) {
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_Desk_AlertDialog))
                .setMessage(R.string.desk_retry_failed_message)
                .setPositiveButton(R.string.desk_resend_message, (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        if (message instanceof UserMessage) {
                            String userInput = ((UserMessage) message).getMessage();
                            sendUserMessage(userInput);
                        } else if (message instanceof FileMessage) {
                            Uri uri = mListAdapter.getTempFileMessageUri(message);
                            sendFileMessage(uri, FILE_TYPE_ALL);
                        }
                        mListAdapter.removeFailedMessage(message);
                    }
                })
                .setNegativeButton(R.string.desk_delete_message, (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        mListAdapter.removeFailedMessage(message);
                    }
                }).show();
    }

    private FileMessage mFileMessageToDownload;

    private void showDownloadConfirmDialog(final FileMessage message) {
        if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermissions(PERMISSION_WRITE_EXTERNAL_STORAGE_DOWNLOAD);
            mFileMessageToDownload = message;
        } else {
            new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_Desk_AlertDialog))
                    .setMessage(R.string.desk_download_file)
                    .setPositiveButton(R.string.desk_download, (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            String url = message.getUrl();
                            String fileName = message.getName();

                            Map<String, String> data = new HashMap<>();
                            data.put("file_name", fileName);
                            data.put("url", url);
                            Event.onEvent(Event.EventListener.CHAT_DOWNLOAD_AGENT_FILE, data);

                            FileUtils.downloadFile(ChatActivity.this, url, fileName);
                        }
                    })
                    .setNegativeButton(R.string.desk_cancel, null).show();
        }
    }

    private class MessageListAdapter extends BaseAdapter {
        private static final int TYPE_UNSUPPORTED = 0;
        private static final int TYPE_USER_MESSAGE_ME = 1;
        private static final int TYPE_USER_MESSAGE_AGENT = 2;
        private static final int TYPE_FILE_MESSAGE_ME = 3;
        private static final int TYPE_FILE_MESSAGE_AGENT = 4;
        private static final int TYPE_FILE_IMAGE_MESSAGE_ME = 5;
        private static final int TYPE_FILE_IMAGE_MESSAGE_AGENT = 6;
        private static final int TYPE_FILE_VIDEO_MESSAGE_ME = 7;
        private static final int TYPE_FILE_VIDEO_MESSAGE_AGENT = 8;
        private static final int TYPE_ADMIN_MESSAGE = 9;
        private static final int TYPE_RICH_MESSAGE_INQUIRE_CLOSURE = 10;
        private static final int TYPE_END = 11;

        private final Context mContext;
        private final LayoutInflater mInflater;
        private final ArrayList<BaseMessage> mMessageList;

        private final ArrayList<String> mFailedMessageIdList = new ArrayList<>();
        private final Hashtable<String, Uri> mTempFileMessageUriTable = new Hashtable<>();

        private MessageListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mMessageList = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return mMessageList.size();
        }

        @Override
        public BaseMessage getItem(int position) {
            return mMessageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private void clear() {
            mMessageList.clear();
        }

        private void insertMessage(BaseMessage message) {
            if (isTempMessage(message) || isFailedMessage(message)) {
                mMessageList.add(0, message);
            } else {
                for (BaseMessage item : mMessageList) {
                    if (item != null) {
                        if (message.getMessageId() == item.getMessageId()) {
                            return;
                        }
                    }
                }
                mMessageList.add(0, message);
            }
        }

        private void appendMessage(BaseMessage message) {
            for (BaseMessage item : mMessageList) {
                if (item != null) {
                    if (message.getMessageId() == item.getMessageId()) {
                        return;
                    }
                }
            }
            mMessageList.add(message);
        }

        private void addMessage(BaseMessage message) {
            mMessageList.add(message);
        }

        private void replaceMessage(BaseMessage message) {
            int index = -1;

            for (BaseMessage oriMessage : mMessageList) {
                if (oriMessage.getMessageId() == message.getMessageId()) {
                    index = mMessageList.indexOf(oriMessage);
                    mMessageList.remove(oriMessage);
                    break;
                }
            }

            if (index != -1) {
                mMessageList.add(index, message);
            }
        }

        private boolean isTempMessage(BaseMessage message) {
            return message.getMessageId() == 0;
        }

        private boolean isFailedMessage(BaseMessage message) {
            if (!isTempMessage(message)) {
                return false;
            }

            if (message instanceof UserMessage) {
                int index = mFailedMessageIdList.indexOf(((UserMessage) message).getRequestId());
                return index >= 0;
            } else if (message instanceof FileMessage) {
                int index = mFailedMessageIdList.indexOf(((FileMessage) message).getRequestId());
                return index >= 0;
            }

            return false;
        }

        private void addTempFileMessageInfo(FileMessage message, Uri uri) {
            mTempFileMessageUriTable.put(message.getRequestId(), uri);
        }

        @Nullable
        private Uri getTempFileMessageUri(BaseMessage message) {
            if (!isTempMessage(message)) {
                return null;
            }

            if (!(message instanceof FileMessage)) {
                return null;
            }

            return mTempFileMessageUriTable.get(((FileMessage) message).getRequestId());
        }

        private void removeFailedMessage(BaseMessage message) {
            if (message instanceof UserMessage) {
                mFailedMessageIdList.remove(((UserMessage) message).getRequestId());
                mMessageList.remove(message);
            } else if (message instanceof FileMessage) {
                mFailedMessageIdList.remove(((FileMessage) message).getRequestId());
                mTempFileMessageUriTable.remove(((FileMessage) message).getRequestId());
                mMessageList.remove(message);
            }

            notifyDataSetChanged();
        }

        private void markMessageFailed(String requestId) {
            mFailedMessageIdList.add(requestId);
            notifyDataSetChanged();
        }

        private void markMessageSent(BaseMessage message) {
            Object msg;

            for (int i = mMessageList.size() - 1; i >= 0; i--) {
                msg = mMessageList.get(i);
                if (message instanceof UserMessage && msg instanceof UserMessage) {
                    if (((UserMessage) msg).getRequestId().equals(((UserMessage) message).getRequestId())) {
                        mMessageList.set(i, message);
                        notifyDataSetChanged();
                        return;
                    }
                } else if (message instanceof FileMessage && msg instanceof FileMessage) {
                    if (((FileMessage) msg).getRequestId().equals(((FileMessage) message).getRequestId())) {
                        mTempFileMessageUriTable.remove(((FileMessage) message).getRequestId());
                        mMessageList.set(i, message);
                        notifyDataSetChanged();
                        return;
                    }
                }
            }
        }

        private boolean isContinuousToNext(BaseMessage currentMsg, BaseMessage nextMsg) {
            // null check
            if (currentMsg == null || nextMsg == null) {
                return false;
            }

            if (currentMsg instanceof AdminMessage && nextMsg instanceof AdminMessage) {
                return true;
            }

            String currentCustomType = currentMsg instanceof UserMessage ? ((UserMessage) currentMsg).getCustomType() :
                    currentMsg instanceof AdminMessage ? ((AdminMessage) currentMsg).getCustomType() : "";
            String nextCustomType = nextMsg instanceof UserMessage ? ((UserMessage) nextMsg).getCustomType() :
                    nextMsg instanceof AdminMessage ? ((AdminMessage) nextMsg).getCustomType() : "";

            if (!currentCustomType.equals(nextCustomType)) {
                return false;
            } else if (DeskUserRichMessage.isInquireCloserType(currentMsg)) {
                return true;
            }

            long currentCreatedAt = currentMsg.getCreatedAt();
            long nextCreatedAt = nextMsg.getCreatedAt();

            // Greater than 1 minutes.
            if (nextCreatedAt - currentCreatedAt > 60 * 1000) {
                return false;
            }

            User currentUser = null, nextUser = null;

            if (currentMsg instanceof UserMessage) {
                currentUser = ((UserMessage) currentMsg).getSender();
            } else if (currentMsg instanceof FileMessage) {
                currentUser = ((FileMessage) currentMsg).getSender();
            }

            if (nextMsg instanceof UserMessage) {
                nextUser = ((UserMessage) nextMsg).getSender();
            } else if (nextMsg instanceof FileMessage) {
                nextUser = ((FileMessage) nextMsg).getSender();
            }

            // If admin message or
            return !(currentUser == null || nextUser == null)
                    && currentUser.getUserId().equals(nextUser.getUserId());
        }

        private boolean isContinuousFromPrevious(BaseMessage currentMsg, BaseMessage precedingMsg) {
            // null check
            if (currentMsg == null || precedingMsg == null) {
                return false;
            }

            if (currentMsg instanceof AdminMessage && precedingMsg instanceof AdminMessage) {
                return true;
            }

            String currentCustomType = currentMsg instanceof UserMessage ? ((UserMessage) currentMsg).getCustomType() :
                    currentMsg instanceof AdminMessage ? ((AdminMessage) currentMsg).getCustomType() : "";
            String precedingCustomType = precedingMsg instanceof UserMessage ? ((UserMessage) precedingMsg).getCustomType() :
                    precedingMsg instanceof AdminMessage ? ((AdminMessage) precedingMsg).getCustomType() : "";

            if (!currentCustomType.equals(precedingCustomType)) {
                return false;
            } else if (DeskUserRichMessage.isInquireCloserType(currentMsg)) {
                return true;
            }

            long currentCreatedAt = currentMsg.getCreatedAt();
            long precedingCreatedAt = precedingMsg.getCreatedAt();

            // Greater than 1 minutes.
            if (currentCreatedAt - precedingCreatedAt > 60 * 1000) {
                return false;
            }

            User currentUser = null, precedingUser = null;

            if (currentMsg instanceof UserMessage) {
                currentUser = ((UserMessage) currentMsg).getSender();
            } else if (currentMsg instanceof FileMessage) {
                currentUser = ((FileMessage) currentMsg).getSender();
            }

            if (precedingMsg instanceof UserMessage) {
                precedingUser = ((UserMessage) precedingMsg).getSender();
            } else if (precedingMsg instanceof FileMessage) {
                precedingUser = ((FileMessage) precedingMsg).getSender();
            }

            // If admin message or
            return !(currentUser == null || precedingUser == null)
                    && currentUser.getUserId().equals(precedingUser.getUserId());
        }

        @Override
        public int getItemViewType(int position) {
            BaseMessage message = mMessageList.get(position);
            Sender sender = message.getSender();
            User currentUser = SendbirdChat.getCurrentUser();
            if (message instanceof UserMessage) {
                if (sender != null && currentUser != null && sender.getUserId().equals(currentUser.getUserId())) {
                    return TYPE_USER_MESSAGE_ME;
                } else {
                    if (DeskUserRichMessage.isInquireCloserType(message)) {
                        return TYPE_RICH_MESSAGE_INQUIRE_CLOSURE;
                    } else {
                        return TYPE_USER_MESSAGE_AGENT;
                    }
                }
            } else if (message instanceof FileMessage) {
                FileMessage fileMessage = (FileMessage) message;
                boolean me = sender != null && currentUser != null && sender.getUserId().equals(currentUser.getUserId());

                int fileType = getFileType(fileMessage);
                switch (fileType) {
                    case FILE_TYPE_IMAGE:
                        if (me) {
                            return TYPE_FILE_IMAGE_MESSAGE_ME;
                        } else {
                            return TYPE_FILE_IMAGE_MESSAGE_AGENT;
                        }
                    case FILE_TYPE_VIDEO:
                        if (me) {
                            return TYPE_FILE_VIDEO_MESSAGE_ME;
                        } else {
                            return TYPE_FILE_VIDEO_MESSAGE_AGENT;
                        }
                }

                if (me) {
                    return TYPE_FILE_MESSAGE_ME;
                } else {
                    return TYPE_FILE_MESSAGE_AGENT;
                }
            } else if (message instanceof AdminMessage) {
                return TYPE_ADMIN_MESSAGE;
            }

            return TYPE_UNSUPPORTED;
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_END;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            boolean isContinuousFromPrevious;
            boolean isContinuousToNext;
            boolean isNewDay = false;
            boolean isTempMessage;
            boolean isFailedMessage;
            Uri tempFileMessageUri;

            final BaseMessage message = getItem(position);
            BaseMessage prevMessage = position > 0 ? mMessageList.get(position - 1) : null;
            BaseMessage nextMessage = position < mMessageList.size() - 1 ? mMessageList.get(position + 1) : null;

            if (prevMessage == null) {
                // The first message.
                isNewDay = true;
                isContinuousFromPrevious = false;
            } else {
                if (!DateUtils.hasSameDate(message.getCreatedAt(), prevMessage.getCreatedAt())) {
                    isNewDay = true;
                    isContinuousFromPrevious = false;
                } else {
                    isContinuousFromPrevious = isContinuousFromPrevious(message, prevMessage);
                }
            }

            isContinuousToNext = nextMessage != null  // If nextMessage is null, the current message is the last one.
                    && DateUtils.hasSameDate(message.getCreatedAt(), nextMessage.getCreatedAt())
                    && isContinuousToNext(message, nextMessage);

            isTempMessage = isTempMessage(message);
            tempFileMessageUri = getTempFileMessageUri(message);
            isFailedMessage = isFailedMessage(message);

            ViewHolder viewHolder;

            if (convertView == null || ((ViewHolder) convertView.getTag()).getViewType() != getItemViewType(position) || mIsScrolling) {
                viewHolder = new ViewHolder();
                viewHolder.setViewType(getItemViewType(position));

                switch (getItemViewType(position)) {
                    case TYPE_USER_MESSAGE_ME:
                        convertView = mInflater.inflate(R.layout.list_item_msg_user_me, parent, false);
                        viewHolder.setView("message_container", convertView.findViewById(R.id.message_container));
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("txt_delivery_status", convertView.findViewById(R.id.txt_delivery_status));
                        viewHolder.setView("loading_progress_bar", convertView.findViewById(R.id.loading_progress_bar));
                        viewHolder.setView("txt_message", convertView.findViewById(R.id.txt_message));

                        viewHolder.setView("url_preview_container", convertView.findViewById(R.id.url_preview_container));
                        viewHolder.setView("img_url_preview_main", convertView.findViewById(R.id.img_url_preview_main));
                        viewHolder.setView("txt_url_preview_title", convertView.findViewById(R.id.txt_url_preview_title));
                        viewHolder.setView("txt_url_preview_domain_name", convertView.findViewById(R.id.txt_url_preview_domain_name));

                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("view_pre_ungrouping", convertView.findViewById(R.id.view_pre_ungrouping));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_USER_MESSAGE_AGENT:
                        convertView = mInflater.inflate(R.layout.list_item_msg_user_agent, parent, false);
                        viewHolder.setView("message_container", convertView.findViewById(R.id.message_container));
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("img_agent_profile", convertView.findViewById(R.id.img_agent_profile));
                        viewHolder.setView("txt_agent_name", convertView.findViewById(R.id.txt_agent_name));
                        viewHolder.setView("txt_message", convertView.findViewById(R.id.txt_message));

                        viewHolder.setView("url_preview_container", convertView.findViewById(R.id.url_preview_container));
                        viewHolder.setView("img_url_preview_main", convertView.findViewById(R.id.img_url_preview_main));
                        viewHolder.setView("txt_url_preview_title", convertView.findViewById(R.id.txt_url_preview_title));
                        viewHolder.setView("txt_url_preview_domain_name", convertView.findViewById(R.id.txt_url_preview_domain_name));

                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        viewHolder.setView("recycler_view_faq", convertView.findViewById(R.id.recycler_view_faq));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_FILE_MESSAGE_ME:
                        convertView = mInflater.inflate(R.layout.list_item_msg_file_me, parent, false);
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("txt_delivery_status", convertView.findViewById(R.id.txt_delivery_status));
                        viewHolder.setView("txt_message", convertView.findViewById(R.id.txt_message));
                        viewHolder.setView("txt_file_size", convertView.findViewById(R.id.txt_file_size));
                        viewHolder.setView("txt_file_download", convertView.findViewById(R.id.txt_file_download));
                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("view_pre_ungrouping", convertView.findViewById(R.id.view_pre_ungrouping));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_FILE_MESSAGE_AGENT:
                        convertView = mInflater.inflate(R.layout.list_item_msg_file_agent, parent, false);
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("img_agent_profile", convertView.findViewById(R.id.img_agent_profile));
                        viewHolder.setView("txt_agent_name", convertView.findViewById(R.id.txt_agent_name));
                        viewHolder.setView("txt_message", convertView.findViewById(R.id.txt_message));
                        viewHolder.setView("txt_file_size", convertView.findViewById(R.id.txt_file_size));
                        viewHolder.setView("txt_file_download", convertView.findViewById(R.id.txt_file_download));
                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_FILE_IMAGE_MESSAGE_ME:
                        convertView = mInflater.inflate(R.layout.list_item_msg_file_image_me, parent, false);
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("txt_delivery_status", convertView.findViewById(R.id.txt_delivery_status));
                        viewHolder.setView("img_thumbnail", convertView.findViewById(R.id.img_thumbnail));
                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("view_pre_ungrouping", convertView.findViewById(R.id.view_pre_ungrouping));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_FILE_IMAGE_MESSAGE_AGENT:
                        convertView = mInflater.inflate(R.layout.list_item_msg_file_image_agent, parent, false);
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("img_agent_profile", convertView.findViewById(R.id.img_agent_profile));
                        viewHolder.setView("txt_agent_name", convertView.findViewById(R.id.txt_agent_name));
                        viewHolder.setView("img_thumbnail", convertView.findViewById(R.id.img_thumbnail));
                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_FILE_VIDEO_MESSAGE_ME:
                        convertView = mInflater.inflate(R.layout.list_item_msg_file_video_me, parent, false);
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("txt_delivery_status", convertView.findViewById(R.id.txt_delivery_status));
                        viewHolder.setView("img_thumbnail", convertView.findViewById(R.id.img_thumbnail));
                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("img_play", convertView.findViewById(R.id.img_play));
                        viewHolder.setView("view_pre_ungrouping", convertView.findViewById(R.id.view_pre_ungrouping));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_FILE_VIDEO_MESSAGE_AGENT:
                        convertView = mInflater.inflate(R.layout.list_item_msg_file_video_agent, parent, false);
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("img_agent_profile", convertView.findViewById(R.id.img_agent_profile));
                        viewHolder.setView("txt_agent_name", convertView.findViewById(R.id.txt_agent_name));
                        viewHolder.setView("img_thumbnail", convertView.findViewById(R.id.img_thumbnail));
                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("img_play", convertView.findViewById(R.id.img_play));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_ADMIN_MESSAGE:
                        convertView = mInflater.inflate(R.layout.list_item_msg_admin, parent, false);
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("txt_message", convertView.findViewById(R.id.txt_message));
                        viewHolder.setView("txt_time", convertView.findViewById(R.id.txt_time));
                        viewHolder.setView("view_pre_ungrouping", convertView.findViewById(R.id.view_pre_ungrouping));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        convertView.setTag(viewHolder);
                        break;

                    case TYPE_RICH_MESSAGE_INQUIRE_CLOSURE:
                        convertView = mInflater.inflate(R.layout.list_item_msg_inquire_closure, parent, false);
                        viewHolder.setView("txt_date", convertView.findViewById(R.id.txt_date));
                        viewHolder.setView("txt_message", convertView.findViewById(R.id.txt_message));
                        viewHolder.setView("btn_yes", convertView.findViewById(R.id.btn_yes));
                        viewHolder.setView("btn_no", convertView.findViewById(R.id.btn_no));
                        viewHolder.setView("progress_bar_yes", convertView.findViewById(R.id.progress_bar_yes));
                        viewHolder.setView("progress_bar_no", convertView.findViewById(R.id.progress_bar_no));
                        viewHolder.setView("view_pre_ungrouping", convertView.findViewById(R.id.view_pre_ungrouping));
                        viewHolder.setView("view_next_ungrouping", convertView.findViewById(R.id.view_next_ungrouping));
                        viewHolder.setView("container_btn", convertView.findViewById(R.id.container_btn));
                        convertView.setTag(viewHolder);
                        break;

                    default:
                        convertView = new View(mInflater.getContext());
                        convertView.setTag(viewHolder);
                        break;
                }
            }

            viewHolder = (ViewHolder) convertView.getTag();
            switch (getItemViewType(position)) {
                case TYPE_USER_MESSAGE_ME: {
                    final UserMessage userMessage = (UserMessage) message;

                    View messageContainer = viewHolder.getView("message_container");
                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    TextView txtDeliveryStatus = viewHolder.getView("txt_delivery_status", TextView.class);
                    ProgressBar loadingProgressBar = viewHolder.getView("loading_progress_bar", ProgressBar.class);
                    TextView txtMessage = viewHolder.getView("txt_message", TextView.class);

                    View urlPreviewContainer = viewHolder.getView("url_preview_container");
                    ImageView imgUrlPreviewMain = viewHolder.getView("img_url_preview_main", ImageView.class);
                    TextView txtUrlPreviewTitle = viewHolder.getView("txt_url_preview_title", TextView.class);
                    TextView txtUrlPreviewDomainName = viewHolder.getView("txt_url_preview_domain_name", TextView.class);

                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    View preUngroup = viewHolder.getView("view_pre_ungrouping");
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, userMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    if (isFailedMessage) {
                        txtDeliveryStatus.setText(R.string.desk_message_failed);
                        txtDeliveryStatus.setVisibility(View.VISIBLE);
                    } else if (isTempMessage) {
                        txtDeliveryStatus.setText(R.string.desk_message_sending);
                        txtDeliveryStatus.setVisibility(View.VISIBLE);
                    } else {
                        txtDeliveryStatus.setVisibility(View.INVISIBLE);
                    }

                    setTextToCheckUrls(txtMessage, userMessage.getMessage());

                    if (DeskUserRichMessage.isUrlPreviewType(userMessage)) {
                        try {
                            loadingProgressBar.setVisibility(View.GONE);
                            final UrlPreviewInfo info = DeskUserRichMessage.getUrlPreviewInfo(userMessage);
                            if (info.getTitle() != null && info.getTitle().length() > 0
                                    && info.getImageUrl() != null && info.getImageUrl().length() > 0) {
                                urlPreviewContainer.setVisibility(View.VISIBLE);
                                ImageUtils.displayImageFromUrl(ChatActivity.this, info.getImageUrl(), imgUrlPreviewMain, null);
                                txtUrlPreviewTitle.setText(info.getTitle());
                                txtUrlPreviewDomainName.setText(info.getDomainName());
                            } else {
                                urlPreviewContainer.setVisibility(View.GONE);
                            }
                        } catch (Exception e) {
                            urlPreviewContainer.setVisibility(View.GONE);
                            e.printStackTrace();
                        }
                    } else {
                        if (mUrlPreviewTempMessage != null && mUrlPreviewTempMessage.getMessageId() == userMessage.getMessageId()) {
                            loadingProgressBar.setVisibility(View.VISIBLE);
                        } else {
                            loadingProgressBar.setVisibility(View.GONE);
                        }
                        urlPreviewContainer.setVisibility(View.GONE);
                    }

                    if (!isContinuousToNext) {
                        txtTime.setText(DateUtils.formatTime(userMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        preUngroup.setVisibility(View.VISIBLE);
                    } else {
                        preUngroup.setVisibility(View.GONE);
                    }

                    if (isFailedMessage) {
                        messageContainer.setOnClickListener(view -> onUserMessageClicked(userMessage));

                        txtMessage.setOnClickListener(v -> onUserMessageClicked(userMessage));
                    } else {
                        urlPreviewContainer.setOnClickListener(v -> onUserMessageUrlPreviewClicked(userMessage));
                    }
                    break;
                }
                case TYPE_USER_MESSAGE_AGENT: {
                    final UserMessage userMessage = (UserMessage) message;

                    View messageContainer = viewHolder.getView("message_container");
                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    ImageView imgAgentProfile = viewHolder.getView("img_agent_profile", ImageView.class);
                    TextView txtAgentName = viewHolder.getView("txt_agent_name", TextView.class);
                    TextView txtMessage = viewHolder.getView("txt_message", TextView.class);

                    View urlPreviewContainer = viewHolder.getView("url_preview_container");
                    ImageView imgUrlPreviewMain = viewHolder.getView("img_url_preview_main", ImageView.class);
                    TextView txtUrlPreviewTitle = viewHolder.getView("txt_url_preview_title", TextView.class);
                    TextView txtUrlPreviewDomainName = viewHolder.getView("txt_url_preview_domain_name", TextView.class);

                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");
                    RecyclerView faqRecyclerView = viewHolder.getView("recycler_view_faq", RecyclerView.class);

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, userMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    setTextToCheckUrls(txtMessage, userMessage.getMessage());

                    if (DeskUserRichMessage.isUrlPreviewType(userMessage)) {
                        try {
                            final UrlPreviewInfo info = DeskUserRichMessage.getUrlPreviewInfo(userMessage);
                            if (info.getTitle() != null && info.getTitle().length() > 0
                                    && info.getImageUrl() != null && info.getImageUrl().length() > 0) {
                                urlPreviewContainer.setVisibility(View.VISIBLE);
                                ImageUtils.displayImageFromUrl(ChatActivity.this, info.getImageUrl(), imgUrlPreviewMain, null);
                                txtUrlPreviewTitle.setText(info.getTitle());
                                txtUrlPreviewDomainName.setText(info.getDomainName());
                            } else {
                                urlPreviewContainer.setVisibility(View.GONE);
                            }
                        } catch (Exception e) {
                            urlPreviewContainer.setVisibility(View.GONE);
                            e.printStackTrace();
                        }
                    } else {
                        urlPreviewContainer.setVisibility(View.GONE);
                    }

                    final FAQData faqData = SendBirdDesk.generateFAQData(userMessage);
                    if (faqData != null && faqData.getFaqFileId() >= 0 &&
                            faqData.getFaqResults() != null && !faqData.getFaqResults().isEmpty()) {
                        faqRecyclerView.setVisibility(View.VISIBLE);
                        final FAQResultAdapter adapter = new FAQResultAdapter();
                        faqRecyclerView.setLayoutManager(new LinearLayoutManager(ChatActivity.this));
                        faqRecyclerView.setAdapter(adapter);
                        adapter.setItems(faqData.getFaqResults());
                        adapter.setOnItemClickListener(result -> {
                            if (TextUtils.isEmpty(result.getUrl())) return;
                            Uri webpage = Uri.parse(result.getUrl());
                            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        });
                    } else {
                        faqRecyclerView.setVisibility(View.GONE);
                    }

                    Sender sender = userMessage.getSender();
                    if (!isContinuousToNext) {
                        TypedArray ta = mContext.obtainStyledAttributes(new int[]{R.attr.deskAvatarIcon});
                        ImageUtils.displayRoundImageFromUrlWithPlaceHolder(mContext,
                                sender != null ? sender.getProfileUrl() : null,
                                imgAgentProfile,
                                ta.getResourceId(0, R.drawable.img_profile));
                        ta.recycle();

                        imgAgentProfile.setVisibility(View.VISIBLE);

                        txtTime.setText(DateUtils.formatTime(userMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        imgAgentProfile.setVisibility(View.INVISIBLE);
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        txtAgentName.setText(sender != null ? sender.getNickname() : "");
                        txtAgentName.setVisibility(View.VISIBLE);
                    } else {
                        txtAgentName.setVisibility(View.GONE);
                    }

                    if (isFailedMessage) {
                        messageContainer.setOnClickListener(view -> onUserMessageClicked(userMessage));

                        txtMessage.setOnClickListener(v -> onUserMessageClicked(userMessage));
                    } else {
                        urlPreviewContainer.setOnClickListener(v -> onUserMessageUrlPreviewClicked(userMessage));
                    }
                    break;
                }

                case TYPE_FILE_MESSAGE_ME: {
                    final FileMessage fileMessage = (FileMessage) message;

                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    TextView txtDeliveryStatus = viewHolder.getView("txt_delivery_status", TextView.class);
                    TextView txtMessage = viewHolder.getView("txt_message", TextView.class);
                    TextView txtFileSize = viewHolder.getView("txt_file_size", TextView.class);
                    TextView txtFileDownload = viewHolder.getView("txt_file_download", TextView.class);
                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    View preUngroup = viewHolder.getView("view_pre_ungrouping");
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, fileMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    if (isFailedMessage) {
                        txtDeliveryStatus.setText(R.string.desk_message_failed);
                        txtDeliveryStatus.setVisibility(View.VISIBLE);
                    } else if (isTempMessage) {
                        txtDeliveryStatus.setText(R.string.desk_message_sending);
                        txtDeliveryStatus.setVisibility(View.VISIBLE);
                    } else {
                        txtDeliveryStatus.setVisibility(View.INVISIBLE);
                    }

                    txtMessage.setText(fileMessage.getName());
                    txtFileSize.setText(FileUtils.toReadableFileSize(fileMessage.getSize()));
                    txtFileDownload.setOnClickListener(v -> onFileMessageClicked(fileMessage));

                    if (!isContinuousToNext) {
                        txtTime.setText(DateUtils.formatTime(fileMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        preUngroup.setVisibility(View.VISIBLE);
                    } else {
                        preUngroup.setVisibility(View.GONE);
                    }
                    break;
                }
                case TYPE_FILE_MESSAGE_AGENT: {
                    final FileMessage fileMessage = (FileMessage) message;

                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    ImageView imgAgentProfile = viewHolder.getView("img_agent_profile", ImageView.class);
                    TextView txtAgentName = viewHolder.getView("txt_agent_name", TextView.class);
                    TextView txtMessage = viewHolder.getView("txt_message", TextView.class);
                    TextView txtFileSize = viewHolder.getView("txt_file_size", TextView.class);
                    TextView txtFileDownload = viewHolder.getView("txt_file_download", TextView.class);
                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, fileMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    txtMessage.setText(fileMessage.getName());
                    txtFileSize.setText(FileUtils.toReadableFileSize(fileMessage.getSize()));
                    txtFileDownload.setOnClickListener(v -> onFileMessageClicked(fileMessage));

                    final Sender fileMessageSender = fileMessage.getSender();
                    if (!isContinuousToNext) {
                        TypedArray ta = mContext.obtainStyledAttributes(new int[]{R.attr.deskAvatarIcon});
                        ImageUtils.displayRoundImageFromUrlWithPlaceHolder(mContext,
                                fileMessageSender != null ? fileMessageSender.getProfileUrl() : null,
                                imgAgentProfile,
                                ta.getResourceId(0, R.drawable.img_profile));
                        ta.recycle();

                        imgAgentProfile.setVisibility(View.VISIBLE);

                        txtTime.setText(DateUtils.formatTime(fileMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        imgAgentProfile.setVisibility(View.INVISIBLE);
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        txtAgentName.setText(fileMessageSender != null ? fileMessageSender.getNickname() : "");
                        txtAgentName.setVisibility(View.VISIBLE);
                    } else {
                        txtAgentName.setVisibility(View.GONE);
                    }
                    break;
                }

                case TYPE_FILE_IMAGE_MESSAGE_ME: {
                    final FileMessage fileMessage = (FileMessage) message;

                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    TextView txtDeliveryStatus = viewHolder.getView("txt_delivery_status", TextView.class);
                    ImageView imgThumbnail = viewHolder.getView("img_thumbnail", ImageView.class);
                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    View preUngroup = viewHolder.getView("view_pre_ungrouping");
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, fileMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    if (isFailedMessage) {
                        txtDeliveryStatus.setText(R.string.desk_message_failed);
                        txtDeliveryStatus.setVisibility(View.VISIBLE);
                    } else if (isTempMessage) {
                        txtDeliveryStatus.setText(R.string.desk_message_sending);
                        txtDeliveryStatus.setVisibility(View.VISIBLE);
                    } else {
                        txtDeliveryStatus.setVisibility(View.INVISIBLE);
                    }

                    if (isTempMessage && tempFileMessageUri != null) {
                        ImageUtils.displayImageFromUrl(ChatActivity.this, tempFileMessageUri.toString(), imgThumbnail, null);
                    } else {
                        // Get thumbnails from FileMessage
                        ArrayList<Thumbnail> thumbnails = (ArrayList<Thumbnail>) fileMessage.getThumbnails();

                        // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                        if (thumbnails.size() > 0) {
                            if (fileMessage.getType().toLowerCase().contains("gif")) {
                                ImageUtils.displayGifImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, thumbnails.get(0).getUrl(), imgThumbnail.getDrawable());
                            } else {
                                ImageUtils.displayImageFromUrl(ChatActivity.this, thumbnails.get(0).getUrl(), imgThumbnail, imgThumbnail.getDrawable());
                            }
                        } else {
                            if (fileMessage.getType().toLowerCase().contains("gif")) {
                                ImageUtils.displayGifImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, null, imgThumbnail.getDrawable());
                            } else {
                                ImageUtils.displayImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, imgThumbnail.getDrawable());
                            }
                        }
                    }
                    imgThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    if (!isContinuousToNext) {
                        txtTime.setText(DateUtils.formatTime(fileMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        preUngroup.setVisibility(View.VISIBLE);
                    } else {
                        preUngroup.setVisibility(View.GONE);
                    }

                    imgThumbnail.setOnClickListener(view -> onFileMessageClicked(fileMessage));
                    break;
                }
                case TYPE_FILE_IMAGE_MESSAGE_AGENT: {
                    final FileMessage fileMessage = (FileMessage) message;

                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    ImageView imgAgentProfile = viewHolder.getView("img_agent_profile", ImageView.class);
                    TextView txtAgentName = viewHolder.getView("txt_agent_name", TextView.class);
                    ImageView imgThumbnail = viewHolder.getView("img_thumbnail", ImageView.class);
                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, fileMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    if (isTempMessage && tempFileMessageUri != null) {
                        ImageUtils.displayImageFromUrl(ChatActivity.this, tempFileMessageUri.toString(), imgThumbnail, null);
                    } else {
                        // Get thumbnails from FileMessage
                        ArrayList<Thumbnail> thumbnails = (ArrayList<Thumbnail>) fileMessage.getThumbnails();

                        // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                        if (thumbnails.size() > 0) {
                            if (fileMessage.getType().toLowerCase().contains("gif")) {
                                ImageUtils.displayGifImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, thumbnails.get(0).getUrl(), imgThumbnail.getDrawable());
                            } else {
                                ImageUtils.displayImageFromUrl(ChatActivity.this, thumbnails.get(0).getUrl(), imgThumbnail, imgThumbnail.getDrawable());
                            }
                        } else {
                            if (fileMessage.getType().toLowerCase().contains("gif")) {
                                ImageUtils.displayGifImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, null, imgThumbnail.getDrawable());
                            } else {
                                ImageUtils.displayImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, imgThumbnail.getDrawable());
                            }
                        }
                    }
                    imgThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    final Sender fileMessageSender = fileMessage.getSender();
                    if (!isContinuousToNext) {
                        TypedArray ta = mContext.obtainStyledAttributes(new int[]{R.attr.deskAvatarIcon});
                        ImageUtils.displayRoundImageFromUrlWithPlaceHolder(mContext,
                                fileMessageSender != null ? fileMessageSender.getProfileUrl() : null,
                                imgAgentProfile,
                                ta.getResourceId(0, R.drawable.img_profile));
                        ta.recycle();

                        imgAgentProfile.setVisibility(View.VISIBLE);

                        txtTime.setText(DateUtils.formatTime(fileMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        imgAgentProfile.setVisibility(View.INVISIBLE);
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        txtAgentName.setText(fileMessageSender != null ? fileMessageSender.getNickname() : "");
                        txtAgentName.setVisibility(View.VISIBLE);
                    } else {
                        txtAgentName.setVisibility(View.GONE);
                    }

                    imgThumbnail.setOnClickListener(view -> onFileMessageClicked(fileMessage));
                    break;
                }

                case TYPE_FILE_VIDEO_MESSAGE_ME: {
                    final FileMessage fileMessage = (FileMessage) message;

                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    TextView txtDeliveryStatus = viewHolder.getView("txt_delivery_status", TextView.class);
                    ImageView imgThumbnail = viewHolder.getView("img_thumbnail", ImageView.class);
                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    ImageView imgPlay = viewHolder.getView("img_play", ImageView.class);
                    View preUngroup = viewHolder.getView("view_pre_ungrouping");
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, fileMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    if (isFailedMessage) {
                        txtDeliveryStatus.setText(R.string.desk_message_failed);
                        txtDeliveryStatus.setVisibility(View.VISIBLE);
                    } else if (isTempMessage) {
                        txtDeliveryStatus.setText(R.string.desk_message_sending);
                        txtDeliveryStatus.setVisibility(View.VISIBLE);
                    } else {
                        txtDeliveryStatus.setVisibility(View.INVISIBLE);
                    }

                    if (isTempMessage && tempFileMessageUri != null) {
                        ImageUtils.displayImageFromUrl(ChatActivity.this, tempFileMessageUri.toString(), imgThumbnail, null);
                    } else {
                        // Get thumbnails from FileMessage
                        ArrayList<Thumbnail> thumbnails = (ArrayList<Thumbnail>) fileMessage.getThumbnails();

                        // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                        if (thumbnails.size() > 0) {
                            if (fileMessage.getType().toLowerCase().contains("gif")) {
                                ImageUtils.displayGifImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, thumbnails.get(0).getUrl(), imgThumbnail.getDrawable());
                            } else {
                                ImageUtils.displayImageFromUrl(ChatActivity.this, thumbnails.get(0).getUrl(), imgThumbnail, imgThumbnail.getDrawable());
                            }
                        } else {
                            if (fileMessage.getType().toLowerCase().contains("gif")) {
                                ImageUtils.displayGifImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, null, imgThumbnail.getDrawable());
                            } else {
                                ImageUtils.displayImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, imgThumbnail.getDrawable());
                            }
                        }
                    }
                    imgThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    if (!isContinuousToNext) {
                        txtTime.setText(DateUtils.formatTime(fileMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        preUngroup.setVisibility(View.VISIBLE);
                    } else {
                        preUngroup.setVisibility(View.GONE);
                    }

                    if (isFailedMessage) {
                        imgThumbnail.setOnClickListener(view -> onFileMessageClicked(fileMessage));
                    } else {
                        imgPlay.setOnClickListener(view -> onFileMessageClicked(fileMessage));
                    }
                    break;
                }
                case TYPE_FILE_VIDEO_MESSAGE_AGENT: {
                    final FileMessage fileMessage = (FileMessage) message;

                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    ImageView imgAgentProfile = viewHolder.getView("img_agent_profile", ImageView.class);
                    TextView txtAgentName = viewHolder.getView("txt_agent_name", TextView.class);
                    ImageView imgThumbnail = viewHolder.getView("img_thumbnail", ImageView.class);
                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    ImageView imgPlay = viewHolder.getView("img_play", ImageView.class);
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, fileMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    if (isTempMessage && tempFileMessageUri != null) {
                        ImageUtils.displayImageFromUrl(ChatActivity.this, tempFileMessageUri.toString(), imgThumbnail, null);
                    } else {
                        // Get thumbnails from FileMessage
                        ArrayList<Thumbnail> thumbnails = (ArrayList<Thumbnail>) fileMessage.getThumbnails();

                        // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                        if (thumbnails.size() > 0) {
                            if (fileMessage.getType().toLowerCase().contains("gif")) {
                                ImageUtils.displayGifImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, thumbnails.get(0).getUrl(), imgThumbnail.getDrawable());
                            } else {
                                ImageUtils.displayImageFromUrl(ChatActivity.this, thumbnails.get(0).getUrl(), imgThumbnail, imgThumbnail.getDrawable());
                            }
                        } else {
                            if (fileMessage.getType().toLowerCase().contains("gif")) {
                                ImageUtils.displayGifImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, null, imgThumbnail.getDrawable());
                            } else {
                                ImageUtils.displayImageFromUrl(ChatActivity.this, fileMessage.getUrl(), imgThumbnail, imgThumbnail.getDrawable());
                            }
                        }
                    }
                    imgThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    final Sender fileMessageSender = fileMessage.getSender();
                    if (!isContinuousToNext) {
                        TypedArray ta = mContext.obtainStyledAttributes(new int[]{R.attr.deskAvatarIcon});
                        ImageUtils.displayRoundImageFromUrlWithPlaceHolder(mContext,
                                fileMessageSender != null ? fileMessageSender.getProfileUrl() : null,
                                imgAgentProfile,
                                ta.getResourceId(0, R.drawable.img_profile));
                        ta.recycle();

                        imgAgentProfile.setVisibility(View.VISIBLE);

                        txtTime.setText(DateUtils.formatTime(fileMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        imgAgentProfile.setVisibility(View.INVISIBLE);
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        txtAgentName.setText(fileMessageSender != null ? fileMessageSender.getNickname() : "");
                        txtAgentName.setVisibility(View.VISIBLE);
                    } else {
                        txtAgentName.setVisibility(View.GONE);
                    }

                    if (isFailedMessage) {
                        imgThumbnail.setOnClickListener(view -> onFileMessageClicked(fileMessage));
                    } else {
                        imgPlay.setOnClickListener(view -> onFileMessageClicked(fileMessage));
                    }
                    break;
                }

                case TYPE_ADMIN_MESSAGE: {
                    AdminMessage adminMessage = (AdminMessage) message;

                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    TextView txtMessage = viewHolder.getView("txt_message", TextView.class);
                    TextView txtTime = viewHolder.getView("txt_time", TextView.class);
                    View preUngroup = viewHolder.getView("view_pre_ungrouping");
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, adminMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    txtMessage.setText(adminMessage.getMessage());

                    if (!isContinuousToNext) {
                        txtTime.setText(DateUtils.formatTime(adminMessage.getCreatedAt()));
                        txtTime.setVisibility(View.VISIBLE);
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        txtTime.setVisibility(View.GONE);
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        preUngroup.setVisibility(View.VISIBLE);
                    } else {
                        preUngroup.setVisibility(View.GONE);
                    }
                    break;
                }

                case TYPE_RICH_MESSAGE_INQUIRE_CLOSURE: {
                    final UserMessage userMessage = (UserMessage) message;

                    TextView txtDate = viewHolder.getView("txt_date", TextView.class);
                    TextView txtMessage = viewHolder.getView("txt_message", TextView.class);
                    final Button btnYes = viewHolder.getView("btn_yes", Button.class);
                    final Button btnNo = viewHolder.getView("btn_no", Button.class);
                    final ProgressBar progressBarYes = viewHolder.getView("progress_bar_yes", ProgressBar.class);
                    final ProgressBar progressBarNo = viewHolder.getView("progress_bar_no", ProgressBar.class);
                    final View btnGroup = viewHolder.getView("container_btn");
                    View preUngroup = viewHolder.getView("view_pre_ungrouping");
                    View nextUngroup = viewHolder.getView("view_next_ungrouping");

                    if (isNewDay) {
                        txtDate.setText(DateUtils.formatDate(mContext, userMessage.getCreatedAt()));
                        txtDate.setVisibility(View.VISIBLE);
                    } else {
                        txtDate.setVisibility(View.GONE);
                    }

                    txtMessage.setText(userMessage.getMessage());

                    if (!isContinuousToNext) {
                        nextUngroup.setVisibility(View.VISIBLE);
                    } else {
                        nextUngroup.setVisibility(View.GONE);
                    }

                    if (!isContinuousFromPrevious) {
                        preUngroup.setVisibility(View.VISIBLE);
                    } else {
                        preUngroup.setVisibility(View.GONE);
                    }

                    if (DeskUserRichMessage.isInquireCloserTypeWaitingState(message)) {
                        txtMessage.setText(userMessage.getMessage());
                        btnGroup.setVisibility(View.VISIBLE);
                        btnGroup.setEnabled(true);
                        btnYes.setAlpha(1f);
                        btnNo.setAlpha(1f);
                        progressBarYes.setVisibility(View.INVISIBLE);
                        progressBarNo.setVisibility(View.INVISIBLE);

                        btnYes.setOnClickListener(view -> {
                            btnGroup.setEnabled(false);
                            btnYes.setAlpha(0.5f);
                            btnNo.setAlpha(0.5f);
                            progressBarYes.setVisibility(View.VISIBLE);
                            progressBarNo.setVisibility(View.INVISIBLE);

                            mTicket.confirmEndOfChat(userMessage, true, (ticket, e) -> {
                                btnGroup.setEnabled(true);
                                btnYes.setAlpha(1f);
                                btnNo.setAlpha(1f);
                                progressBarYes.setVisibility(View.INVISIBLE);
                                progressBarNo.setVisibility(View.INVISIBLE);

                                if (e != null) {
                                    return;
                                }

                                Map<String, String> data = new HashMap<>();
                                data.put("choice", "yes");
                                Event.onEvent(Event.EventListener.CHAT_CONFIRM_END_OF_CHAT, data);
                            });
                        });

                        btnNo.setOnClickListener(view -> {
                            btnGroup.setEnabled(false);
                            btnYes.setAlpha(0.5f);
                            btnNo.setAlpha(0.5f);
                            progressBarYes.setVisibility(View.INVISIBLE);
                            progressBarNo.setVisibility(View.VISIBLE);

                            mTicket.confirmEndOfChat(userMessage, false, (ticket, e) -> {
                                btnGroup.setEnabled(true);
                                btnYes.setAlpha(1f);
                                btnNo.setAlpha(1f);
                                progressBarYes.setVisibility(View.INVISIBLE);
                                progressBarNo.setVisibility(View.INVISIBLE);

                                if (e != null) {
                                    return;
                                }

                                Map<String, String> data = new HashMap<>();
                                data.put("choice", "no");
                                Event.onEvent(Event.EventListener.CHAT_CONFIRM_END_OF_CHAT, data);
                            });
                        });
                    } else if (DeskUserRichMessage.isInquireCloserTypeConfirmedState(message)) {
                        txtMessage.setText(userMessage.getMessage());
                        btnGroup.setVisibility(View.GONE);
                    } else if (DeskUserRichMessage.isInquireCloserTypeDeclinedState(message)) {
                        txtMessage.setText(userMessage.getMessage());
                        btnGroup.setVisibility(View.GONE);
                    }
                    break;
                }

                default:
                    break;
            }

            return convertView;
        }

        private class ViewHolder {
            private final Hashtable<String, View> holder = new Hashtable<>();
            private int type;

            private int getViewType() {
                return this.type;
            }

            private void setViewType(int type) {
                this.type = type;
            }

            private void setView(String k, View v) {
                holder.put(k, v);
            }

            private View getView(String k) {
                return holder.get(k);
            }

            private <T> T getView(String k, Class<T> type) {
                return type.cast(getView(k));
            }
        }
    }

    private static class FAQResultAdapter extends RecyclerView.Adapter<FAQResultAdapter.FAQResultViewHolder> {
        public interface OnItemClickListener {
            void onItemClicked(@NonNull FAQData.FAQResult result);
        }

        @NonNull
        private final List<FAQData.FAQResult> items = new ArrayList<>();
        @Nullable
        private OnItemClickListener listener;

        @NonNull
        @Override
        public FAQResultViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new FAQResultViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_faq_result, viewGroup, false), viewHolder -> {
                if (listener != null) listener.onItemClicked(items.get(viewHolder.getAdapterPosition()));
            });
        }

        @Override
        public void onBindViewHolder(@NonNull final FAQResultViewHolder faqResultViewHolder, int i) {
            faqResultViewHolder.bind(items.get(i));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void setItems(@NonNull List<FAQData.FAQResult> items) {
            this.items.clear();
            this.items.addAll(items);
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
            this.listener = listener;
        }

        private static class FAQResultViewHolder extends RecyclerView.ViewHolder {
            public interface OnViewHolderClickListener {
                void onClicked(@NonNull RecyclerView.ViewHolder viewHolder);
            }

            private final TextView questionTextView;
            private final TextView answerTextView;
            private final ImageView imageView;

            public FAQResultViewHolder(@NonNull View itemView, @Nullable final OnViewHolderClickListener listener) {
                super(itemView);
                questionTextView = itemView.findViewById(R.id.txt_question);
                answerTextView = itemView.findViewById(R.id.txt_answer);
                imageView = itemView.findViewById(R.id.img);
                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onClicked(FAQResultViewHolder.this);
                });
            }

            public void bind(final FAQData.FAQResult result) {
                questionTextView.setText(result.getQuestion());
                answerTextView.setText(result.getAnswer());
                if (!TextUtils.isEmpty(result.getImageUrl())) {
                    imageView.setVisibility(View.VISIBLE);
                    ImageUtils.displayImageFromUrl(itemView.getContext(), result.getImageUrl(), imageView, null);
                } else {
                    imageView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setTextToCheckUrls(TextView textView, String text) {
        try {
            textView.setText(text);
            BetterLinkMovementMethod
                    .linkify(Linkify.WEB_URLS, textView)
                    .setOnLinkClickListener((textView1, url) -> {
                        startWebActivity(url);
                        return true;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startWebActivity(String url) {
        if (url != null && url.length() > 0) {
            Intent intent = new Intent(ChatActivity.this, WebActivity.class);
            intent.putExtra(WebActivity.INTENT_EXTRA_URL, url);
            startActivity(intent);
        }
    }

    private int getFileType(FileMessage fileMessage) {
        int fileType = FILE_TYPE_ALL;
        String type = fileMessage.getType();
        if (type.startsWith("image")) {
            fileType = FILE_TYPE_IMAGE;
        } else if (type.startsWith("video")) {
            fileType = FILE_TYPE_VIDEO;
        }
        return fileType;
    }
}
