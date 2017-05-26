package com.github.gnastnosaj.pandora.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.bilibili.socialize.share.core.shareparam.ShareParamText;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.request.ImageRequest;
import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.PandoraAdapter;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.GitOSCService;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.datasource.service.SplashService;
import com.github.gnastnosaj.pandora.event.TabEvent;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
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
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.tab)
    TabLayout tabLayout;

    @BindView(R.id.view_pager)
    ViewPager viewPager;

    @BindView(R.id.search_view)
    MaterialSearchView searchView;

    private Observable<TabEvent> tabEventObservable;

    private JSoupDataSource searchDataSource;

    @Override
    public void onBackPressed() {
        if (searchView.isOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_view_pager_with_tab);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        tabEventObservable = RxBus.getInstance().register(TabEvent.TAG_PANDORA_TAB, TabEvent.class);

        initViewPager();
        initSearchView();
        checkForUpdate();
        prepareSplashImage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.getInstance().unregister(TabEvent.TAG_PANDORA_TAB, tabEventObservable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pandora, menu);
        menu.findItem(R.id.action_search).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_search)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_share).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchView.openSearch();
                return true;
            case R.id.action_share:
                ShareHelper.share(this, new ShareParamText(getResources().getString(R.string.action_share), getResources().getString(R.string.share_pandora)));
                return true;
            case R.id.action_favourite:
                return true;
            case R.id.action_about:
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        tabEventObservable
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

        progressBar.setVisibility(View.VISIBLE);

        Snackbar.make(searchView, R.string.searching, Snackbar.LENGTH_LONG).show();

        GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);

        searchDataSource = null;

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
                        }
                )
                .switchMap((data -> {
                    if (ListUtils.isEmpty(data)) {
                        return githubService.getJSoupDataSource(GithubService.DATE_SOURCE_JAVLIB_TAB).flatMap(jsoupDataSource -> {
                            searchDataSource = jsoupDataSource;
                            return jsoupDataSource.searchData(keyword).onErrorReturn(throwable -> new ArrayList<>());
                        });
                    } else {
                        return Observable.just(data);
                    }
                }))
                .switchMap((data -> {
                    if (ListUtils.isEmpty(data)) {
                        return githubService.getJSoupDataSource(GithubService.DATE_SOURCE_AVSOX_TAB).flatMap(jsoupDataSource -> {
                            searchDataSource = jsoupDataSource;
                            return jsoupDataSource.searchData(keyword).onErrorReturn(throwable -> new ArrayList<>());
                        });
                    } else {
                        return Observable.just(data);
                    }
                }))
                .switchMap((data -> {
                    if (ListUtils.isEmpty(data)) {
                        return githubService.getJSoupDataSource(GithubService.DATE_SOURCE_BTDB).flatMap(jsoupDataSource -> {
                            searchDataSource = jsoupDataSource;
                            return jsoupDataSource.searchData(keyword).onErrorReturn(throwable -> new ArrayList<>());
                        });
                    } else {
                        return Observable.just(data);
                    }
                }))
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    progressBar.setVisibility(View.GONE);
                    if (ListUtils.isEmpty(data)) {
                        Snackbar.make(searchView, R.string.search_result_not_found, Snackbar.LENGTH_LONG).show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.search_result_found)
                                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                                .setPositiveButton(R.string.action_check, (dialog, which) -> {
                                    if (searchDataSource.id.equals(GithubService.DATE_SOURCE_JAVLIB_TAB)) {
                                        JSoupData jsoupData = data.get(0);
                                        if (!TextUtils.isEmpty(jsoupData.getAttr("cover"))) {
                                            Intent i = new Intent(this, GalleryActivity.class);
                                            i.putExtra(GalleryActivity.EXTRA_DATASOURCE, GithubService.DATE_SOURCE_JAVLIB_GALLERY);
                                            i.putExtra(GalleryActivity.EXTRA_TITLE, keyword);
                                            i.putExtra(GalleryActivity.EXTRA_HREF, searchDataSource.getCurrentPage());
                                            i.putParcelableArrayListExtra(GalleryActivity.EXTRA_CACHE, (ArrayList<? extends Parcelable>) data);
                                            startActivity(i);
                                        } else {
                                            Intent i = new Intent(this, GalleryActivity.class);
                                            i.putExtra(GalleryActivity.EXTRA_DATASOURCE, GithubService.DATE_SOURCE_JAVLIB_GALLERY);
                                            i.putExtra(GalleryActivity.EXTRA_TITLE, keyword);
                                            i.putExtra(GalleryActivity.EXTRA_HREF, jsoupData.getAttr("url"));
                                            startActivity(i);
                                        }
                                    } else if (searchDataSource.id.equals(GithubService.DATE_SOURCE_BTDB)) {
                                        Intent i = new Intent(this, BTDBActivity.class);
                                        i.putExtra(BTDBActivity.EXTRA_KEYWORD, keyword);
                                        i.putExtra(BTDBActivity.EXTRA_TITLE, keyword);
                                        i.putParcelableArrayListExtra(BTDBActivity.EXTRA_CACHE, (ArrayList<? extends Parcelable>) data);
                                        startActivity(i);
                                    }
                                    dialog.dismiss();
                                }).setCancelable(false).show();
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
