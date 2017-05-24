package com.github.gnastnosaj.pandora.ui.fragment;

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
import com.github.gnastnosaj.pandora.adapter.PandoraHomeAdapter;
import com.github.gnastnosaj.pandora.adapter.PandoraTabAdapter;
import com.github.gnastnosaj.pandora.datasource.PandoraHomeDataSource;
import com.github.gnastnosaj.pandora.datasource.PandoraHomeDataSource.Model;
import com.github.gnastnosaj.pandora.datasource.PandoraTabDataSource;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by jasontsang on 5/24/17.
 */

public class PandoraTabFragment extends Fragment {

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private int tab;
    private View rootView;
    private GestureDetector gestureDetector;
    private MVCHelper mvcHelper;

    public static PandoraTabFragment newInstance(int tab) {
        PandoraTabFragment instance = new PandoraTabFragment();
        instance.tab = tab;
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.layout_recycler_view_with_swipe_refresh, container, false);
            ButterKnife.bind(this, rootView);
            if (tab == 0) {
                initPandoraHomeView();
            } else {
                initPandoraTabView();
            }
        }
        return rootView;
    }

    private void initPandoraHomeView() {
        PandoraHomeDataSource pandoraHomeDataSource = new PandoraHomeDataSource(getActivity());
        PandoraHomeAdapter pandoraHomeAdapter = new PandoraHomeAdapter(getActivity());

        int spanCount = getResources().getInteger(R.integer.pandora_tab_grid_span_count);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL);
        staggeredGridLayoutManager.setItemPrefetchEnabled(false);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);

        gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
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
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (gestureDetector.onTouchEvent(e)) {
                    try {
                        View childView = rv.findChildViewUnder(e.getX(), e.getY());
                        int childPosition = rv.getChildAdapterPosition(childView);
                        Model model = pandoraHomeAdapter.getData().get(childPosition);
                        if (model.type == Model.TYPE_VALUE_SLIDE) {

                        } else if (model.type == Model.TYPE_VALUE_GROUP) {

                        } else if (model.type == Model.TYPE_VALUE_DATA) {

                        }
                    } catch (Exception exception) {
                        Timber.e(exception, "recyclerView touch exception");
                    }
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

        mvcHelper = new MVCSwipeRefreshHelper<>(swipeRefreshLayout);
        mvcHelper.setDataSource(pandoraHomeDataSource);
        mvcHelper.setAdapter(pandoraHomeAdapter);

        mvcHelper.refresh();
    }

    private void initPandoraTabView() {
        PandoraTabDataSource pandoraTabDataSource = new PandoraTabDataSource(getActivity(), tab);
        PandoraTabAdapter pandoraTabAdapter = new PandoraTabAdapter(getActivity());

        int spanCount = getResources().getInteger(R.integer.pandora_tab_grid_span_count);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL);
        staggeredGridLayoutManager.setItemPrefetchEnabled(false);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);

        gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
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
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (gestureDetector.onTouchEvent(e)) {
                    try {
                        View childView = rv.findChildViewUnder(e.getX(), e.getY());
                        int childPosition = rv.getChildAdapterPosition(childView);
                        JSoupData data = pandoraTabAdapter.getData().get(childPosition);
                    } catch (Exception exception) {
                        Timber.e(exception, "recyclerView touch exception");
                    }
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

        mvcHelper = new MVCSwipeRefreshHelper<>(swipeRefreshLayout);
        mvcHelper.setDataSource(pandoraTabDataSource);
        mvcHelper.setAdapter(pandoraTabAdapter);

        mvcHelper.refresh();
    }
}
