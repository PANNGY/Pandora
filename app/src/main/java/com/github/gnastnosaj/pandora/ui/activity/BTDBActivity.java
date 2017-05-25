package com.github.gnastnosaj.pandora.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.BTDBAdapter;
import com.github.gnastnosaj.pandora.datasource.BTDBDataSource;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.List;

import br.com.mauker.materialsearchview.MaterialSearchView;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class BTDBActivity extends BaseActivity {

    public static final String EXTRA_KEYWORD = "keyword";
    public static final String EXTRA_TITLE = "title";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.search_view)
    MaterialSearchView searchView;

    private BTDBDataSource btdbDataSource;
    private BTDBAdapter btdbAdapter;
    private MVCHelper<List<JSoupData>> mvcHelper;

    private GestureDetector gestureDetector;

    private String keyword;
    private String title;

    @Override
    public void onBackPressed() {
        if (searchView.isOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btdb);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        keyword = getIntent().getStringExtra(EXTRA_KEYWORD);
        title = getIntent().getStringExtra(EXTRA_TITLE);

        setTitle(title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                setTitle(query);
                btdbDataSource.setKeyword(query);
                mvcHelper.refresh();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnItemClickListener((adapterView, view, i, l) -> {
            try {
                keyword = searchView.getSuggestionAtPosition(i);
                setTitle(keyword);
                btdbDataSource.setKeyword(keyword);
                mvcHelper.refresh();
                searchView.closeSearch();
            } catch (Exception e) {
                Timber.e(e, "BTDBActivity searchView OnItemClick exception");
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this).size(1).build());
        gestureDetector = new GestureDetector(BTDBActivity.this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent event) {
                super.onLongPress(event);
                try {
                    View childView = recyclerView.findChildViewUnder(event.getX(), event.getY());
                    int childPosition = recyclerView.getChildAdapterPosition(childView);
                    JSoupData jsoupData = btdbAdapter.getData().get(childPosition);
                    ClipData clipData = ClipData.newPlainText("Magnet Link", jsoupData.getAttr("magnet"));
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(clipData);
                    Snackbar.make(recyclerView, R.string.copy_magnet_success, Snackbar.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Timber.e(e, "onLongPress exception");
                }
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    View childView = rv.findChildViewUnder(event.getX(), event.getY());
                    int childPosition = rv.getChildAdapterPosition(childView);
                    if (-1 < childPosition && childPosition < btdbAdapter.getData().size()) {
                        JSoupData jsoupData = btdbAdapter.getData().get(childPosition);
                        Uri uri = Uri.parse(jsoupData.getAttr("magnet"));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(WebVideoViewActivity.EXTRA_ID, keyword);
                        intent.putExtra(WebVideoViewActivity.EXTRA_TITLE, jsoupData.getAttr("title"));
                        startActivity(intent);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });

        btdbAdapter = new BTDBAdapter();
        btdbDataSource = new BTDBDataSource(this);
        btdbDataSource.setKeyword(keyword);

        mvcHelper = new MVCSwipeRefreshHelper<>(swipeRefreshLayout);
        mvcHelper.setDataSource(btdbDataSource);
        mvcHelper.setAdapter(btdbAdapter);
        mvcHelper.refresh();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_btdb, menu);
        menu.findItem(R.id.action_search).setIcon(new IconicsDrawable(this)
                .icon(GoogleMaterial.Icon.gmd_search)
                .color(Color.WHITE).sizeDp(18));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_search:
                searchView.openSearch();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
