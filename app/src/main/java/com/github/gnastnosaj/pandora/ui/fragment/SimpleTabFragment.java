package com.github.gnastnosaj.pandora.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.SimpleTabAdapter;
import com.github.gnastnosaj.pandora.datasource.SimpleDataSource;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.ui.activity.GalleryActivity;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jasontsang on 5/24/17.
 */

public class SimpleTabFragment extends Fragment {
    public final static String STATE_HREF = "href";
    public final static String STATE_TAB_DATASOURCE = "tabDataSource";
    public final static String STATE_GALLERY_DATASOURCE = "galleryDataSource";

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private String href;
    private String tabDataSource;
    private String galleryDataSource;

    private View rootView;

    public static SimpleTabFragment newInstance(String href, String tabDataSource, String galleryDataSource) {
        SimpleTabFragment instance = new SimpleTabFragment();
        instance.href = href;
        instance.tabDataSource = tabDataSource;
        instance.galleryDataSource = galleryDataSource;
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.layout_recycler_view_with_swipe_refresh, container, false);
            ButterKnife.bind(this, rootView);

            if (savedInstanceState != null) {
                href = savedInstanceState.getString(STATE_HREF);
                tabDataSource = savedInstanceState.getString(STATE_TAB_DATASOURCE);
                galleryDataSource = savedInstanceState.getString(STATE_GALLERY_DATASOURCE);
            }
            initSimpleTabView();
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_HREF, href);
        outState.putString(STATE_TAB_DATASOURCE, tabDataSource);
        outState.putString(STATE_GALLERY_DATASOURCE, galleryDataSource);
    }

    private void initSimpleTabView() {
        SimpleDataSource simpleDataSource = new SimpleDataSource(getContext(), tabDataSource, href);
        SimpleTabAdapter simpleTabAdapter = new SimpleTabAdapter();

        int spanCount = getResources().getInteger(R.integer.pandora_tab_grid_span_count);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL);
        staggeredGridLayoutManager.setItemPrefetchEnabled(false);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);

        GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
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
                        JSoupData data = simpleTabAdapter.getData().get(childPosition);
                        startActivity(new Intent(getContext(), GalleryActivity.class)
                                .putExtra(GalleryActivity.EXTRA_TAB_DATASOURCE, tabDataSource)
                                .putExtra(GalleryActivity.EXTRA_GALLERY_DATASOURCE, galleryDataSource)
                                .putExtra(GalleryActivity.EXTRA_DATA, data));
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
        mvcHelper.setDataSource(simpleDataSource);
        mvcHelper.setAdapter(simpleTabAdapter);

        mvcHelper.refresh();
    }
}
