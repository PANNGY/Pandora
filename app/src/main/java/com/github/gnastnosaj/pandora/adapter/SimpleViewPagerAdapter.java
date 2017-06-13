package com.github.gnastnosaj.pandora.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.github.gnastnosaj.pandora.ui.fragment.SimpleTabFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontsang on 5/26/17.
 */

public class SimpleViewPagerAdapter extends FragmentPagerAdapter {
    private List<JSoupLink> tabs;
    private List<Fragment> fragments;

    public SimpleViewPagerAdapter(FragmentManager fm, List<JSoupLink> tabs, String tabDataSource, String galleryDataSource) {
        super(fm);
        this.tabs = new ArrayList<>(tabs);
        fragments = new ArrayList<>();
        for (JSoupLink tab : this.tabs) {
            fragments.add(SimpleTabFragment.newInstance(tab.url, tabDataSource, galleryDataSource));
        }
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabs.get(position).title;
    }
}
