package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.SimpleVideoInfoAdapter;
import com.github.gnastnosaj.pandora.datasource.PythonVideoDataSource;
import com.github.gnastnosaj.pandora.event.VideoEvent;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.model.VideoInfo;
import com.github.gnastnosaj.pandora.ui.widget.VideoPlayer;
import com.shizhefei.mvc.ILoadViewFactory;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.Arrays;
import java.util.LinkedList;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.trinea.android.common.util.ListUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

/**
 * Created by jasontsang on 12/26/16.
 */

public class SimpleVideoInfoActivity extends BaseActivity {
    public final static String EXTRA_PLUGIN = "plugin";
    public final static String EXTRA_TYPE = "type";
    public final static String EXTRA_VIDEO_INFO = "videoInfo";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private Plugin plugin;
    private int type;
    private VideoInfo videoInfo;
    private String title;

    private Observable<VideoEvent> videoEventObservable;

    private String url;
    private LinkedList<String> urls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_recycler_view_with_swipe_refresh);
        ButterKnife.bind(this);

        createDynamicBox(findViewById(R.id.swipe_refresh_layout));

        setSupportActionBar(toolbar);
        initSystemBar();

        plugin = getIntent().getParcelableExtra(EXTRA_PLUGIN);
        type = getIntent().getIntExtra(EXTRA_TYPE, PythonVideoDataSource.TYPE_CATEGORY);
        videoInfo = getIntent().getParcelableExtra(EXTRA_VIDEO_INFO);
        title = videoInfo == null ? plugin.name : videoInfo.title;
        setTitle(title);
        if (videoInfo != null) {
            setTitle(videoInfo.title);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        videoEventObservable = RxBus.getInstance().register(title, VideoEvent.class);
    }

    private void initContentView() {
        PythonVideoDataSource videoInfoDataSource = new PythonVideoDataSource(this, plugin, type, videoInfo == null ? null : videoInfo.id);
        SimpleVideoInfoAdapter videoInfoAdapter = new SimpleVideoInfoAdapter(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this).build());
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (gestureDetector.onTouchEvent(e)) {
                    try {
                        View childView = rv.findChildViewUnder(e.getX(), e.getY());
                        int childPosition = rv.getChildAdapterPosition(childView);
                        VideoInfo videoInfo = videoInfoAdapter.getData().get(childPosition);
                        if (videoInfo != null) {
                            showDynamicBoxCustomView(BaseActivity.DYNAMIC_BOX_BALLPULSE);
                            Observable.create(subscriber -> {
                                try {
                                    VideoSourceDataSource videoSourceDataSource = new VideoSourceDataSource(plugin, videoInfo);
                                    List<VideoSource> videoSourceList = videoSourceDataSource.refresh();
                                    subscriber.onNext(videoSourceList);
                                } catch (Exception e1) {
                                    subscriber.onError(e1);
                                    Timber.e(e1, "VideoInfoActivity videoSourceDataSource error");
                                }
                            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(data -> {
                                dimissDynamicBox();
                                List<VideoSource> videoSourceList = (List<VideoSource>) data;
                                if (videoSourceList.size() == 1) {
                                    VideoSource videoSource = videoSourceList.get(0);
                                    if (videoSource.getUrl().startsWith("stack://")) {
                                        title = videoSource.getTitle();
                                        String[] urls = videoSource.getUrl().substring(8).split(" , ");
                                        VideoInfoActivity.this.urls = new LinkedList<>();
                                        VideoInfoActivity.this.urls.addAll(Arrays.asList(urls));
                                        url = VideoInfoActivity.this.urls.pop();
                                        VideoPlayer.startFullscreen(VideoInfoActivity.this, VideoPlayer.class, url, title);
                                    } else if (videoSource.getUrl().startsWith("https")) {
                                        Intent intent = new Intent(VideoInfoActivity.this, WebVideoViewActivity.class);
                                        intent.putExtra(WebVideoViewActivity.EXTRA_URL, videoSource.getUrl());
                                        intent.putExtra(WebVideoViewActivity.EXTRA_TITLE, videoSource.getTitle());
                                        startActivity(intent);
                                    } else {
                                        VideoPlayer.startFullscreen(VideoInfoActivity.this, VideoPlayer.class, videoSource.getUrl(), videoSource.getTitle());
                                    }
                                } else if (videoSourceList.size() > 1) {
                                    Intent intent = new Intent(VideoInfoActivity.this, VideoSourceActivity.class);
                                    intent.putExtra(VideoSourceActivity.EXTRA_PLUGIN, plugin);
                                    intent.putExtra(VideoSourceActivity.EXTRA_VIDEO_INFO, videoInfo);
                                    startActivity(intent);
                                }
                            }, throwable -> {
                                dimissDynamicBox();
                                Timber.e(throwable, "VideoInfoActivity videoSourceDataSource error");
                            });
                        }
                    } catch (Exception e2) {
                        dimissDynamicBox();
                        Timber.e(e2, "VideoInfoActivity recyclerView touch error");
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        ILoadViewFactory.ILoadView loadView = new ILoadViewFactory.ILoadView() {
            @Override
            public void init(View switchView, View.OnClickListener onClickRefreshListener) {

            }

            @Override
            public void showLoading() {
                if (videoInfoAdapter.getData().size() == 0) {
                    showDynamicBoxCustomView(BaseActivity.DYNAMIC_BOX_BALLPULSE);
                } else {
                    refreshView.setRefreshing(true);
                }
            }

            @Override
            public void showFail(Exception e) {
                showErrorView();
            }

            @Override
            public void showEmpty() {
                restore();
            }

            @Override
            public void tipFail(Exception e) {
            }

            @Override
            public void restore() {
                if (refreshView.isRefreshing()) {
                    refreshView.setRefreshing(false);
                } else {
                    dimissDynamicBox();
                }
            }
        };

        mvcHelper = new MVCSwipeRefreshHelper<>(refreshView, loadView, new LoadViewFactory().madeLoadMoreView());
        mvcHelper.setDataSource(videoInfoDataSource);
        mvcHelper.setAdapter(videoInfoAdapter);

        mvcHelper.refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.getInstance().unregister(title, videoEventObservable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoEventObservable.compose(bindToLifecycle()).subscribe(videoEvent -> {
            if (videoEvent.type == VideoEvent.TYPE_ON_AUTO_COMPLETION) {
                if (!ListUtils.isEmpty(urls)) {
                    url = urls.pop();
                    if (!TextUtils.isEmpty(url)) {
                        VideoPlayer.startFullscreen(this, VideoPlayer.class, url, title);
                    }
                }
            } else if (videoEvent.type == VideoEvent.TYPE_ON_FULLSCREEN) {
                VideoPlayer.destroy();
                Snackbar.make(recyclerView, R.string.video_player_error, Snackbar.LENGTH_SHORT).show();
            } else if (videoEvent.type == VideoEvent.TYPE_ON_ERROR) {
                VideoPlayer.destroy();
            }
        }, throwable -> Timber.e(throwable, "videoEventObservable exception"));
        VideoPlayer.resume();
    }

    @Override
    public void onBackPressed() {
        if (VideoPlayer.destroy()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
