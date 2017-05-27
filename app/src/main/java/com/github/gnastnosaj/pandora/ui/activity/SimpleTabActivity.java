package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.SimpleTabAdapter;
import com.github.gnastnosaj.pandora.datasource.FavouriteDataSource;
import com.github.gnastnosaj.pandora.datasource.SimpleDataSource;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;

import java.util.List;

import br.com.mauker.materialsearchview.MaterialSearchView;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jasontsang on 5/26/17.
 */

public class SimpleTabActivity extends BaseActivity {
    public final static String EXTRA_TAB_DATASOURCE = "tab_datasource";
    public final static String EXTRA_GALLERY_DATASOURCE = "gallery_datasource";
    public final static String EXTRA_TITLE = "title";
    public final static String EXTRA_HREF = "href";
    public final static String EXTRA_CACHE = "cache";
    public final static String EXTRA_TYPE = "type";

    public final static int TYPE_DEFAULT = 0;
    public final static int TYPE_FAVOURITE = 1;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.search_view)
    MaterialSearchView searchView;

    private String tabDataSource;
    private String galleryDataSource;
    private int type;
    private String title;
    private String href;
    private List<JSoupData> cache;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_coordinator_with_recycler_view);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        tabDataSource = getIntent().getStringExtra(EXTRA_TAB_DATASOURCE);
        galleryDataSource = getIntent().getStringExtra(EXTRA_GALLERY_DATASOURCE);
        type = getIntent().getIntExtra(EXTRA_TYPE, TYPE_DEFAULT);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        href = getIntent().getStringExtra(EXTRA_HREF);
        cache = getIntent().getParcelableArrayListExtra(EXTRA_CACHE);

        setTitle(TextUtils.isEmpty(title) ? "" : title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initContentView();
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

    private void initContentView() {
        SimpleTabAdapter simpleTabAdapter = new SimpleTabAdapter(this);

        int spanCount = getResources().getInteger(R.integer.pandora_tab_grid_span_count);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL);
        staggeredGridLayoutManager.setItemPrefetchEnabled(false);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    View childView = rv.findChildViewUnder(event.getX(), event.getY());
                    int childPosition = rv.getChildAdapterPosition(childView);
                    if (-1 < childPosition && childPosition < simpleTabAdapter.getData().size()) {
                        JSoupData data = simpleTabAdapter.getData().get(childPosition);
                        Intent i = new Intent(SimpleTabActivity.this, GalleryActivity.class);
                        i.putExtra(GalleryActivity.EXTRA_TAB_DATASOURCE, tabDataSource);
                        i.putExtra(GalleryActivity.EXTRA_GALLERY_DATASOURCE, galleryDataSource);
                        i.putExtra(GalleryActivity.EXTRA_DATA, data);
                        startActivity(i);
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

        MVCHelper mvcHelper = new MVCSwipeRefreshHelper<>(swipeRefreshLayout);
        if (type == TYPE_DEFAULT) {
            SimpleDataSource simpleDataSource = new SimpleDataSource(this, tabDataSource, href);
            simpleDataSource.setCache(cache);
            mvcHelper.setDataSource(simpleDataSource);
        } else if (type == TYPE_FAVOURITE) {
            FavouriteDataSource favouriteDataSource = new FavouriteDataSource(tabDataSource);
            mvcHelper.setDataSource(favouriteDataSource);
        }
        mvcHelper.setAdapter(simpleTabAdapter);
        mvcHelper.refresh();
    }
}