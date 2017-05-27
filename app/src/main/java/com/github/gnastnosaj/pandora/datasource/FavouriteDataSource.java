package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.IDataSource;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by Jason on 7/17/2015.
 */
public class FavouriteDataSource implements IDataSource<List<JSoupData>> {

    private RealmConfiguration favouriteRealmConfig;
    private List<JSoupData> data;

    private int pages;
    private int currentPage = 1;
    private int pageSize = 10;

    public FavouriteDataSource(String datasource) {
        favouriteRealmConfig = new RealmConfiguration.Builder().name(datasource + "_FAVOURITE_CACHE").schemaVersion(BuildConfig.VERSION_CODE)
                .migration(Pandora.getRealmMigration()).build();
    }

    @Override
    public List<JSoupData> refresh() throws Exception {
        data = new ArrayList<>();

        Realm realm = Realm.getInstance(favouriteRealmConfig);
        RealmResults<JSoupData> results = realm.where(JSoupData.class).findAll();
        data.addAll(JSoupData.from(results));
        realm.close();

        pages = data.size() / pageSize + (data.size() % pageSize == 0 ? 0 : 1);
        currentPage = 1;

        return loadMore();
    }

    @Override
    public List<JSoupData> loadMore() throws Exception {
        List<JSoupData> data = new ArrayList<>();
        int i = (currentPage - 1) * pageSize;
        for (int j = 0; j < pageSize; j++) {
            if (i < this.data.size()) {
                data.add(this.data.get(i));
                i++;
            } else {
                break;
            }
        }
        currentPage++;
        return data;
    }

    @Override
    public boolean hasMore() {
        return currentPage < pages;
    }
}

