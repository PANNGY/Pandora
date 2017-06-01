package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.model.Plugin;
import com.shizhefei.mvc.IDataCacheLoader;
import com.shizhefei.mvc.IDataSource;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 12/24/16.
 */

public class MyPluginsDataSource implements IDataSource<List<Plugin>>, IDataCacheLoader<List<Plugin>> {
    @Override
    public List<Plugin> loadCache(boolean isEmpty) {
        return null;
    }

    @Override
    public List<Plugin> refresh() throws Exception {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Plugin> results = realm.where(Plugin.class).findAll();
        List<Plugin> plugins = Plugin.from(results);
        realm.close();
        return plugins;
    }

    @Override
    public List<Plugin> loadMore() throws Exception {
        return null;
    }

    @Override
    public boolean hasMore() {
        return false;
    }
}
