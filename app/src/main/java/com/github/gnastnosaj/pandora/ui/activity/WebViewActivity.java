package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.bilibili.socialize.share.core.shareparam.ShareParamText;
import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import mehdi.sakout.dynamicbox.DynamicBox;
import timber.log.Timber;

/**
 * Created by jason on 8/17/2016.
 */
public class WebViewActivity extends BaseActivity {
    public static final String EXTRA_HREF = "href";
    public static final String EXTRA_TITLE = "title";

    public static final String HACK_CSS_DIR = "hack_css/";

    @BindView(R.id.app_bar)
    AppBarLayout appBar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.web_view)
    WebView webView;

    private DynamicBox dynamicBox;

    private String href;
    private String title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_video_view);
        ButterKnife.bind(this);

        dynamicBox = createDynamicBox(swipeRefreshLayout);

        setSupportActionBar(toolbar);
        initSystemBar();

        href = getIntent().getStringExtra(EXTRA_HREF);
        title = getIntent().getStringExtra(EXTRA_TITLE);

        setTitle(TextUtils.isEmpty(title) ? "" : title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        webView.setWebViewClient(new WebVideoViewClient());
        webView.setWebChromeClient(new WebChrome());
        WebSettings settings = webView.getSettings();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setJavaScriptEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSaveFormData(false);
        settings.setAppCacheEnabled(true);
        settings.setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Boilerplate.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!TextUtils.isEmpty(href)) {
                webView.loadUrl(href);
            }
            Observable.timer(500, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((l) ->
                            swipeRefreshLayout.setRefreshing(false));
        });

        if (!TextUtils.isEmpty(href)) {
            showDynamicBoxCustomView(DYNAMIC_BOX_AV_BALLGRIDPULSE, this);
            webView.loadUrl(href);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_web_video_view, menu);

        menu.findItem(R.id.action_share).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_open_with_browser).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_open_in_browser)
                .color(Color.WHITE).sizeDp(18));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_share:
                ShareHelper.share(this, new ShareParamText(title, href));
                return true;
            case R.id.action_open_with_browser:
                if (!TextUtils.isEmpty(href)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(href));
                    startActivity(intent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void injectCSS(String filename) {
        try {
            InputStream inputStream = getAssets().open(HACK_CSS_DIR + filename);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()");
        } catch (Exception e) {
            Timber.e(e, "injectCSS exception");
        }
    }

    private class WebVideoViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }
    }

    private class WebChrome extends WebChromeClient implements MediaPlayer.OnCompletionListener {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (title.equals("Webpage not available")) {
                webView.setVisibility(View.INVISIBLE);
                showDynamicBoxExceptionLayout(WebViewActivity.this);
            } else {
                webView.setVisibility(View.VISIBLE);
                dismissDynamicBox(WebViewActivity.this);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (mp != null) {
                if (mp.isPlaying()) {
                    mp.stop();
                }
                mp.reset();
                mp.release();
            }
        }
    }

}
