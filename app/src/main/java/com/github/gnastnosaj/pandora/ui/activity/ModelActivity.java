package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.ModelAdapter;
import com.github.gnastnosaj.pandora.datasource.SimpleDataSource;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jasontsang on 6/13/17.
 */

public class ModelActivity extends BaseActivity {
    public final static String EXTRA_TAB_DATASOURCE = "tab_datasource";
    public final static String EXTRA_GALLERY_DATASOURCE = "gallery_datasource";
    public final static String EXTRA_MODEL_DATASOURCE = "model_datasource";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private String tabDataSource;
    private String galleryDataSource;
    private String modelDataSource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_coordinator_with_recycler_view);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        tabDataSource = getIntent().getStringExtra(EXTRA_TAB_DATASOURCE);
        galleryDataSource = getIntent().getStringExtra(EXTRA_GALLERY_DATASOURCE);
        modelDataSource = getIntent().getStringExtra(EXTRA_MODEL_DATASOURCE);

        setTitle(R.string.action_model);
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
        ModelAdapter modelAdapter = new ModelAdapter(this);

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
                    if (-1 < childPosition && childPosition < modelAdapter.getData().size()) {
                        JSoupData data = modelAdapter.getData().get(childPosition);
                        startActivity(new Intent(ModelActivity.this, SimpleTabActivity.class)
                                .putExtra(SimpleTabActivity.EXTRA_TITLE, data.getAttr("title"))
                                .putExtra(SimpleTabActivity.EXTRA_HREF, data.getAttr("href"))
                                .putExtra(SimpleTabActivity.EXTRA_TAB_DATASOURCE, tabDataSource)
                                .putExtra(SimpleTabActivity.EXTRA_GALLERY_DATASOURCE, galleryDataSource));
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
        SimpleDataSource simpleDataSource = new SimpleDataSource(this, modelDataSource, null);
        mvcHelper.setDataSource(simpleDataSource);
        mvcHelper.setAdapter(modelAdapter);
        mvcHelper.refresh();
    }
}
