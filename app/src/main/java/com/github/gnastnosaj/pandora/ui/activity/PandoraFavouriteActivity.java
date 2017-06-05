package com.github.gnastnosaj.pandora.ui.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.SimpleTabAdapter;
import com.github.gnastnosaj.pandora.datasource.FavouriteDataSource;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jasontsang on 5/26/17.
 */

public class PandoraFavouriteActivity extends BaseActivity {
    public final static String DATA_SOURCE = "PANDORA";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

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

        setTitle(R.string.action_favourite);
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
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    View childView = rv.findChildViewUnder(event.getX(), event.getY());
                    int childPosition = rv.getChildAdapterPosition(childView);
                    if (-1 < childPosition && childPosition < simpleTabAdapter.getData().size()) {

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
        FavouriteDataSource favouriteDataSource = new FavouriteDataSource(DATA_SOURCE);
        mvcHelper.setDataSource(favouriteDataSource);
        mvcHelper.setAdapter(simpleTabAdapter);
        mvcHelper.refresh();
    }
}