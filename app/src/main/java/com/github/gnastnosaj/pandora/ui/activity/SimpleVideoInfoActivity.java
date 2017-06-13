package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
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
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.SimpleVideoInfoAdapter;
import com.github.gnastnosaj.pandora.datasource.PythonVideoDataSource;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.model.VideoInfo;
import com.shizhefei.mvc.ILoadViewFactory;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import butterknife.BindView;
import butterknife.ButterKnife;

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

    private boolean isFull;
    private boolean isSmall;

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

        initContentView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GSYVideoManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        GSYVideoManager.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation != ActivityInfo.SCREEN_ORIENTATION_USER) {
            isFull = false;
        } else {
            isFull = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GSYVideoPlayer.releaseAllVideos();
    }

    private void initContentView() {
        PythonVideoDataSource videoInfoDataSource = new PythonVideoDataSource(this, plugin, type, videoInfo == null ? null : videoInfo.id);
        SimpleVideoInfoAdapter videoInfoAdapter = new SimpleVideoInfoAdapter(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this).size(1).build());
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
                            startActivity(new Intent(SimpleVideoInfoActivity.this, SimpleVideoInfoActivity.class)
                                    .putExtra(SimpleVideoInfoActivity.EXTRA_PLUGIN, plugin)
                                    .putExtra(SimpleVideoInfoActivity.EXTRA_TYPE, PythonVideoDataSource.TYPE_VIDEO)
                                    .putExtra(SimpleVideoInfoActivity.EXTRA_VIDEO_INFO, videoInfo));
                        } else if (type == PythonVideoDataSource.TYPE_VIDEO) {
                            if (TextUtils.isEmpty(videoInfo.url)) {
                                startActivity(new Intent(SimpleVideoInfoActivity.this, SimpleVideoInfoActivity.class)
                                        .putExtra(SimpleVideoInfoActivity.EXTRA_PLUGIN, plugin)
                                        .putExtra(SimpleVideoInfoActivity.EXTRA_TYPE, PythonVideoDataSource.TYPE_VIDEO_INFO)
                                        .putExtra(SimpleVideoInfoActivity.EXTRA_VIDEO_INFO, videoInfo));
                            } else {
                                return false;
                            }
                        } else if (type == PythonVideoDataSource.TYPE_VIDEO_INFO) {
                            return false;
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

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            int firstVisibleItem, lastVisibleItem;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                if (GSYVideoManager.instance().getPlayPosition() >= 0 && GSYVideoManager.instance().getPlayTag().equals(SimpleVideoInfoAdapter.PLAY_TAG)) {
                    int position = GSYVideoManager.instance().getPlayPosition();
                    if ((position < firstVisibleItem || position > lastVisibleItem)) {
                        if (!isFull) {
                            GSYVideoPlayer.releaseAllVideos();
                        }
                    }
                }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
