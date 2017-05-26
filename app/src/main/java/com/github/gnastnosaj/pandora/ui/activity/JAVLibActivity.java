package com.github.gnastnosaj.pandora.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;

import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.List;

import cn.trinea.android.common.util.ListUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 5/26/17.
 */

public class JAVLibActivity extends SimpleTabActivity {

    private RealmConfiguration realmConfig = new RealmConfiguration.Builder().name("_JAVLIB_TABS_CACHE_").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

    private GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);

    private static List<JSoupLink> tabs;

    private JSoupDataSource searchDataSource;

    @Override
    protected String getDataSource() {
        return GithubService.DATE_SOURCE_JAVLIB_TAB;
    }

    @Override
    protected void search(String keyword) {

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

    @Override
    protected Observable<List<JSoupLink>> initTabs() {
        if (ListUtils.isEmpty(tabs)) {
            Realm realm = Realm.getInstance(realmConfig);
            RealmResults<JSoupLink> results = realm.where(JSoupLink.class).findAll();
            tabs = JSoupLink.from(results);
            realm.close();

            if (ListUtils.isEmpty(tabs)) {
                return githubService.getJSoupDataSource(getDataSource())
                        .flatMap(jsoupDataSource -> jsoupDataSource.loadTabs())
                        .flatMap(data -> {
                            tabs = data;
                            Realm bgRealm = Realm.getInstance(realmConfig);
                            bgRealm.executeTransactionAsync(bg -> {
                                bg.delete(JSoupLink.class);
                                bg.insertOrUpdate(tabs);
                            });
                            bgRealm.close();
                            return Observable.just(data);
                        });
            } else {
                githubService.getJSoupDataSource(getDataSource())
                        .flatMap(jsoupDataSource -> jsoupDataSource.loadTabs())
                        .subscribeOn(Schedulers.newThread())
                        .subscribe(data -> {
                            tabs = data;
                            Realm bgRealm = Realm.getInstance(realmConfig);
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
}
