/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sendbird.desk.android.sample.activity.chat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.app.Event;
import com.sendbird.desk.android.sample.utils.FileUtils;

import java.util.HashMap;
import java.util.Map;

public class MediaPlayerActivity extends Activity implements
        OnBufferingUpdateListener, OnCompletionListener,
        OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback {

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0xf0;

    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private String mUrl;
    private String mName;

    private ProgressBar mProgressBar;
    private boolean mShowButtons = true;
    private View mContainer;

    private int mVideoWidth;
    private int mVideoHeight;

    private boolean mIsVideoReadyToBePlayed = false;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsContainerSizeKnown = false;

    private boolean mIsPaused = false;
    private int mCurrentPosition = -1;

    @Override
    public void onCreate(@Nullable Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_media_player);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUrl = extras.getString("url");
            mName = extras.getString("name");
        }

        mProgressBar.setVisibility(View.VISIBLE);
        initToolbar();
    }

    private void initToolbar() {
        mContainer = findViewById(R.id.layout_media_player);
        final View buttonContainer = findViewById(R.id.layout_btn);

        findViewById(R.id.btn_close).setOnClickListener(view -> finish());

        findViewById(R.id.btn_download).setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(MediaPlayerActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MediaPlayerActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_WRITE_EXTERNAL_STORAGE
                );
            } else {
                download();
            }
        });

        mContainer.setOnTouchListener((view, motionEvent) -> {
            if (mShowButtons) {
                buttonContainer.setVisibility(View.GONE);
                mShowButtons = false;
            } else {
                buttonContainer.setVisibility(View.VISIBLE);
                mShowButtons = true;
            }
            return view.performClick();
        });

        setContainerLayoutListener(false);
    }

    private void setContainerLayoutListener(final boolean screenRotated) {
        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                mIsContainerSizeKnown = true;
                if (screenRotated) {
                    setVideoSize();
                } else {
                    tryToStartVideoPlayback();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
        Event.onEvent(Event.EventListener.VIDEO_PLAYER_DOWNLOAD_FILE, data);

        FileUtils.downloadFile(this, mUrl, mName);
    }

    private void playVideo() {
        mProgressBar.setVisibility(View.VISIBLE);
        doCleanUp();
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(mUrl);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    public void onCompletion(MediaPlayer mp) {
        finish();
    }

    public void onPrepared(MediaPlayer mediaplayer) {
        mIsVideoReadyToBePlayed = true;
        tryToStartVideoPlayback();
    }

    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }

        mVideoWidth = width;
        mVideoHeight = height;
        mIsVideoSizeKnown = true;
        tryToStartVideoPlayback();
    }

    public void surfaceChanged(@NonNull SurfaceHolder surfaceholder, int i, int j, int k) {
    }

    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceholder) {
    }

    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        playVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Event.onEvent(Event.EventListener.VIDEO_PLAYER_ENTER, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Event.onEvent(Event.EventListener.VIDEO_PLAYER_EXIT, null);
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
            mIsPaused = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        doCleanUp();
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void doCleanUp() {
        mVideoWidth = 0;
        mVideoHeight = 0;

        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

    private void tryToStartVideoPlayback() {
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown && mIsContainerSizeKnown) {
            startVideoPlayback();
        }
    }

    private void startVideoPlayback() {
        mProgressBar.setVisibility(View.INVISIBLE);
        if (!mMediaPlayer.isPlaying()) {
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
            setVideoSize();

            if (mIsPaused) {
                mMediaPlayer.seekTo(mCurrentPosition);
                mIsPaused = false;
            }
            mMediaPlayer.start();
        }
    }

    private void setVideoSize() {
        try {
            int videoWidth = mMediaPlayer.getVideoWidth();
            int videoHeight = mMediaPlayer.getVideoHeight();
            float videoProportion = (float) videoWidth / (float) videoHeight;

            int videoWidthInContainer = mContainer.getWidth();
            int videoHeightInContainer = mContainer.getHeight();
            float videoInContainerProportion = (float) videoWidthInContainer / (float) videoHeightInContainer;

            android.view.ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
            if (videoProportion > videoInContainerProportion) {
                lp.width = videoWidthInContainer;
                lp.height = (int) ((float) videoWidthInContainer / videoProportion);
            } else {
                lp.width = (int) (videoProportion * (float) videoHeightInContainer);
                lp.height = videoHeightInContainer;
            }
            mSurfaceView.setLayoutParams(lp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContainerLayoutListener(true);
    }
}
