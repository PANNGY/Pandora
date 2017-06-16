package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;

import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.model.JSoupAttr;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.IDataCacheLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 5/23/17.
 */

public class PandoraHomeDataSource extends RxDataSource<List<PandoraHomeDataSource.Model>> implements IDataCacheLoader<List<PandoraHomeDataSource.Model>> {
    private RealmConfiguration realmConfig = new RealmConfiguration.Builder().name("PANDORA_HOME").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

    private GithubService githubService = Retrofit.newGithubServicePlus();

    private String[] groups;

    private List<JSoupDataSource> dataSources;

    public PandoraHomeDataSource(Context context) {
        super(context);

        groups = context.getResources().getStringArray(R.array.pandora_home_groups);

        dataSources = new ArrayList<>();
    }

    @Override
    public List<Model> loadCache(boolean isEmpty) {
        Realm realm = Realm.getInstance(realmConfig);
        RealmResults<JSoupData> results = realm.where(JSoupData.class).findAll();
        List<JSoupData> data = JSoupData.from(results);
        realm.close();
        return fromJSoupData(data);
    }

    @Override
    public Observable<List<Model>> refresh() throws Exception {
        Observable<List<Model>> refresh = Observable.zip(
                githubService.getJSoupDataSource(GithubService.DATE_SOURCE_LEEEBO_SLIDE)
                        .map(jsoupDataSource -> {
                            dataSources.add(jsoupDataSource);
                            return jsoupDataSource;
                        }).flatMap(jsoupDataSource -> jsoupDataSource.loadData(true).onErrorReturn((throwable -> new ArrayList<>()))),
                githubService.getJSoupDataSource(GithubService.DATE_SOURCE_LEEEBO_HOME)
                        .map(jsoupDataSource -> {
                            dataSources.add(jsoupDataSource);
                            return jsoupDataSource;
                        }).flatMap(jsoupDataSource -> jsoupDataSource.loadData(true).onErrorReturn((throwable -> new ArrayList<>()))),
                githubService.getJSoupDataSource(GithubService.DATE_SOURCE_K8DY_HOME)
                        .map(jsoupDataSource -> {
                            dataSources.add(jsoupDataSource);
                            return jsoupDataSource;
                        }).flatMap(jsoupDataSource -> jsoupDataSource.loadData(true).onErrorReturn((throwable -> new ArrayList<>())))
                , (data1, data2, data3) -> {
                    List<JSoupData> jsoupData = new ArrayList<>();
                    jsoupData.addAll(data1);
                    jsoupData.addAll(data2);
                    jsoupData.addAll(data3);
                    Realm realm = Realm.getInstance(realmConfig);
                    realm.executeTransactionAsync(bgRealm -> {
                        bgRealm.delete(JSoupData.class);
                        bgRealm.insertOrUpdate(jsoupData);
                    });
                    realm.close();
                    return fromJSoupData(jsoupData);
                });
        return refresh;
    }

    @Override
    public Observable<List<Model>> loadMore() throws Exception {
        return null;
    }

    @Override
    public boolean hasMore() {
        return false;
    }

    public String[] getGroups() {
        return groups;
    }

    private List<Model> fromJSoupData(JSoupData[] jsoupData) {
        return fromJSoupData(Arrays.asList(jsoupData));
    }

    private List<Model> fromJSoupData(List<JSoupData> jsoupData) {
        List<Model> models = new ArrayList<>();

        Model slideModel = new Model();
        slideModel.type = Model.TYPE_VALUE_SLIDE;
        List<JSoupData> slideData = new ArrayList<>();
        for (JSoupData data : jsoupData) {
            for (JSoupAttr attr : data.attrs) {
                if (attr.label.equals("type") && attr.content.equals(Model.TYPE_SLIDE)) {
                    slideData.add(data);
                    break;
                }
            }
        }
        slideModel.data = slideData;
        if (!slideData.isEmpty()) {
            models.add(slideModel);
        }

        for (String group : groups) {
            Model model = new Model();
            model.type = Model.TYPE_VALUE_GROUP;
            model.data = group;
            models.add(model);
        }

        for (JSoupData data : jsoupData) {
            if (data.getAttr("type").equals(Model.TYPE_DATA)) {
                int position = -1;
                switch (data.group.getAttr("title")) {
                    case "热门":
                    case "热门推荐":
                    case "热门影片":
                    case "热门电影":
                    case "推荐影片":
                        position = getPosition(models, groups[0]);
                        break;
                    case "电影":
                    case "影片":
                        position = getPosition(models, groups[1]);
                        break;
                    case "电视剧":
                    case "连续剧":
                        position = getPosition(models, groups[2]);
                        break;
                    case "综艺":
                    case "综艺节目":
                    case "真人秀":
                        position = getPosition(models, groups[3]);
                        break;
                    case "动漫":
                    case "动画":
                    case "动画片":
                        position = getPosition(models, groups[4]);
                        break;
                    case "微电影":
                    case "微影片":
                        position = getPosition(models, groups[5]);
                        break;
                    case "福利":
                    case "伦理":
                    case "伦理片":
                        position = getPosition(models, groups[6]);
                        break;
                }
                if (position != -1) {
                    Model model = new Model();
                    model.type = Model.TYPE_VALUE_DATA;
                    model.data = data;
                    if (position < models.size()) {
                        models.add(position, model);
                    } else {
                        models.add(model);
                    }
                }
            }
        }

        return models;
    }

    private int getPosition(List<Model> models, String group) {
        int position = -1;
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i).type == Model.TYPE_VALUE_GROUP && models.get(i).data.equals(group)) {
                position = i;
                break;
            }
        }
        while (position + 1 < models.size()) {
            if (models.get(position + 1).type == Model.TYPE_VALUE_GROUP) {
                break;
            } else {
                position++;
            }
        }
        return position + 1;
    }

    public static class Model {
        public final static String TYPE_SLIDE = "slide";
        public final static String TYPE_GROUP = "group";
        public final static String TYPE_DATA = "data";

        public final static int TYPE_VALUE_SLIDE = 0;
        public final static int TYPE_VALUE_GROUP = 1;
        public final static int TYPE_VALUE_DATA = 2;

        public int type;
        public Object data;
    }
}
