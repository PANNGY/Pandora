package com.github.gnastnosaj.pandora.datasource.service;

import android.Manifest;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.io.File;

import cn.trinea.android.common.util.PackageUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import zlc.season.rxdownload2.RxDownload;

/**
 * Created by jasontsang on 5/30/17.
 */

public class UpdateService {
    public static void checkForUpdate(BaseActivity baseActivity) {
        checkForUpdate(baseActivity, false);
    }

    public static void checkForUpdate(BaseActivity baseActivity, boolean showAppUpdated) {
        new RxPermissions(baseActivity).request(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS)
                .compose(baseActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(grant -> {
                            AppUpdater appUpdater = new AppUpdater(baseActivity)
                                    .setDisplay(Display.DIALOG)
                                    .setUpdateFrom(UpdateFrom.JSON)
                                    .setUpdateJSON(baseActivity.getResources().getString(R.string.url_update))
                                    .showAppUpdated(showAppUpdated)
                                    .setButtonUpdateClickListener((dialog, which) ->
                                            Retrofit.newGithubServicePlus()
                                                    .getUpdateData()
                                                    .compose(baseActivity.bindUntilEvent(ActivityEvent.DESTROY))
                                                    .subscribeOn(Schedulers.newThread())
                                                    .subscribe(updateData -> RxDownload.getInstance(Boilerplate.getInstance())
                                                                    .download(updateData.url)
                                                                    .compose(baseActivity.bindUntilEvent(ActivityEvent.DESTROY))
                                                                    .subscribeOn(Schedulers.io())
                                                                    .observeOn(AndroidSchedulers.mainThread())
                                                                    .subscribe(downloadStatus -> {
                                                                            },
                                                                            throwable -> Timber.w(throwable, "update download exception"),
                                                                            () -> {
                                                                                File[] files = RxDownload.getInstance(Boilerplate.getInstance()).getRealFiles(updateData.url);
                                                                                if (files != null) {
                                                                                    File file = files[0];
                                                                                    PackageUtils.install(Boilerplate.getInstance(), file.getPath());
                                                                                }
                                                                            }),
                                                            throwable -> Timber.w(throwable, "update check exception"))
                                    );
                            appUpdater.start();
                        },
                        throwable -> Timber.w(throwable, "update permission exception"));
    }
}
