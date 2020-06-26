package com.sendbird.desk.android.sample.activity.chat;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sendbird.desk.android.sample.R;
import com.sendbird.desk.android.sample.app.Event;

import java.net.URL;

public class WebActivity extends AppCompatActivity {

    public static final String INTENT_EXTRA_URL = "INTENT_EXTRA_URL";

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private TextView mTextViewTitle;
    private TextView mTextViewSubTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        init();
        setWebView();
        loadUrl();
    }

    private void init() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("TEST");
            actionBar.setDisplayHomeAsUpEnabled(true);

            TypedArray ta = obtainStyledAttributes(new int[]{R.attr.deskCloseIcon});
            actionBar.setHomeAsUpIndicator(getResources().getDrawable(ta.getResourceId(0, R.drawable.btn_close)));
            ta.recycle();
        }

        mTextViewTitle = (TextView)findViewById(R.id.text_view_web_title);
        mTextViewSubTitle = (TextView)findViewById(R.id.text_view_web_sub_title);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void setWebView() {
        mWebView = (WebView)findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (title != null && title.length() > 0) {
                    if (!title.startsWith("http")) {
                        mTextViewTitle.setText(title);
                    }
                }
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mTextViewSubTitle.setText(getBaseUrl(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                mProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private String getBaseUrl(String urlString) {
        String baseUrl = "";
        try {
            if (urlString != null && urlString.length() > 0) {
                URL url = new URL(urlString);
                baseUrl = url.getProtocol() + "://" + url.getHost();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baseUrl;
    }

    private void loadUrl() {
        String url = null;
        Intent intent = getIntent();
        if (intent != null) {
            url = intent.getStringExtra(INTENT_EXTRA_URL);
        }

        if (url != null && url.length() > 0) {
            mWebView.loadUrl(url);
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            Event.onEvent(Event.EventListener.WEB_VIEWER_RELOAD, null);
            mWebView.reload();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Event.onEvent(Event.EventListener.WEB_VIEWER_ENTER, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Event.onEvent(Event.EventListener.WEB_VIEWER_EXIT, null);
    }
}
