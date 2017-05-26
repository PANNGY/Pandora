package com.github.gnastnosaj.pandora.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.ui.activity.BTDBActivity;
import com.github.gnastnosaj.pandora.ui.activity.GalleryActivity;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.List;

import br.com.mauker.materialsearchview.MaterialSearchView;
import cn.trinea.android.common.util.ListUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by jasontsang on 5/27/17.
 */

public class SearchHelper {
    private Context context;
    private ProgressBar progressBar;
    private MaterialSearchView searchView;

    private JSoupDataSource searchDataSource;

    private SearchHelper(Context context, ProgressBar progressBar, MaterialSearchView searchView) {
        this.context = context;
        this.progressBar = progressBar;
        this.searchView = searchView;
    }

    private void search(String keyword) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        if (searchView != null) {
            Snackbar.make(searchView, R.string.searching, Snackbar.LENGTH_LONG).show();
        } else if (context instanceof BaseActivity) {
            Snackbar.make(((BaseActivity) context).findViewById(android.R.id.content), R.string.searching, Snackbar.LENGTH_LONG).show();
        }

        GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);

        searchDataSource = null;

        Observable<List<JSoupData>> search = githubService.getJSoupDataSource(GithubService.DATE_SOURCE_LEEEBO_TAB)
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
                }));

        if (context instanceof BaseActivity) {
            search = search.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
        }

        search.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (ListUtils.isEmpty(data)) {
                        if (searchView != null) {
                            Snackbar.make(searchView, R.string.searching, Snackbar.LENGTH_LONG).show();
                        } else if (context instanceof BaseActivity) {
                            Snackbar.make(searchView, R.string.search_result_not_found, Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        new AlertDialog.Builder(context)
                                .setMessage(R.string.search_result_found)
                                .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                                .setPositiveButton(R.string.action_check, (dialog, which) -> {
                                    if (searchDataSource.id.equals(GithubService.DATE_SOURCE_JAVLIB_TAB)) {
                                        JSoupData jsoupData = data.get(0);
                                        if (!TextUtils.isEmpty(jsoupData.getAttr("cover"))) {
                                            Intent i = new Intent(context, GalleryActivity.class);
                                            i.putExtra(GalleryActivity.EXTRA_DATASOURCE, GithubService.DATE_SOURCE_JAVLIB_GALLERY);
                                            i.putExtra(GalleryActivity.EXTRA_TITLE, keyword);
                                            i.putExtra(GalleryActivity.EXTRA_HREF, searchDataSource.getCurrentPage());
                                            i.putParcelableArrayListExtra(GalleryActivity.EXTRA_CACHE, (ArrayList<? extends Parcelable>) data);
                                            context.startActivity(i);
                                        } else {
                                            Intent i = new Intent(context, GalleryActivity.class);
                                            i.putExtra(GalleryActivity.EXTRA_DATASOURCE, GithubService.DATE_SOURCE_JAVLIB_GALLERY);
                                            i.putExtra(GalleryActivity.EXTRA_TITLE, keyword);
                                            i.putExtra(GalleryActivity.EXTRA_HREF, jsoupData.getAttr("url"));
                                            context.startActivity(i);
                                        }
                                    } else if (searchDataSource.id.equals(GithubService.DATE_SOURCE_BTDB)) {
                                        Intent i = new Intent(context, BTDBActivity.class);
                                        i.putExtra(BTDBActivity.EXTRA_KEYWORD, keyword);
                                        i.putExtra(BTDBActivity.EXTRA_TITLE, keyword);
                                        i.putParcelableArrayListExtra(BTDBActivity.EXTRA_CACHE, (ArrayList<? extends Parcelable>) data);
                                        context.startActivity(i);
                                    }
                                    dialog.dismiss();
                                }).setCancelable(false).show();
                    }
                });
    }

    public static void search(String keyword, Context context, ProgressBar progressBar, MaterialSearchView searchView) {
        new SearchHelper(context, progressBar, searchView).search(keyword);
    }
}
