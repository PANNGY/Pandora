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
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.request.ImageRequest;
import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.PandoraAdapter;
import com.github.gnastnosaj.pandora.datasource.service.GitOSCService;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.datasource.service.SplashService;
import com.github.gnastnosaj.pandora.event.TabEvent;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import br.com.mauker.materialsearchview.MaterialSearchView;
import butterknife.BindView;
import butterknife.ButterKnife;
import cn.trinea.android.common.util.ListUtils;
import cn.trinea.android.common.util.PackageUtils;
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

        initViewPager();
        initSearchView();
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

    private void initViewPager() {
        PandoraAdapter pandoraAdapter = new PandoraAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(pandoraAdapter);
        tabLayout.setupWithViewPager(viewPager);
        RxBus.getInstance().register(TabEvent.class, TabEvent.class)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(tabEvent -> viewPager.setCurrentItem(tabEvent.tab));
    }

    private void initSearchView() {
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    search(query);
                } catch (Exception e) {
                    Timber.e(e, "searchView onQueryText exception");
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnItemClickListener((adapterView, view, i, l) -> {
            try {
                ListView suggestionsListView = (ListView) searchView.findViewById(R.id.suggestion_list);
                if (suggestionsListView.getHeaderViewsCount() > 0) {
                    if (i == 0) {
                        searchView.clearAll();
                    } else {
                        String keyword = searchView.getSuggestionAtPosition(i - 1);
                        if (!TextUtils.isEmpty(keyword)) {
                            search(keyword);
                        }
                    }
                } else {
                    String keyword = searchView.getSuggestionAtPosition(i);
                    if (!TextUtils.isEmpty(keyword)) {
                        search(keyword);
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "searchView onItemClick exception");
            } finally {
                searchView.closeSearch();
            }
        });
        try {
            ListView suggestionsListView = (ListView) searchView.findViewById(R.id.suggestion_list);
            if (suggestionsListView.getHeaderViewsCount() == 0) {
                View deleteIconView = getLayoutInflater().inflate(R.layout.view_search_delete, null);
                suggestionsListView.addHeaderView(deleteIconView);
            }
        } catch (Exception e) {
            Timber.e(e, "initSearchView exception");
        }
    }

    private void search(String keyword) {
        GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);

        githubService.getJSoupDataSource(GithubService.DATE_SOURCE_LEEEBO_TAB)
                .flatMap(jsoupDataSource -> jsoupDataSource.searchData(keyword).onErrorReturn(throwable -> new ArrayList<>()))
                .zipWith(
                        githubService.getJSoupDataSource(GithubService.DATE_SOURCE_K8DY_TAB)
                                .flatMap(jsoupDataSource -> jsoupDataSource.searchData(keyword).onErrorReturn(throwable -> new ArrayList<>())),
                        (data1, data2) -> {
                            List<JSoupData> data = new ArrayList<>();
                            data.addAll(data1);
                            data.addAll(data2);
                            return data;
                        }).compose(bindUntilEvent(ActivityEvent.DESTROY))
                .switchIfEmpty(githubService.getJSoupDataSource(GithubService.DATE_SOURCE_JAVLIB_TAB).flatMap(jsoupDataSource -> jsoupDataSource.searchData(keyword)))
                .switchIfEmpty(githubService.getJSoupDataSource(GithubService.DATE_SOURCE_AVSOX_TAB).flatMap(jsoupDataSource -> jsoupDataSource.searchData(keyword)))
                .subscribeOn(Schedulers.newThread())
                .subscribe(data -> {
                    if (ListUtils.isEmpty(data)) {

                    }
                });
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int splashImageDataSource = sharedPreferences.getInt(SplashService.PRE_SPLASH_IMAGE_DATA_SOURCE, SplashService.SPLASH_IMAGE_DATA_SOURCE_GIRL_ATLAS);

        Single<String> splashImageSingle = null;

        switch (splashImageDataSource) {
            case SplashService.SPLASH_IMAGE_DATA_SOURCE_GANK:
                splashImageSingle = SplashService.gankSingle();
                break;
            case SplashService.SPLASH_IMAGE_DATA_SOURCE_GIRL_ATLAS:
                splashImageSingle = SplashService.girlAtlasSingle();
                break;
            case SplashService.SPLASH_IMAGE_DATA_SOURCE_NANRENCD:
                splashImageSingle = SplashService.nanrencdSingle();
                break;
            case SplashService.SPLASH_IMAGE_DATA_SOURCE_JAVLIB:
                splashImageSingle = SplashService.javlibSingle();
                break;
        }

        splashImageSingle
                .retry(3)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .subscribe(uriString -> {
                    Timber.d("next time splash image: %s", uriString);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(SplashService.PRE_SPLASH_IMAGE, uriString);
                    editor.apply();
                    Fresco.getImagePipeline().prefetchToDiskCache(ImageRequest.fromUri(uriString), this);
                });
    }
}
