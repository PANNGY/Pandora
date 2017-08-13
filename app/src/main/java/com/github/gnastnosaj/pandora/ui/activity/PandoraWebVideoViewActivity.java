package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bilibili.socialize.share.core.shareparam.ShareParamText;
import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.JSoupCatalog;
import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.next.tagview.TagCloudView;

/**
 * Created by jason on 8/17/2016.
 */
public class PandoraWebVideoViewActivity extends BaseActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_HREF = "href";
    public static final String EXTRA_RESOURCE = "resource";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.web_view)
    WebView webView;

    @BindView(R.id.video_player)
    StandardGSYVideoPlayer videoPlayer;

    @BindView(R.id.detail_resource_container)
    LinearLayout resourceContainer;

    private String title;
    private String href;
    private List<JSoupCatalog> resource;

    private String videoHref;
    private String videoSrc;
    private String frameSrc;

    @Override
    public void onBackPressed() {
        if (StandardGSYVideoPlayer.backFromWindowFull(this)) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pandora_web_video_view);
        ButterKnife.bind(this);

        createDynamicBox(findViewById(R.id.video_container));

        setSupportActionBar(toolbar);
        initSystemBar();

        title = getIntent().getStringExtra(EXTRA_TITLE);
        href = getIntent().getStringExtra(EXTRA_HREF);
        resource = getIntent().getParcelableArrayListExtra(EXTRA_RESOURCE);

        setTitle(title);
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

        loadUrl(href);

        resourceContainer.removeAllViews();
        for (int i = 0; i < resource.size(); i++) {
            JSoupCatalog catalog = resource.get(i);
            View resourceView = getLayoutInflater().inflate(R.layout.item_pandora_resource, null, false);
            ((TextView) resourceView.findViewById(R.id.detail_resource_title)).setText(TextUtils.isEmpty(catalog.link.title) ? ("#" + (i + 1)) : catalog.link.title);
            TagCloudView resourceCloudView = resourceView.findViewById(R.id.detail_resource_cloud);
            List<String> tagList = new ArrayList<>();
            for (JSoupLink link : catalog.tags) {
                tagList.add(link.title);
            }
            resourceCloudView.setTags(tagList);
            resourceCloudView.setOnTagClickListener((item) -> {
                String url = catalog.tags.get(item).url;
                startActivity(new Intent(this, PandoraWebVideoViewActivity.class)
                        .putExtra(PandoraWebVideoViewActivity.EXTRA_HREF, url)
                        .putExtra(PandoraWebVideoViewActivity.EXTRA_TITLE, title)
                        .putParcelableArrayListExtra(PandoraWebVideoViewActivity.EXTRA_RESOURCE, (ArrayList<? extends Parcelable>) resource)
                        .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
            });
            resourceContainer.addView(resourceView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        GSYVideoManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onResume();
        }
        GSYVideoManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
        GSYVideoPlayer.releaseAllVideos();
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
                ShareHelper.share(this, new ShareParamText(title, TextUtils.isEmpty(videoHref) ? href : videoHref));
                return true;
            case R.id.action_open_with_browser:
                if (!TextUtils.isEmpty(videoHref)) {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(videoHref)));
                } else if (!TextUtils.isEmpty(href)) {
                    startActivity(new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(href)));
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUrl(String url) {
        videoHref = null;
        videoSrc = null;
        frameSrc = null;
        if (url != null) {
            showDynamicBoxCustomView(DYNAMIC_BOX_AV_BALLGRIDPULSE, this);
            webView.loadUrl(url);
        }
    }

    private void extractVideoSrc() {
        String extractScript = "javascript:(" +
                "function() {" +
                "String.prototype.startWith = function(str){" +
                "var reg=new RegExp(\"^\"+str);" +
                "return reg.test(this);" +
                "};" +
                "var extractVideoSrc = setInterval(function() {" +
                "var source = document.getElementsByTagName('source').item(0);" +
                "if(source && source.src) {" +
                "clearInterval(extractVideoSrc);" +
                "alert('video.src=' + source.src);" +
                "return;" +
                "}" +
                "var video = document.getElementsByTagName('video').item(0);" +
                "if(video && video.src) {" +
                "clearInterval(extractVideoSrc);" +
                "alert('video.src=' + video.src);" +
                "return;" +
                "}" +
                "var iframe, subiframe;" +
                "var item = 0;" +
                "var subitem = 0;" +
                "while(true) {" +
                "iframe = document.getElementsByTagName('iframe').item(item++);" +
                "if(!iframe || !iframe.contentWindow || !iframe.contentWindow.document) {" +
                "break;" +
                "}" +
                "video = iframe.contentWindow.document.getElementsByTagName('video').item(0);" +
                "if(video && video.src) {" +
                "clearInterval(extractVideoSrc);" +
                "alert('iframe.src=' + iframe.src);" +
                "alert('video.src=' + video.src);" +
                "return;" +
                "}" +
                "source = iframe.contentWindow.document.getElementsByTagName('source').item(0);" +
                "if(source && source.src) {" +
                "clearInterval(extractVideoSrc);" +
                "alert('iframe.src=' + iframe.src);" +
                "alert('video.src=' + source.src);" +
                "return;" +
                "}" +
                "subitem = 0;" +
                "while(true) {" +
                "try{" +
                "subiframe = iframe.contentWindow.document.getElementsByTagName('iframe').item(subitem++);" +
                "if(!subiframe || !subiframe.contentWindow || !subiframe.contentWindow.document) {" +
                "break;" +
                "}" +
                "}catch(e) {" +
                "if(!subiframe.src.startWith('http://pl.263gmail.org') && !subiframe.src.startWith('http://slb.jiedubang.cn')) {" +
                "alert('subiframe.src=' + subiframe.src);" +
                "}" +
                "}" +
                "}" +
                "}" +
                "}, 2000);" +
                "setTimeout(function() {" +
                "clearInterval(extractVideoSrc);" +
                "}, 20000);" +
                "}" +
                ")()";
        if (!TextUtils.isEmpty(extractScript)) {
            webView.loadUrl(extractScript);
        }
    }

    private boolean isVideoSrc(String videoSrc) {
        if (videoSrc.startsWith("https://k8dy.top/api/")) {
            return false;
        } else {
            return true;
        }
    }

    private class WebVideoViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            extractVideoSrc();
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
                showDynamicBoxExceptionLayout(PandoraWebVideoViewActivity.this);
            } else if (title.equals("about:blank")) {

            } else {
                extractVideoSrc();
            }
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if (message.startsWith("iframe.src=")) {
                videoHref = message.substring(11);
            }
            if (message.startsWith("subiframe.src=") && frameSrc == null) {
                frameSrc = message.substring(14);
                webView.loadUrl(frameSrc);
            } else if (message.startsWith("video.src=") && videoSrc == null) {
                webView.loadUrl("about:blank");
                videoSrc = message.substring(10);
                if (isVideoSrc(videoSrc)) {
                    videoPlayer.setUp(videoSrc, true, null, null);
                    videoPlayer.getBackButton().setVisibility(View.GONE);
                    videoPlayer.getFullscreenButton().setOnClickListener(v -> videoPlayer.startWindowFullscreen(PandoraWebVideoViewActivity.this, true, true));
                    videoPlayer.setRotateViewAuto(true);
                    videoPlayer.setLockLand(true);
                    videoPlayer.setShowFullAnimation(true);
                    videoPlayer.setNeedLockFull(true);
                    videoPlayer.findViewById(R.id.start).performClick();
                    dismissDynamicBox(PandoraWebVideoViewActivity.this);
                } else {
                    webView.loadUrl(videoSrc);
                    videoSrc = null;
                }
            }
            result.cancel();
            return true;
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
