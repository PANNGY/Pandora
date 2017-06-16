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
 * Created by jasontsang on 5/23/17.
 */

public class PandoraTabDataSource extends RxDataSource<List<JSoupData>> implements IDataCacheLoader<List<JSoupData>> {
    private GithubService githubService = Retrofit.newGithubServicePlus();

    private RealmConfiguration realmConfig;

    private int tab;
    private String keyword;
    private List<JSoupData> cache;

    private JSoupDataSource leeeboTabDataSource;
    private JSoupDataSource k8dyTabDataSource;

    public PandoraTabDataSource(Context context, String keyword) {
        super(context);

        realmConfig = new RealmConfiguration.Builder().name("PANDORA_TAB_" + keyword).schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

        this.keyword = keyword;
    }

    public PandoraTabDataSource(Context context, int tab) {
        super(context);

        realmConfig = new RealmConfiguration.Builder().name("PANDORA_TAB_" + tab).schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

        this.tab = tab;
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
        Observable.zip(
                githubService.getJSoupDataSource(GithubService.DATE_SOURCE_LEEEBO_TAB)
                        .map(jsoupDataSource -> {
                            leeeboTabDataSource = jsoupDataSource;
                            return jsoupDataSource;
                        }).flatMap(jsoupDataSource -> {
                    Observable<List<JSoupData>> leeeboTabLoadData = TextUtils.isEmpty(keyword) ? leeeboTabDataSource.loadData(leeeboTabDataSource.baseUrl + leeeboTabDataSource.pages[tab], true) : leeeboTabDataSource.searchData(keyword);
                    if (leeeboTabDataSource.getNextPage().equals(leeeboTabDataSource.baseUrl)) {
                        leeeboTabDataSource.setNextPage(null);
                        leeeboTabLoadData = Observable.create(subscriber -> {
                            subscriber.onNext(new ArrayList<>());
                            subscriber.onComplete();
                        });
                    }
                    return leeeboTabLoadData.onErrorReturn((throwable -> new ArrayList<>()));
                }),
                githubService.getJSoupDataSource(GithubService.DATE_SOURCE_K8DY_TAB)
                        .map(jsoupDataSource -> {
                            k8dyTabDataSource = jsoupDataSource;
                            return jsoupDataSource;
                        }).flatMap(jsoupDataSource -> {
                    Observable<List<JSoupData>> k8dyTabLoadData = TextUtils.isEmpty(keyword) ? k8dyTabDataSource.loadData(k8dyTabDataSource.baseUrl + k8dyTabDataSource.pages[tab], true) : k8dyTabDataSource.searchData(keyword);
                    if (k8dyTabDataSource.getNextPage().equals(k8dyTabDataSource.baseUrl)) {
                        k8dyTabDataSource.setNextPage(null);
                        k8dyTabLoadData = Observable.create(subscriber -> {
                            subscriber.onNext(new ArrayList<>());
                            subscriber.onComplete();
                        });
                    }
                    return k8dyTabLoadData.onErrorReturn((throwable -> new ArrayList<>()));
                }), (leeeboTabData, k8dyTabData) -> {
                    List<JSoupData> jsoupData = new ArrayList<>();
                    jsoupData.addAll(leeeboTabData);
                    jsoupData.addAll(k8dyTabData);
                    return jsoupData;
                });

        Observable<List<JSoupData>> leeeboTabLoadData = TextUtils.isEmpty(keyword) ? leeeboTabDataSource.loadData(leeeboTabDataSource.baseUrl + leeeboTabDataSource.pages[tab], true) : leeeboTabDataSource.searchData(keyword);


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

        Observable<List<JSoupData>> refresh = Observable.zip(leeeboTabLoadData.onErrorReturn((throwable -> new ArrayList<>())),
                k8dyTabLoadData.onErrorReturn((throwable -> new ArrayList<>())),
                (leeeboTabData, k8dyTabData) -> {
                    List<JSoupData> jsoupData = new ArrayList<>();
                    jsoupData.addAll(leeeboTabData);
                    jsoupData.addAll(k8dyTabData);
                    return jsoupData;
                });

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

        Observable<List<JSoupData>> leeeboTabLoadData = TextUtils.isEmpty(keyword) ? leeeboTabDataSource.loadData() : leeeboTabDataSource.searchData(keyword);
        Observable<List<JSoupData>> k8dyTabLoadData = TextUtils.isEmpty(keyword) ? k8dyTabDataSource.loadData() : k8dyTabDataSource.searchData(keyword);

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

        Observable<List<JSoupData>> loadMore = Observable.zip(leeeboTabLoadData.onErrorReturn((throwable -> new ArrayList<>())),
                k8dyTabLoadData.onErrorReturn((throwable -> new ArrayList<>())),
                (leeeboTabData, k8dyTabData) -> {
                    List<JSoupData> jsoupData = new ArrayList<>();
                    jsoupData.addAll(leeeboTabData);
                    jsoupData.addAll(k8dyTabData);
                    return jsoupData;
                });

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
        return leeeboTabDataSource.hasMore() || leeeboTabDataSource.hasMore();
    }
}
