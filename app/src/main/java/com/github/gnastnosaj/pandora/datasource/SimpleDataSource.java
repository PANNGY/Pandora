package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;
import android.text.TextUtils;

import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.event.TagEvent;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.IDataCacheLoader;

import java.util.ArrayList;
import java.util.List;

import cn.trinea.android.common.util.ListUtils;
import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 5/25/17.
 */

public class SimpleDataSource extends RxDataSource<List<JSoupData>> implements IDataCacheLoader<List<JSoupData>> {

    private String dataSource;
    private String href;

    private JSoupDataSource simpleDataSource;

    private List<JSoupData> cache;

    private RealmConfiguration realmConfig;

    public SimpleDataSource(Context context, String dataSource, String href) {
        super(context);

        this.dataSource = dataSource;
        this.href = href;

        realmConfig = new RealmConfiguration.Builder().name(dataSource + (TextUtils.isEmpty(href) ? "" : ("_" + href.hashCode()))).schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();
    }

    public void setCache(List<JSoupData> cache) {
        if (!ListUtils.isEmpty(cache)) {
            this.cache = new ArrayList<>(cache);
        }
    }

    @Override
    public List<JSoupData> loadCache(boolean isEmpty) {
        if (!ListUtils.isEmpty(cache)) {
            RxBus.getInstance().post(href, new TagEvent(cache.get(0).tags));
            return cache;
        } else {
            Realm realm = Realm.getInstance(realmConfig);
            RealmResults<JSoupData> results = realm.where(JSoupData.class).findAll();
            List<JSoupData> data = JSoupData.from(results);
            if (!ListUtils.isEmpty(data) && !TextUtils.isEmpty(href)) {
                RxBus.getInstance().post(href, new TagEvent(data.get(0).tags));
            }
            realm.close();
            return data;
        }
    }

    @Override
    public Observable<List<JSoupData>> refresh() throws Exception {
        Observable refresh = Retrofit.newGithubServicePlus().getJSoupDataSource(dataSource)
                .map(jsoupDataSource -> {
                    simpleDataSource = jsoupDataSource;
                    return jsoupDataSource;
                })
                .flatMap(jsoupDataSource -> jsoupDataSource.loadData(href, true))
                .map(jsoupData -> {
                    if (!ListUtils.isEmpty(jsoupData) && !TextUtils.isEmpty(href)) {
                        RxBus.getInstance().post(href, new TagEvent(jsoupData.get(0).tags));
                    }
                    Realm realm = Realm.getInstance(realmConfig);
                    realm.executeTransactionAsync(bgRealm -> {
                        bgRealm.delete(JSoupData.class);
                        bgRealm.insertOrUpdate(jsoupData);
                    });
                    realm.close();
                    return jsoupData;
                });
        return refresh;
    }

    @Override
    public Observable<List<JSoupData>> loadMore() throws Exception {
        Observable loadMore = simpleDataSource.loadData()
                .map(jsoupData -> {
                    Realm realm = Realm.getInstance(realmConfig);
                    realm.executeTransactionAsync(bgRealm -> bgRealm.insertOrUpdate(jsoupData));
                    realm.close();
                    return jsoupData;
                });
        return loadMore;
    }

    @Override
    public boolean hasMore() {
        return simpleDataSource.hasMore();
    }
}
