package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;
import android.text.TextUtils;

import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.IDataCacheLoader;
import com.shizhefei.mvc.IDataSource;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import cn.trinea.android.common.util.ListUtils;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 5/25/17.
 */

public class SearchDataSource implements IDataSource<List<JSoupData>>, IDataCacheLoader<List<JSoupData>> {

    private GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);

    private RealmConfiguration realmConfig;

    private Context context;

    private JSoupDataSource searchDataSource;

    private String keyword;
    private List<JSoupData> cache;

    private CountDownLatch initLock;
    private CountDownLatch refreshLock;
    private CountDownLatch loadMoreLock;

    public SearchDataSource(Context context, String dataSource, String keyword) {
        this.context = context;
        this.keyword = keyword;

        realmConfig = new RealmConfiguration.Builder().name(dataSource + (TextUtils.isEmpty(keyword) ? "" : ("_" + keyword.hashCode()))).schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

        initLock = new CountDownLatch(1);

        Observable<JSoupDataSource> init = githubService.getJSoupDataSource(dataSource);

        if (context instanceof BaseActivity) {
            init = init.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
        }

        init.subscribeOn(Schedulers.newThread())
                .subscribe(datasource -> {
                    searchDataSource = datasource;
                    initLock.countDown();
                });
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setCache(List<JSoupData> cache) {
        if (!ListUtils.isEmpty(cache)) {
            this.cache = new ArrayList<>(cache);
        }
    }

    @Override
    public List<JSoupData> loadCache(boolean isEmpty) {
        if (!ListUtils.isEmpty(cache)) {
            return cache;
        } else {
            Realm realm = Realm.getInstance(realmConfig);
            RealmResults<JSoupData> results = realm.where(JSoupData.class).findAll();
            List<JSoupData> data = JSoupData.from(results);
            realm.close();
            return data;
        }
    }

    @Override
    public List<JSoupData> refresh() throws Exception {
        if (TextUtils.isEmpty(keyword)) {
            throw new Exception("keyword is empty");
        }
        if (refreshLock != null) {
            refreshLock.await();
        }
        if (loadMoreLock != null) {
            loadMoreLock.await();
        }
        refreshLock = new CountDownLatch(1);

        List<JSoupData> data = new ArrayList<>();

        initLock.await();

        Observable<List<JSoupData>> refresh = searchDataSource.searchData(keyword, null, true);

        if (context instanceof BaseActivity) {
            refresh = refresh.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
        }

        refresh.subscribeOn(Schedulers.newThread())
                .subscribe(jsoupData -> {
                    data.addAll(jsoupData);
                    Realm realm = Realm.getInstance(realmConfig);
                    realm.executeTransactionAsync(bgRealm -> {
                        bgRealm.delete(JSoupData.class);
                        bgRealm.insertOrUpdate(data);
                    });
                    realm.close();
                    refreshLock.countDown();
                }, throwable -> refreshLock.countDown());

        refreshLock.await();

        return data;
    }

    @Override
    public List<JSoupData> loadMore() throws Exception {
        if (refreshLock != null) {
            refreshLock.await();
        }
        if (loadMoreLock != null) {
            loadMoreLock.await();
        }
        loadMoreLock = new CountDownLatch(1);

        List<JSoupData> data = new ArrayList<>();

        initLock.await();

        Observable<List<JSoupData>> loadMore = searchDataSource.searchData(keyword);

        if (context instanceof BaseActivity) {
            loadMore = loadMore.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
        }

        loadMore.subscribeOn(Schedulers.newThread())
                .subscribe(jsoupData -> {
                    data.addAll(jsoupData);
                    Realm realm = Realm.getInstance(realmConfig);
                    realm.executeTransactionAsync(bgRealm -> bgRealm.insertOrUpdate(data));
                    realm.close();
                    loadMoreLock.countDown();
                }, throwable -> loadMoreLock.countDown());

        loadMoreLock.await();

        return data;
    }

    @Override
    public boolean hasMore() {
        return searchDataSource.hasMore();
    }
}
