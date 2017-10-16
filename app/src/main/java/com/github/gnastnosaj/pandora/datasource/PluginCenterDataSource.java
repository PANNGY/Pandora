package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;
import android.preference.PreferenceManager;

import com.alipay.euler.andfix.util.FileUtil;
import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.event.PluginEvent;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.shizhefei.mvc.IDataCacheLoader;
import com.trello.rxlifecycle2.android.ActivityEvent;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import timber.log.Timber;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadStatus;

/**
 * Created by jasontsang on 12/24/16.
 */

public class PluginCenterDataSource extends RxDataSource<List<Plugin>> implements IDataCacheLoader<List<Plugin>> {

    private Context context;

    private RealmConfiguration realmConfiguration;

    public PluginCenterDataSource(Context context) {
        super(context);

        this.context = context;

        realmConfiguration = new RealmConfiguration.Builder().name("PLUGIN_CENTER").schemaVersion(BuildConfig.VERSION_CODE)
                .migration(Pandora.getRealmMigration()).build();
    }

    @Override
    public List<Plugin> loadCache(boolean isEmpty) {
        return null;
    }

    @Override
    public Observable<List<Plugin>> refresh() throws Exception {
        Observable refresh = Retrofit.newGithubServicePlus().getPluginData()
                .flatMap(pluginData -> {
                    List<Plugin> plugins = new ArrayList<>();
                    boolean nsw = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Pandora.PRE_PRO_VERSION, false);

                    Realm pluginCenterRealm = Realm.getInstance(realmConfiguration);
                    Realm defaultRealm = Realm.getDefaultInstance();

                    pluginCenterRealm.beginTransaction();
                    defaultRealm.beginTransaction();

                    for (Plugin plugin : pluginData.plugins) {
                        if (nsw || !plugin.desc.contains(Plugin.NSW)) {
                            plugins.add(plugin);
                        }
                        Plugin result = pluginCenterRealm.where(Plugin.class).equalTo("id", plugin.id).findFirst();
                        if (result == null || plugin.versionCode > result.versionCode || pluginData.force || (plugin.type == Plugin.TYPE_PYTHON_VIDEO && !plugin.getPluginDirectory(context).exists())) {
                            pluginCenterRealm.insertOrUpdate(plugin);
                            RealmResults<Plugin> results = defaultRealm.where(Plugin.class).equalTo("id", plugin.id).findAll();
                            if (!results.isEmpty()) {
                                defaultRealm.insertOrUpdate(plugin);
                            }
                            if (plugin.type == Plugin.TYPE_PYTHON_VIDEO) {
                                String url = context.getResources().getString(R.string.url_python_plugin, plugin.reference);
                                Observable<DownloadStatus> download = RxDownload.getInstance(Boilerplate.getInstance())
                                        .download(url);

                                if (context instanceof BaseActivity) {
                                    download = download.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
                                }

                                download.subscribe(downloadStatus -> {
                                        },
                                        throwable -> Timber.w(throwable, "plugin download exception"),
                                        () -> {
                                            File[] files = RxDownload.getInstance(Boilerplate.getInstance()).getRealFiles(url);
                                            if (files != null) {
                                                File dir = plugin.getPluginDirectory(context);
                                                if (dir.exists()) {
                                                    FileUtil.deleteFile(dir);
                                                }
                                                File file = files[0];
                                                ZipUtil.unpack(file, dir);
                                                file.delete();
                                                RxBus.getInstance().post(PluginEvent.class, new PluginEvent(PluginEvent.TYPE_UPDATE, plugin));
                                            }
                                        });
                            } else {
                                RxBus.getInstance().post(PluginEvent.class, new PluginEvent(PluginEvent.TYPE_UPDATE, plugin));
                            }
                        }
                    }

                    RealmResults<Plugin> origin = pluginCenterRealm.where(Plugin.class).findAll();
                    for (Plugin plugin : origin) {
                        if (!pluginData.plugins.contains(plugin)) {
                            RealmResults<Plugin> results = defaultRealm.where(Plugin.class).equalTo("id", plugin.id).findAll();
                            if (!results.isEmpty()) {
                                results.deleteAllFromRealm();
                            }
                            plugin.deleteFromRealm();
                        }
                    }

                    defaultRealm.commitTransaction();
                    pluginCenterRealm.commitTransaction();

                    defaultRealm.close();
                    pluginCenterRealm.close();
                    return Observable.just(plugins);
                });
        return refresh;
    }

    @Override
    public Observable<List<Plugin>> loadMore() throws Exception {
        return null;
    }

    @Override
    public boolean hasMore() {
        return false;
    }
}
