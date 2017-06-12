package com.github.gnastnosaj.pandora.datasource.service;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.PythonVideoDataSource;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.ui.activity.SimpleVideoInfoActivity;
import com.github.gnastnosaj.pandora.ui.activity.SimpleViewPagerActivity;
import com.github.gnastnosaj.pythonforandroid.PythonForAndroid;

import com.googlecode.android_scripting.BaseApplication;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

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
            Intent i = new Intent(context, SimpleViewPagerActivity.class)
                    .putExtra(SimpleViewPagerActivity.EXTRA_TITLE, plugin.name)
                    .putExtra(SimpleViewPagerActivity.EXTRA_TAB_DATASOURCE, plugin.reference + "-tab")
                    .putExtra(SimpleViewPagerActivity.EXTRA_GALLERY_DATASOURCE, plugin.reference + "-gallery");
            if (!TextUtils.isEmpty(plugin.args)) {
                try {
                    JSONObject args = new JSONObject(plugin.args);
                    if (args.getBoolean("model")) {
                        i = i.putExtra(SimpleViewPagerActivity.EXTRA_MODEL_DATASOURCE, plugin.reference + "-model");
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
            context.startActivity(i);
        } else if (plugin.type == Plugin.TYPE_PYTHON_VIDEO) {
            if (PythonForAndroid.isInitialized()) {
                startPythonPlugin(context, plugin);
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
                            } else {
                                Observable.timer(1, TimeUnit.SECONDS).subscribeOn(Schedulers.newThread()).subscribe(aLong -> {
                                    if (PythonForAndroid.isInitialized()) {
                                        startPythonPlugin(context, plugin);
                                    }
                                });
                            }
                        });
            }
        }
    }

    public static void startPythonPlugin(Context context, Plugin plugin) {
        Intent i = new Intent(context, SimpleVideoInfoActivity.class);
        i.putExtra(SimpleVideoInfoActivity.EXTRA_PLUGIN, plugin);
        i.putExtra(SimpleVideoInfoActivity.EXTRA_TYPE, PythonVideoDataSource.TYPE_CATEGORY);
        context.startActivity(i);
    }
}
