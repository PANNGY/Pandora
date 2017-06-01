package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;
import android.preference.PreferenceManager;

import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.shizhefei.mvc.IDataCacheLoader;
import com.shizhefei.mvc.IDataSource;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 12/24/16.
 */

public class MyPluginsDataSource implements IDataSource<List<Plugin>>, IDataCacheLoader<List<Plugin>> {
    private Context context;

    public MyPluginsDataSource(Context context) {
        this.context = context;
    }

    @Override
    public List<Plugin> loadCache(boolean isEmpty) {
        return null;
    }

    @Override
    public List<Plugin> refresh() throws Exception {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Plugin> results = realm.where(Plugin.class).findAll();
        List<Plugin> plugins = new ArrayList<>();
        boolean nsw = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Pandora.PRE_PRO_VERSION, false);
        for (Plugin plugin : results) {
            if (nsw || !plugin.desc.contains(Plugin.NSW)) {
                plugins.add(plugin);
            }
        }
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
