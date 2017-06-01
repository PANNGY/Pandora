package com.github.gnastnosaj.pandora.datasource.service;

import android.content.Context;
import android.content.Intent;

import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.ui.activity.SimpleViewPagerActivity;

/**
 * Created by jasontsang on 6/1/17.
 */

public class PluginService {
    public static void start(Context context, Plugin plugin) {
        if (plugin.type == Plugin.TYPE_JSOUP_GALLERY) {
            context.startActivity(new Intent(context, SimpleViewPagerActivity.class)
                    .putExtra(SimpleViewPagerActivity.EXTRA_TITLE, plugin.name)
                    .putExtra(SimpleViewPagerActivity.EXTRA_TAB_DATASOURCE, plugin.reference + "-tab")
                    .putExtra(SimpleViewPagerActivity.EXTRA_GALLERY_DATASOURCE, plugin.reference + "-gallery"));
        }
    }
}
