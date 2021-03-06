package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bilibili.socialize.share.core.shareparam.ShareParamText;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.datasource.service.SearchService;
import com.github.gnastnosaj.pandora.model.JSoupCatalog;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.github.gnastnosaj.pandora.ui.widget.RatioImageView;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.trinea.android.common.util.ListUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import me.next.tagview.TagCloudView;

/**
 * Created by Jason on 10/16/2016.
 */

public class PandoraDetailActivity extends BaseActivity {
    public final static String DATA_SOURCE = "PANDORA";

    public static final String EXTRA_DATA = "data";

    Menu menu;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.detail_thumbnail)
    RatioImageView thumbnail;

    @BindView(R.id.detail_desc_container)
    LinearLayout descContainer;

    @BindView(R.id.detail_resource_container)
    LinearLayout resourceContainer;

    @BindView(R.id.detail_intro_content)
    TextView intro;

    private JSoupData data;
    private String title;
    private String href;

    private String play;
    private List<JSoupCatalog> resource;
    private boolean favourite;

    private RealmConfiguration favouriteRealmConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pandora_detail);
        ButterKnife.bind(this);

        createDynamicBox(findViewById(R.id.nested_scroll_view));

        setSupportActionBar(toolbar);
        initSystemBar();

        data = getIntent().getParcelableExtra(EXTRA_DATA);
        data.group = null;
        href = data.getAttr("href");
        title = data.getAttr("title");

        setTitle(title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        favouriteRealmConfiguration = new RealmConfiguration.Builder().name(DATA_SOURCE + "_FAVOURITE_CACHE").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

        showDynamicBoxCustomView(DYNAMIC_BOX_AV_BALLGRIDPULSE, this);
        init(href);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            init(href);
            Observable.timer(500, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((l) ->
                            swipeRefreshLayout.setRefreshing(false));
        });
    }

    private void init(String href) {
        GithubService githubService = Retrofit.newGithubServicePlus();
        githubService.getJSoupDataSource(GithubService.DATE_SOURCE_K8DY_DETAIL)
                .switchMap(jsoupDataSource -> {
                    if (href.startsWith(jsoupDataSource.baseUrl)) {
                        return Observable.just(jsoupDataSource);
                    } else {
                        return githubService.getJSoupDataSource(GithubService.DATE_SOURCE_LEEEBO_DETAIL);
                    }
                })
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .subscribe(jsoupDataSource -> {
                    jsoupDataSource.loadData(href)
                            .retry(3)
                            .compose(bindUntilEvent(ActivityEvent.DESTROY))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(data -> {
                                if (!ListUtils.isEmpty(data)) {
                                    DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                                            .setUri(data.get(0).getAttr("thumbnail"))
                                            .setOldController(thumbnail.getController())
                                            .setControllerListener(new BaseControllerListener<ImageInfo>() {
                                                @Override
                                                public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable anim) {
                                                    thumbnail.setOriginalSize(imageInfo.getWidth(), imageInfo.getHeight());
                                                }

                                                @Override
                                                public void onFailure(String id, Throwable throwable) {

                                                }
                                            }).build();
                                    thumbnail.setController(draweeController);
                                    intro.setText(data.get(0).getAttr("intro"));
                                    play = data.get(0).getAttr("href");

                                    descContainer.removeAllViews();
                                    if (!ListUtils.isEmpty(data.get(0).tags)) {
                                        for (JSoupLink link : data.get(0).tags) {
                                            TextView textView = new TextView(this);
                                            textView.setText(link.title);
                                            textView.setTextSize(15);
                                            descContainer.addView(textView);
                                        }
                                    }
                                }
                                dismissDynamicBox(this);
                            }, throwable -> dismissDynamicBox(this));
                    jsoupDataSource.loadCatalogs(href)
                            .retry(3)
                            .compose(bindUntilEvent(ActivityEvent.DESTROY))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(data -> {
                                resource = data;
                                resourceContainer.removeAllViews();
                                for (int i = 0; i < data.size(); i++) {
                                    JSoupCatalog catalog = (JSoupCatalog) data.get(i);
                                    View resourceView = getLayoutInflater().inflate(R.layout.item_pandora_resource, null, false);
                                    ((TextView) resourceView.findViewById(R.id.detail_resource_title)).setText(TextUtils.isEmpty(catalog.link.title) ? ("#" + (i + 1)) : catalog.link.title);
                                    TagCloudView resourceCloudView = resourceView.findViewById(R.id.detail_resource_cloud);
                                    List<String> tagList = new ArrayList<>();
                                    for (JSoupLink link : catalog.tags) {
                                        tagList.add(link.title);
                                    }
                                    resourceCloudView.setTags(tagList);
                                    resourceCloudView.setOnTagClickListener((item) -> {
                                        String url = catalog.tags.get(item).url;
                                        startActivity(new Intent(this, PandoraWebVideoViewActivity.class)
                                                .putExtra(PandoraWebVideoViewActivity.EXTRA_HREF, url)
                                                .putExtra(PandoraWebVideoViewActivity.EXTRA_TITLE, title)
                                                .putParcelableArrayListExtra(PandoraWebVideoViewActivity.EXTRA_RESOURCE, (ArrayList<? extends Parcelable>) data)
                                                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
                                    });
                                    resourceContainer.addView(resourceView);
                                }
                            });
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pandora_detail, menu);
        this.menu = menu;

        menu.findItem(R.id.action_search).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_archive)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_share).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_play).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_youtube_play)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_favourite).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                .color(Color.WHITE).sizeDp(18));

        Observable.<Boolean>create(subscriber -> {
            Realm realm = Realm.getInstance(favouriteRealmConfiguration);
            RealmResults<JSoupData> results = realm.where(JSoupData.class).findAll();
            for (JSoupData result : results) {
                if (href.equals(result.getAttr("href"))) {
                    favourite = true;
                    break;
                }
            }
            subscriber.onNext(favourite);
            subscriber.onComplete();
        }).compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(favourite -> {
                    if (favourite) {
                        menu.findItem(R.id.action_favourite).setIcon(new IconicsDrawable(this)
                                .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                                .color(getResources().getColor(R.color.colorAccent)).sizeDp(18));
                    }
                });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_share:
                ShareHelper.share(this, new ShareParamText(title, href));
                return true;
            case R.id.action_search:
                SearchService.search(title, title, SearchService.TYPE_MAGNET, this, new SearchService.SearchListener() {
                    @Override
                    public void onStart() {
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onSuccess(List<JSoupData> data) {
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
                return true;
            case R.id.action_play:
                try {
                    String url = TextUtils.isEmpty(play) ? resource.get(0).tags.get(0).url : play;
                    if (!TextUtils.isEmpty(url)) {
                        startActivity(new Intent(this, PandoraWebVideoViewActivity.class)
                                .putExtra(PandoraWebVideoViewActivity.EXTRA_HREF, url)
                                .putExtra(PandoraWebVideoViewActivity.EXTRA_TITLE, title)
                                .putParcelableArrayListExtra(PandoraWebVideoViewActivity.EXTRA_RESOURCE, (ArrayList<? extends Parcelable>) resource)
                                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
                    } else {
                        Snackbar.make(swipeRefreshLayout, R.string.search_result_not_found, Snackbar.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Snackbar.make(swipeRefreshLayout, R.string.search_result_not_found, Snackbar.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_favourite:
                if (data != null) {
                    if (favourite) {
                        Realm realm = Realm.getInstance(favouriteRealmConfiguration);
                        realm.executeTransactionAsync(bgRealm -> {
                            RealmResults<JSoupData> results = bgRealm.where(JSoupData.class).findAll();
                            for (JSoupData result : results) {
                                if (href.equals(result.getAttr("href"))) {
                                    result.deleteFromRealm();
                                    break;
                                }
                            }
                        });
                        realm.close();
                        menu.findItem(R.id.action_favourite).setIcon(new IconicsDrawable(this)
                                .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                                .color(Color.WHITE).sizeDp(18));
                    } else {
                        Realm realm = Realm.getInstance(favouriteRealmConfiguration);
                        realm.executeTransactionAsync(bgRealm -> bgRealm.insertOrUpdate(data));
                        realm.close();
                        menu.findItem(R.id.action_favourite).setIcon(new IconicsDrawable(this)
                                .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                                .color(getResources().getColor(R.color.colorAccent)).sizeDp(18));
                    }
                    favourite = !favourite;
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
