package com.github.gnastnosaj.pandora.ui.widget;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;

import com.shizhefei.mvc.ILoadViewFactory;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.viewhandler.ViewHandler;

/**
 * Created by jasontsang on 5/27/17.
 */

public class ViewPagerViewHandler implements ViewHandler {
    @Override
    public boolean handleSetAdapter(View contentView, Object viewAdapter, ILoadViewFactory.ILoadMoreView loadMoreView, View.OnClickListener onClickLoadMoreListener) {
        ViewPager viewPager = (ViewPager) contentView;
        boolean hasInit = false;
        if (loadMoreView != null) {
            loadMoreView.init(new ILoadViewFactory.FootViewAdder() {
                @Override
                public View addFootView(View view) {
                    return view;
                }

                @Override
                public View addFootView(int layoutId) {
                    View view = LayoutInflater.from(viewPager.getContext()).inflate(layoutId, viewPager, false);
                    return addFootView(view);
                }

                @Override
                public View getContentView() {
                    return viewPager;
                }
            }, onClickLoadMoreListener);
            hasInit = true;
        }
        viewPager.setAdapter((PagerAdapter) viewAdapter);
        return hasInit;
    }

    @Override
    public void setOnScrollBottomListener(View contentView, MVCHelper.OnScrollBottomListener onScrollBottomListener) {
        ViewPager viewPager = (ViewPager) contentView;
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == viewPager.getAdapter().getCount() - 1) {
                    onScrollBottomListener.onScorllBootom();
                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
