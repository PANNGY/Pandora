package com.github.gnastnosaj.pandora.datasource.service;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.ui.activity.SimpleViewPagerActivity;
import com.github.gnastnosaj.pythonforandroid.PythonForAndroid;

import com.googlecode.android_scripting.BaseApplication;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.List;

import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

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
        } else if (plugin.type == Plugin.TYPE_PYTHON_VIDEO) {
            if (PythonForAndroid.isInitialized()) {
                Snackbar.make(((Activity) context).findViewById(android.R.id.content), "正在打开PYTHON插件，请稍候...", Snackbar.LENGTH_SHORT).show();
                JCVideoPlayerStandard.startFullscreen(context, JCVideoPlayerStandard.class, "http://2449.vod.myqcloud.com/2449_22ca37a6ea9011e5acaaf51d105342e3.f20.mp4", "嫂子辛苦了");
            } else {
                Snackbar.make(((Activity) context).findViewById(android.R.id.content), R.string.plugin_center_python_initialize, Snackbar.LENGTH_SHORT).show();
                Observable<Boolean> initialize = new RxPermissions((Activity) context).request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET)
                        .flatMap(granted -> Observable.create(subscriber -> {
                            if (granted) {
                                PythonForAndroid.initialize((BaseApplication) Boilerplate.getInstance());
                                subscriber.onNext(true);
                            } else {
                                subscriber.onNext(false);
                            }
                            subscriber.onComplete();
                        }));

                if (context instanceof BaseActivity) {
                    initialize = initialize.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
                }

                initialize.subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(success -> {
                            if (!success) {
                                Snackbar.make(((Activity) context).findViewById(android.R.id.content), R.string.plugin_center_python_initialize_fail, Snackbar.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
}
