package com.sendbird.desk.android.sample.activity.chat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.app.Event;
import com.sendbird.desk.android.sample.utils.FileUtils;
import com.sendbird.desk.android.sample.utils.image.ImageUtils;
import com.sendbird.desk.android.sample.utils.photoview.PhotoView;

import java.util.HashMap;
import java.util.Map;

public class PhotoViewerActivity extends AppCompatActivity {

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0xf0;

    private boolean showButtons = true;
    private String mUrl;
    private String mName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_viewer);

        mUrl = getIntent().getStringExtra("url");
        mName = getIntent().getStringExtra("name");
        String type = getIntent().getStringExtra("type");

        final View buttonContainer = findViewById(R.id.layout_btn);

        findViewById(R.id.btn_close).setOnClickListener(view -> finish());

        findViewById(R.id.btn_download).setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(PhotoViewerActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        PhotoViewerActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_WRITE_EXTERNAL_STORAGE
                );
            } else {
                download();
            }
        });

        PhotoView photoView = (PhotoView) findViewById(R.id.main_image_view);
        photoView.setOnPhotoTapListener((view, x, y) -> {
            if (showButtons) {
                // Hides buttons.
                buttonContainer.setVisibility(View.GONE);
                showButtons = false;
            } else {
                // Shows buttons.
                buttonContainer.setVisibility(View.VISIBLE);
                showButtons = true;
            }
        });

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        if (type != null && type.toLowerCase().contains("gif")) {
            ImageUtils.displayGifImageFromUrl(this, mUrl, photoView, null, new RequestListener<GifDrawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }
            });
        } else {
            ImageUtils.displayImageFromUrl(this, mUrl, photoView, null, new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    progressBar.setVisibility(View.GONE);
                    return false;
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                download();
            }
        }
    }

    private void download() {
        Map<String, String> data = new HashMap<>();
        data.put("file_name", mName);
        data.put("url", mUrl);
        Event.onEvent(Event.EventListener.PHOTO_VIEWER_DOWNLOAD_FILE, data);

        FileUtils.downloadFile(this, mUrl, mName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Event.onEvent(Event.EventListener.PHOTO_VIEWER_ENTER, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Event.onEvent(Event.EventListener.PHOTO_VIEWER_EXIT, null);
    }
}
