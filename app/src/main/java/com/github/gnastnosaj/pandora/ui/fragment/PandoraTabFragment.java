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

import com.daimajia.slider.library.SliderLayout;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.PandoraHomeAdapter;
import com.github.gnastnosaj.pandora.adapter.SimpleTabAdapter;
import com.github.gnastnosaj.pandora.datasource.PandoraHomeDataSource;
import com.github.gnastnosaj.pandora.datasource.PandoraHomeDataSource.Model;
import com.github.gnastnosaj.pandora.datasource.PandoraTabDataSource;
import com.github.gnastnosaj.pandora.event.TabEvent;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.ui.activity.PandoraDetailActivity;
import com.github.gnastnosaj.pandora.ui.widget.PinnedHeaderDecoration;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCSwipeRefreshHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

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

        PinnedHeaderDecoration decoration = new PinnedHeaderDecoration();
        decoration.registerTypePinnedHeader(1, (parent, adapterPosition) -> true);
        recyclerView.addItemDecoration(decoration);

        GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    View childView = rv.findChildViewUnder(event.getX(), event.getY());
                    int childPosition = rv.getChildAdapterPosition(childView);
                    if (-1 < childPosition && childPosition < pandoraHomeAdapter.getData().size()) {
                        Model model = pandoraHomeAdapter.getData().get(childPosition);
                        if (model.type == Model.TYPE_VALUE_SLIDE) {
                            Bundle bundle = ((SliderLayout) childView).getCurrentSlider().getBundle();
                            JSoupData data = bundle.getParcelable(PandoraHomeAdapter.SLIDE_BUNDLE_DATA);
                            Intent i = new Intent(getContext(), PandoraDetailActivity.class);
                            i.putExtra(PandoraDetailActivity.EXTRA_DATA, data);
                            startActivity(i);
                        } else if (model.type == Model.TYPE_VALUE_GROUP) {
                            String[] groups = pandoraHomeDataSource.getGroups();
                            for (int i = 0; i < groups.length; i++) {
                                String group = groups[i];
                                if (group.equals(model.data)) {
                                    RxBus.getInstance().post(TabEvent.TAG_PANDORA_TAB, new TabEvent(i));
                                    break;
                                }
                            }
                        } else if (model.type == Model.TYPE_VALUE_DATA) {
                            JSoupData data = (JSoupData) model.data;
                            Intent i = new Intent(getContext(), PandoraDetailActivity.class);
                            i.putExtra(PandoraDetailActivity.EXTRA_DATA, data);
                            startActivity(i);
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });

        MVCHelper mvcHelper = new MVCSwipeRefreshHelper<>(swipeRefreshLayout);
        mvcHelper.setDataSource(pandoraHomeDataSource);
        mvcHelper.setAdapter(pandoraHomeAdapter);

        mvcHelper.refresh();
    }

    private void initPandoraTabView() {
        PandoraTabDataSource pandoraTabDataSource = new PandoraTabDataSource(getActivity(), tab);
        SimpleTabAdapter pandoraTabAdapter = new SimpleTabAdapter();

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
                    if (-1 < childPosition && childPosition < pandoraTabAdapter.getData().size()) {
                        JSoupData data = pandoraTabAdapter.getData().get(childPosition);
                        Intent i = new Intent(getContext(), PandoraDetailActivity.class);
                        i.putExtra(PandoraDetailActivity.EXTRA_DATA, data);
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
        mvcHelper.setDataSource(pandoraTabDataSource);
        mvcHelper.setAdapter(pandoraTabAdapter);

        mvcHelper.refresh();
    }
}
