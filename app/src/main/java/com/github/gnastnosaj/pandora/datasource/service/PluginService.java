package com.github.gnastnosaj.pandora.datasource.service;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.ui.activity.SimpleViewPagerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontsang on 6/1/17.
 */

public class PluginService {
    public final static List<String> history = new ArrayList<>();

    public static void start(Context context, Plugin plugin) {
        if (plugin.desc.contains(Plugin.NSW)) {
            if (history.contains(plugin.id)) {
                startPlugin(context, plugin);
            } else {
                new AlertDialog.Builder(context)
                        .setMessage(R.string.adult_warning)
                        .setNegativeButton(R.string.adult_warning_not_18, (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(R.string.adult_warning_continue, (dialog, which) -> {
                            history.add(plugin.id);
                            startPlugin(context, plugin);
                            dialog.dismiss();
                        }).setCancelable(false).show();
            }
        } else {
            startPlugin(context, plugin);
        }
    }

    public static void startPlugin(Context context, Plugin plugin) {
        if (plugin.type == Plugin.TYPE_JSOUP_GALLERY) {
            context.startActivity(new Intent(context, SimpleViewPagerActivity.class)
                    .putExtra(SimpleViewPagerActivity.EXTRA_TITLE, plugin.name)
                    .putExtra(SimpleViewPagerActivity.EXTRA_TAB_DATASOURCE, plugin.reference + "-tab")
                    .putExtra(SimpleViewPagerActivity.EXTRA_GALLERY_DATASOURCE, plugin.reference + "-gallery"));
        }
    }
}
