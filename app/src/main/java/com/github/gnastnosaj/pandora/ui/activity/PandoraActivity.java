package com.github.gnastnosaj.pandora.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.GitOSCService;
import com.github.gnastnosaj.pandora.datasource.GithubService;
import com.github.gnastnosaj.pandora.datasource.Retrofit;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.io.File;
import java.util.concurrent.TimeUnit;

import cn.trinea.android.common.util.PackageUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import zlc.season.rxdownload2.RxDownload;

/**
 * Created by jasontsang on 4/23/17.
 */

public class PandoraActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkForUpdate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void checkForUpdate() {
        Observable.interval(0, 1, TimeUnit.HOURS)
                .flatMap(aLong -> new RxPermissions(this).request(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS))
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(grant -> {
                    AppUpdater appUpdater = new AppUpdater(this)
                            .setDisplay(Display.DIALOG)
                            .setUpdateFrom(UpdateFrom.JSON)
                            .setUpdateJSON(getResources().getString(R.string.update_url))
                            .setButtonUpdateClickListener((dialog, which) ->
                                    Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class)
                                            .getUpdateData()
                                            .timeout(3, TimeUnit.SECONDS, Retrofit.newSimpleService(GitOSCService.BASE_URL, GitOSCService.class).getUpdateData())
                                            .compose(bindUntilEvent(ActivityEvent.DESTROY))
                                            .subscribeOn(Schedulers.newThread())
                                            .subscribe(updateData -> RxDownload.getInstance(Boilerplate.getInstance())
                                                            .download(updateData.url)
                                                            .compose(bindUntilEvent(ActivityEvent.DESTROY))
                                                            .subscribeOn(Schedulers.io())
                                                            .observeOn(AndroidSchedulers.mainThread())
                                                            .subscribe(downloadStatus -> {
                                                            }, throwable -> Timber.e(throwable, "update download exception"), () -> {
                                                                File[] files = RxDownload.getInstance(Boilerplate.getInstance()).getRealFiles(updateData.url);
                                                                if (files != null) {
                                                                    File file = files[0];
                                                                    PackageUtils.install(Boilerplate.getInstance(), file.getPath());
                                                                }
                                                            })
                                                    , throwable -> Timber.e(throwable, "update check exception"))
                            );
                    appUpdater.start();
                });
    }
}
