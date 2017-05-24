package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;

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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 5/23/17.
 */

public class PandoraTabDataSource implements IDataSource<List<JSoupData>>, IDataCacheLoader<List<JSoupData>> {
    private GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);

    private RealmConfiguration realmConfig;

    private JSoupDataSource leeeboTabDataSource;
    private JSoupDataSource k8dyTabDataSource;
    private String leeeboTabPage;
    private String k8dyTabPage;

    private CountDownLatch initLock;
    private CountDownLatch refreshLock;
    private CountDownLatch loadMoreLock;

    public PandoraTabDataSource(Context context, int tab) {
        realmConfig = new RealmConfiguration.Builder().name("PANDORA_TAB_" + tab).schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

        initLock = new CountDownLatch(1);

        Observable init = Observable.zip(githubService.getJSoupDataSource(GithubService.DATE_SOURCE_LEEEBO_TAB),
                githubService.getJSoupDataSource(GithubService.DATE_SOURCE_K8DY_TAB),
                (leeeboTabDataSource, k8dyTabDataSource) -> {
                    leeeboTabDataSource.setNextPage(leeeboTabDataSource.baseUrl + leeeboTabDataSource.pages[tab]);
                    k8dyTabDataSource.setNextPage(k8dyTabDataSource.baseUrl + k8dyTabDataSource.pages[tab]);
                    this.leeeboTabDataSource = leeeboTabDataSource;
                    this.k8dyTabDataSource = k8dyTabDataSource;
                    return true;
                }).retry();

        if (context instanceof BaseActivity) {
            init = init.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
        }

        init.subscribeOn(Schedulers.newThread())
                .subscribe(success -> initLock.countDown());
    }

    @Override
    public List<JSoupData> loadCache(boolean isEmpty) {
        Realm realm = Realm.getInstance(realmConfig);
        RealmResults<JSoupData> results = realm.where(JSoupData.class).findAll();
        JSoupData[] data = new JSoupData[results.size()];
        results.toArray(data);
        return Arrays.asList(data);
    }

    @Override
    public List<JSoupData> refresh() throws Exception {
        if (refreshLock != null) {
            refreshLock.await();
        }
        refreshLock = new CountDownLatch(1);

        List<JSoupData> data = new ArrayList<>();

        initLock.await();

        Observable<List<JSoupData>> leeeboTabLoadData = leeeboTabDataSource.loadData(true);
        Observable<List<JSoupData>> k8dyTabLoadData = k8dyTabDataSource.loadData(true);

        if (leeeboTabDataSource.getNextPage().equals(leeeboTabDataSource.baseUrl)) {
            leeeboTabDataSource.setNextPage(null);
            leeeboTabLoadData = Observable.create(subscriber -> {
                subscriber.onNext(new ArrayList<>());
                subscriber.onComplete();
            });
        }

        if (k8dyTabDataSource.getNextPage().equals(k8dyTabDataSource.baseUrl)) {
            k8dyTabDataSource.setNextPage(null);
            k8dyTabLoadData = Observable.create(subscriber -> {
                subscriber.onNext(new ArrayList<>());
                subscriber.onComplete();
            });
        }

        Observable<List<JSoupData>> loadData = Observable.zip(leeeboTabLoadData.onErrorReturn((throwable -> new ArrayList<>())),
                k8dyTabLoadData.onErrorReturn((throwable -> new ArrayList<>())),
                (leeeboTabData, k8dyTabData) -> {
                    List<JSoupData> jsoupData = new ArrayList<>();
                    jsoupData.addAll(leeeboTabData);
                    jsoupData.addAll(k8dyTabData);
                    return jsoupData;
                });

        loadData.subscribeOn(Schedulers.newThread())
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
        if (loadMoreLock != null) {
            loadMoreLock.await();
        }
        loadMoreLock = new CountDownLatch(1);

        List<JSoupData> data = new ArrayList<>();

        Observable<List<JSoupData>> leeeboTabLoadData = leeeboTabDataSource.loadData();
        Observable<List<JSoupData>> k8dyTabLoadData = k8dyTabDataSource.loadData();

        if (!leeeboTabDataSource.hasMore()) {
            leeeboTabLoadData = Observable.create(subscriber -> {
                subscriber.onNext(new ArrayList<>());
                subscriber.onComplete();
            });
        }

        if (!k8dyTabDataSource.hasMore()) {
            k8dyTabLoadData = Observable.create(subscriber -> {
                subscriber.onNext(new ArrayList<>());
                subscriber.onComplete();
            });
        }

        Observable<List<JSoupData>> loadData = Observable.zip(leeeboTabLoadData.onErrorReturn((throwable -> new ArrayList<>())),
                k8dyTabLoadData.onErrorReturn((throwable -> new ArrayList<>())),
                (leeeboTabData, k8dyTabData) -> {
                    List<JSoupData> jsoupData = new ArrayList<>();
                    jsoupData.addAll(leeeboTabData);
                    jsoupData.addAll(k8dyTabData);
                    return jsoupData;
                });

        loadData.subscribeOn(Schedulers.newThread())
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
        return leeeboTabDataSource.hasMore() || leeeboTabDataSource.hasMore();
    }
}