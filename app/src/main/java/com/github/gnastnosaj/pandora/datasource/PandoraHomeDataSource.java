package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.IDataCacheLoader;
import com.shizhefei.mvc.IDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 5/23/17.
 */

public class PandoraHomeDataSource implements IDataSource<List<PandoraHomeDataSource.Model>>, IDataCacheLoader<List<PandoraHomeDataSource.Model>> {
    private RealmConfiguration realmConfig = new RealmConfiguration.Builder().name("PANDORA_HOME").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

    private GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);

    private JSoupDataSource leeeboHomeDataSource;
    private JSoupDataSource k8dyHomeDataSource;

    private CountDownLatch initLock;
    private CountDownLatch refreshLock;

    public PandoraHomeDataSource() {
        initLock = new CountDownLatch(1);

        githubService.getJSoupDataSource(GithubService.DATE_SOURCE_LEEEBO_HOME)
                .zipWith(githubService.getJSoupDataSource(GithubService.DATE_SOURCE_K8DY_HOME), (jsoupDataSource1, jsoupDataSource2) -> {
                    leeeboHomeDataSource = jsoupDataSource1;
                    k8dyHomeDataSource = jsoupDataSource2;
                    return true;
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(success -> initLock.countDown());
    }

    @Override
    public List<Model> loadCache(boolean isEmpty) {
        Realm realm = Realm.getInstance(realmConfig);
        RealmResults<JSoupData> results = realm.where(JSoupData.class).findAll();
        List<JSoupData> data = results.subList(0, results.size() - 1);
        realm.close();
        return fromJSoupData(data);
    }

    @Override
    public List<Model> refresh() throws Exception {
        if (refreshLock != null) {
            refreshLock.await();
        }
        refreshLock = new CountDownLatch(1);

        List<JSoupData> data = new ArrayList<>();

        initLock.await();

        leeeboHomeDataSource.loadData().zipWith(k8dyHomeDataSource.loadData(), (jsoupData1, jsoupData2) -> {
            data.addAll(jsoupData1);
            data.addAll(jsoupData2);
            return true;
        }).subscribeOn(Schedulers.newThread())
                .subscribe(success -> {
                    Realm realm = Realm.getInstance(realmConfig);
                    realm.executeTransactionAsync(bgRealm -> {
                        bgRealm.delete(JSoupData.class);
                        bgRealm.insertOrUpdate(data);
                    });
                    realm.close();
                    refreshLock.countDown();
                }, throwable -> refreshLock.countDown());

        return fromJSoupData(data);
    }

    @Override
    public List<Model> loadMore() throws Exception {
        return null;
    }

    @Override
    public boolean hasMore() {
        return false;
    }

    public static class Model {
        public final static int TYPE_SLIDE = 0;
        public final static int TYPE_GROUP = 1;
        public final static int TYPE_DATA = 2;

        public int type;
        public Object data;
    }

    private List<Model> fromJSoupData(List<JSoupData> jsoupData) {
        List<Model> models = new ArrayList<>();
        return models;
    }
}
