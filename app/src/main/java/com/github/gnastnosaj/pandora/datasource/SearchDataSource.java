package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;
import android.text.TextUtils;

import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
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

public class SearchDataSource extends RxDataSource<List<JSoupData>> implements IDataCacheLoader<List<JSoupData>> {

    private String dataSource;
    private String keyword;

    private JSoupDataSource searchDataSource;

    private List<JSoupData> cache;

    private RealmConfiguration realmConfig;

    public SearchDataSource(Context context, String dataSource, String keyword) {
        super(context);

        this.dataSource = dataSource;
        this.keyword = keyword;

        realmConfig = new RealmConfiguration.Builder().name(dataSource + (TextUtils.isEmpty(keyword) ? "" : ("_" + keyword.hashCode()))).schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();
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
    public Observable<List<JSoupData>> refresh() throws Exception {
        if (TextUtils.isEmpty(keyword)) {
            throw new Exception("keyword is empty");
        }

        Observable<List<JSoupData>> refresh = Retrofit.newGithubServicePlus().getJSoupDataSource(dataSource).map(jsoupDataSource -> {
            searchDataSource = jsoupDataSource;
            return jsoupDataSource;
        }).flatMap(searchDataSource -> searchDataSource.searchData(keyword, null, true));

        refresh = refresh.map(jsoupData -> {
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
        Observable<List<JSoupData>> loadMore = searchDataSource.searchData(keyword);

        loadMore = loadMore.map(jsoupData -> {
            Realm realm = Realm.getInstance(realmConfig);
            realm.executeTransactionAsync(bgRealm -> bgRealm.insertOrUpdate(jsoupData));
            realm.close();
            return jsoupData;
        });

        return loadMore;
    }

    @Override
    public boolean hasMore() {
        return searchDataSource.hasMore();
    }
}
