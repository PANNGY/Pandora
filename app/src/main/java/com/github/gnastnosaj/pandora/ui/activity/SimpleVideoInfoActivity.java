package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.github.gnastnosaj.boilerplate.mvchelper.LoadViewFactory;
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
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.trinea.android.common.util.ListUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
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

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

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
        setContentView(R.layout.layout_coordinator_with_recycler_view);
        ButterKnife.bind(this);

        createDynamicBox(findViewById(R.id.swipe_refresh_layout));

        setSupportActionBar(toolbar);
        initSystemBar();

        plugin = getIntent().getParcelableExtra(EXTRA_PLUGIN);
        type = getIntent().getIntExtra(EXTRA_TYPE, PythonVideoDataSource.TYPE_CATEGORY);
        videoInfo = getIntent().getParcelableExtra(EXTRA_VIDEO_INFO);

        title = videoInfo == null ? plugin.name : videoInfo.title;
        setTitle(title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        videoEventObservable = RxBus.getInstance().register(title, VideoEvent.class);

        initContentView();
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
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    View childView = rv.findChildViewUnder(event.getX(), event.getY());
                    int childPosition = rv.getChildAdapterPosition(childView);
                    if (-1 < childPosition && childPosition < videoInfoAdapter.getData().size()) {
                        VideoInfo videoInfo = videoInfoAdapter.getData().get(childPosition);
                        if (type == PythonVideoDataSource.TYPE_CATEGORY) {
                            Intent intent = new Intent(SimpleVideoInfoActivity.this, SimpleVideoInfoActivity.class);
                            intent.putExtra(SimpleVideoInfoActivity.EXTRA_PLUGIN, plugin);
                            intent.putExtra(SimpleVideoInfoActivity.EXTRA_TYPE, PythonVideoDataSource.TYPE_VIDEO);
                            intent.putExtra(SimpleVideoInfoActivity.EXTRA_VIDEO_INFO, videoInfo);
                            startActivity(intent);
                        } else if (type == PythonVideoDataSource.TYPE_VIDEO) {
                            showDynamicBoxCustomView(BaseActivity.DYNAMIC_BOX_AV_BALLGRIDPULSE, SimpleVideoInfoActivity.this);
                            Observable.<List<VideoInfo>>create(subscriber -> {
                                try {
                                    PythonVideoDataSource videoSourceDataSource = new PythonVideoDataSource(SimpleVideoInfoActivity.this, plugin, PythonVideoDataSource.TYPE_VIDEO_INFO, videoInfo.id);
                                    List<VideoInfo> videoSourceList = videoSourceDataSource.refresh();
                                    subscriber.onNext(videoSourceList);
                                } catch (Exception e) {
                                    subscriber.onError(e);
                                    Timber.e(e, "videoSourceDataSource exception");
                                }
                            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(videoSourceList -> {
                                dismissDynamicBox(SimpleVideoInfoActivity.this);
                                if (videoSourceList.size() == 1) {
                                    VideoInfo videoSource = videoSourceList.get(0);
                                    if (videoSource.url.startsWith("stack://")) {
                                        title = videoSource.title;
                                        String[] urls = videoSource.url.substring(8).split(" , ");
                                        SimpleVideoInfoActivity.this.urls = new LinkedList<>();
                                        SimpleVideoInfoActivity.this.urls.addAll(Arrays.asList(urls));
                                        url = SimpleVideoInfoActivity.this.urls.pop();
                                        VideoPlayer.startFullscreen(SimpleVideoInfoActivity.this, VideoPlayer.class, url, title);
                                    } else if (videoSource.url.startsWith("https")) {
                                        Intent intent = new Intent(SimpleVideoInfoActivity.this, WebVideoViewActivity.class);
                                        intent.putExtra(WebVideoViewActivity.EXTRA_TITLE, videoSource.title);
                                        intent.putExtra(WebVideoViewActivity.EXTRA_HREF, videoSource.url);
                                        startActivity(intent);
                                    } else {
                                        VideoPlayer.startFullscreen(SimpleVideoInfoActivity.this, VideoPlayer.class, videoSource.url, videoSource.title);
                                    }
                                } else if (videoSourceList.size() > 1) {
                                    Intent intent = new Intent(SimpleVideoInfoActivity.this, SimpleVideoInfoActivity.class);
                                    intent.putExtra(SimpleVideoInfoActivity.EXTRA_PLUGIN, plugin);
                                    intent.putExtra(SimpleVideoInfoActivity.EXTRA_TYPE, PythonVideoDataSource.TYPE_VIDEO_INFO);
                                    intent.putExtra(SimpleVideoInfoActivity.EXTRA_VIDEO_INFO, videoInfo);
                                    startActivity(intent);
                                }
                            }, throwable -> {
                                dismissDynamicBox(SimpleVideoInfoActivity.this);
                                Timber.e(throwable, "videoSourceDataSource exception");
                            });
                        }
                    } else if (type == PythonVideoDataSource.TYPE_VIDEO_INFO) {
                        if (videoInfo.url.startsWith("stack://")) {
                            title = videoInfo.title;
                            String[] urls = videoInfo.url.substring(8).split(" , ");
                            SimpleVideoInfoActivity.this.urls = new LinkedList<>();
                            SimpleVideoInfoActivity.this.urls.addAll(Arrays.asList(urls));
                            url = SimpleVideoInfoActivity.this.urls.pop();
                            VideoPlayer.startFullscreen(SimpleVideoInfoActivity.this, VideoPlayer.class, url, title);
                        } else if (videoInfo.url.startsWith("https")) {
                            Intent intent = new Intent(SimpleVideoInfoActivity.this, WebVideoViewActivity.class);
                            intent.putExtra(WebVideoViewActivity.EXTRA_TITLE, videoInfo.title);
                            intent.putExtra(WebVideoViewActivity.EXTRA_HREF, videoInfo.url);
                            startActivity(intent);
                        } else {
                            VideoPlayer.startFullscreen(SimpleVideoInfoActivity.this, VideoPlayer.class, videoInfo.url, videoInfo.title);
                        }
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
                    showDynamicBoxCustomView(BaseActivity.DYNAMIC_BOX_AV_BALLGRIDPULSE, SimpleVideoInfoActivity.this);
                } else {
                    swipeRefreshLayout.setRefreshing(true);
                }
            }

            @Override
            public void showFail(Exception e) {
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
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                } else {
                    dismissDynamicBox(SimpleVideoInfoActivity.this);
                }
            }
        };

        MVCHelper mvcHelper = new MVCSwipeRefreshHelper(swipeRefreshLayout, loadView, new LoadViewFactory().madeLoadMoreView());
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
