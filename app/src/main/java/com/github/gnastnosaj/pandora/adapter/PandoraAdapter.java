package com.github.gnastnosaj.pandora.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.ui.fragment.PandoraTabFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontsang on 5/24/17.
 */

public class PandoraAdapter extends FragmentPagerAdapter {
    private String[] tabs;
    private List<Fragment> fragments;

    public PandoraAdapter(Context context, FragmentManager fm) {
        super(fm);
        tabs = context.getResources().getStringArray(R.array.pandora_tabs);
        fragments = new ArrayList<>();
        for (int i = 0; i < tabs.length; i++) {
            fragments.add(PandoraTabFragment.newInstance(i));
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
        return tabs[position];
    }
}
