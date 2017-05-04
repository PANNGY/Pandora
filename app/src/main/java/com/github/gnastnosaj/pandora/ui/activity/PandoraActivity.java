package com.github.gnastnosaj.pandora.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.GankService;
import com.github.gnastnosaj.pandora.datasource.GitOSCService;
import com.github.gnastnosaj.pandora.datasource.GithubService;
import com.github.gnastnosaj.pandora.datasource.Retrofit;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import br.com.mauker.materialsearchview.MaterialSearchView;
import butterknife.BindView;
import butterknife.ButterKnife;
import cn.trinea.android.common.util.PackageUtils;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import zlc.season.rxdownload2.RxDownload;

/**
 * Created by jasontsang on 4/23/17.
 */

public class PandoraActivity extends BaseActivity {
    @BindView(R.id.app_bar)
    AppBarLayout appBar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.tab)
    TabLayout tabLayout;

    @BindView(R.id.view_pager)
    ViewPager viewPager;

    @BindView(R.id.search_view)
    MaterialSearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_view_pager_with_tab);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        checkForUpdate();
        prepareSplashImage();
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
        new RxPermissions(this).request(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(grant -> {
                            AppUpdater appUpdater = new AppUpdater(this)
                                    .setDisplay(Display.DIALOG)
                                    .setUpdateFrom(UpdateFrom.JSON)
                                    .setUpdateJSON(getResources().getString(R.string.update_url))
                                    .setButtonUpdateClickListener((dialog, which) ->
                                            Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class)
                                                    .getUpdateData()
                                                    .timeout(5, TimeUnit.SECONDS, Retrofit.newSimpleService(GitOSCService.BASE_URL, GitOSCService.class).getUpdateData())
                                                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                                                    .subscribeOn(Schedulers.newThread())
                                                    .subscribe(updateData -> RxDownload.getInstance(Boilerplate.getInstance())
                                                                    .download(updateData.url)
                                                                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
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

    private void prepareSplashImage() {
        Single<String> splashImageSingle;
        if (Pandora.pro) {
            GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);
            splashImageSingle = githubService.getDataSource(GithubService.DATE_SOURCE_JAVLIB_TAB)
                    .flatMap(jsoupDataSource -> jsoupDataSource.loadData())
                    .map(data -> data.get(new Random().nextInt(data.size() - 1)).attrs.get("url"))
                    .flatMap(url -> githubService.getDataSource(GithubService.DATE_SOURCE_JAVLIB_GALLERY).flatMap(jsoupDataSource -> jsoupDataSource.loadData(url)))
                    .flatMap(data -> Observable.fromIterable(data))
                    .lastOrError()
                    .map(data -> data.attrs.get("cover"));
        } else {
            splashImageSingle = Retrofit.newSimpleService(GankService.BASE_URL, GankService.class)
                    .getGankData("福利", 1, 1)
                    .flatMap(gankData -> Observable.fromIterable(gankData.results))
                    .lastOrError()
                    .map(result -> result.url);
        }

        splashImageSingle
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uriString -> {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(SplashActivity.PRE_SPLASH_IMAGE, uriString);
                    editor.apply();
                }, throwable -> Timber.w(throwable, "prepare splash image exception"));
    }
}
