package com.github.gnastnosaj.pandora.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.bilibili.socialize.share.core.shareparam.ShareParamText;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.SimplePagerAdapter;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.List;

import br.com.mauker.materialsearchview.MaterialSearchView;
import butterknife.BindView;
import butterknife.ButterKnife;

import cn.trinea.android.common.util.ListUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Created by jasontsang on 5/26/17.
 */

public class SimpleTabActivity extends BaseActivity {
    public final static String EXTRA_TITLE = "title";
    public final static String EXTRA_DATASOURCE = "datasource";

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

    private String title;
    private String datasource;

    private static List<JSoupLink> tabs;

    private GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);
    private RealmConfiguration tabCacheRealmConfig;
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

        title = getIntent().getStringExtra(EXTRA_TITLE);
        datasource = getIntent().getStringExtra(EXTRA_DATASOURCE);

        setTitle(TextUtils.isEmpty(title) ? "" : title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initViewPager();
        initSearchView();
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
            case android.R.id.home:
                onBackPressed();
                return true;
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
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initViewPager() {
        initTabs().compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tabs -> {
                    SimplePagerAdapter simplePagerAdapter = new SimplePagerAdapter(this, getSupportFragmentManager(), tabs, datasource);
                    viewPager.setAdapter(simplePagerAdapter);
                    tabLayout.setupWithViewPager(viewPager);
                });
    }

    private Observable<List<JSoupLink>> initTabs() {
        if (ListUtils.isEmpty(tabs)) {
            tabCacheRealmConfig = new RealmConfiguration.Builder().name(datasource + "_TAB_CACHE").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

            Realm realm = Realm.getInstance(tabCacheRealmConfig);
            RealmResults<JSoupLink> results = realm.where(JSoupLink.class).findAll();
            tabs = JSoupLink.from(results);
            realm.close();

            if (ListUtils.isEmpty(tabs)) {
                return githubService.getJSoupDataSource(datasource)
                        .flatMap(jsoupDataSource -> jsoupDataSource.loadTabs())
                        .flatMap(data -> {
                            tabs = data;
                            Realm bgRealm = Realm.getInstance(tabCacheRealmConfig);
                            bgRealm.executeTransactionAsync(bg -> {
                                bg.delete(JSoupLink.class);
                                bg.insertOrUpdate(tabs);
                            });
                            bgRealm.close();
                            return Observable.just(data);
                        });
            } else {
                githubService.getJSoupDataSource(datasource)
                        .flatMap(jsoupDataSource -> jsoupDataSource.loadTabs())
                        .subscribeOn(Schedulers.newThread())
                        .subscribe(data -> {
                            tabs = data;
                            Realm bgRealm = Realm.getInstance(tabCacheRealmConfig);
                            bgRealm.executeTransactionAsync(bg -> {
                                bg.delete(JSoupLink.class);
                                bg.insertOrUpdate(tabs);
                            });
                            bgRealm.close();
                        });
                return Observable.just(tabs);
            }
        } else {
            return Observable.just(tabs);
        }
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

        searchDataSource = null;

        githubService.getJSoupDataSource(GithubService.DATE_SOURCE_JAVLIB_TAB).flatMap(jsoupDataSource -> {
            searchDataSource = jsoupDataSource;
            return jsoupDataSource.searchData(keyword).onErrorReturn(throwable -> new ArrayList<>());
        }).switchMap((data -> {
            if (ListUtils.isEmpty(data)) {
                return githubService.getJSoupDataSource(GithubService.DATE_SOURCE_AVSOX_TAB).flatMap(jsoupDataSource -> {
                    searchDataSource = jsoupDataSource;
                    return jsoupDataSource.searchData(keyword).onErrorReturn(throwable -> new ArrayList<>());
                });
            } else {
                return Observable.just(data);
            }
        })).switchMap((data -> {
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
}
