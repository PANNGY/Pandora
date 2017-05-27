package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import com.bilibili.socialize.share.core.shareparam.ShareImage;
import com.bilibili.socialize.share.core.shareparam.ShareParamImage;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.GalleryAdapter;
import com.github.gnastnosaj.pandora.datasource.SimpleDataSource;
import com.github.gnastnosaj.pandora.datasource.service.SearchService;
import com.github.gnastnosaj.pandora.event.ArchiveEvent;
import com.github.gnastnosaj.pandora.event.TagEvent;
import com.github.gnastnosaj.pandora.event.ToolbarEvent;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.github.gnastnosaj.pandora.ui.widget.ViewPagerViewHandler;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.shizhefei.mvc.ILoadViewFactory;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCNormalHelper;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.List;

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
 * Created by jasontsang on 5/26/17.
 */

public class GalleryActivity extends BaseActivity {
    public final static String EXTRA_TAB_DATASOURCE = "tab_datasource";
    public final static String EXTRA_GALLERY_DATASOURCE = "gallery_datasource";
    public final static String EXTRA_DATA = "data";
    public static final String EXTRA_CACHE = "cache";

    @BindView(R.id.app_bar)
    AppBarLayout appBar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.view_pager)
    ViewPager viewPager;

    @BindView(R.id.tag_cloud_view)
    TagCloudView tagCloudView;

    Menu menu;

    private String tabDataSource;
    private String galleryDataSource;
    private List<JSoupData> cache;
    private JSoupData data;

    private String id;
    private String title;
    private String href;
    private String keyword;

    private boolean isAppBarHidden;
    private Observable<TagEvent> tagEventObservable;
    private GalleryAdapter galleryAdapter;

    private RealmConfiguration favouriteRealmConfiguration;
    private boolean favourite;
    private RealmConfiguration archiveRealmConfiguration;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.getInstance().unregister(href, tagEventObservable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_view_pager_with_tag_cloud);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        tabDataSource = getIntent().getStringExtra(EXTRA_TAB_DATASOURCE);
        galleryDataSource = getIntent().getStringExtra(EXTRA_GALLERY_DATASOURCE);
        cache = getIntent().getParcelableArrayListExtra(EXTRA_CACHE);
        data = getIntent().getParcelableExtra(EXTRA_DATA);

        id = data.getAttr("id");
        title = data.getAttr("title");
        href = data.getAttr("href");

        keyword = SearchService.betterKeyword(TextUtils.isEmpty(id) ? title : id);

        setTitle(TextUtils.isEmpty(title) ? "" : title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tagEventObservable = RxBus.getInstance().register(href, TagEvent.class);
        tagEventObservable.compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tagEvent -> {
                    List<String> tags = new ArrayList<>();
                    for (JSoupLink link : tagEvent.tags) {
                        tags.add(link.title);
                    }
                    tagCloudView.setTags(tags);
                    tagCloudView.setOnTagClickListener(position -> {
                        Intent intent = new Intent(this, SimpleTabActivity.class);
                        intent.putExtra(SimpleTabActivity.EXTRA_TAB_DATASOURCE, tabDataSource);
                        intent.putExtra(SimpleTabActivity.EXTRA_GALLERY_DATASOURCE, galleryDataSource);
                        intent.putExtra(SimpleTabActivity.EXTRA_TITLE, tags.get(position));
                        intent.putExtra(SimpleTabActivity.EXTRA_HREF, tagEvent.tags.get(position).url);
                        startActivity(intent);
                    });
                });

        favouriteRealmConfiguration = new RealmConfiguration.Builder().name(tabDataSource + "_FAVOURITE_CACHE").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();
        archiveRealmConfiguration = new RealmConfiguration.Builder().name(tabDataSource + "_ARCHIVE_CACHE").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

        initViewPager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ToolbarEvent.observable.compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(toolbarEvent -> hideOrShowToolbar());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gallery, menu);

        this.menu = menu;

        menu.findItem(R.id.action_search).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_archive)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_share).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_play).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_cloud)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_favourite).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_mosaic).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_blur_linear)
                .color(Color.WHITE).sizeDp(18));

        ArchiveEvent.observable.subscribeOn(Schedulers.newThread()).map(archiveEvent -> {
            if (archiveEvent.keyword.equals(keyword)) {
                Realm realm = Realm.getInstance(archiveRealmConfiguration);
                realm.executeTransactionAsync(bgRealm -> bgRealm.insertOrUpdate(archiveEvent));
                realm.close();
                return true;
            } else {
                return false;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(archive -> {
                    if (archive) {
                        menu.findItem(R.id.action_play).setVisible(true);
                    }
                });

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
                String thumbnail = galleryAdapter.getData().get(viewPager.getCurrentItem()).getAttr("thumbnail");
                ShareParamImage shareParamImage = new ShareParamImage(title, thumbnail, href);
                shareParamImage.setImage(new ShareImage(thumbnail));
                ShareHelper.share(this, shareParamImage);
                return true;
            case R.id.action_search:
                SearchService.search(title, keyword, SearchService.TYPE_MAGNET, this, new SearchService.SearchListener() {
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
                Observable.<ArchiveEvent>create(subcriber -> {
                    Realm realm = Realm.getInstance(archiveRealmConfiguration);
                    RealmResults<ArchiveEvent> results = realm.where(ArchiveEvent.class).equalTo("keyword", keyword).findAll();
                    if (ListUtils.isEmpty(results)) {
                        subcriber.onNext(null);
                    } else {
                        subcriber.onNext(results.get(0).clone());
                    }
                    realm.close();
                    subcriber.onComplete();
                }).compose(bindUntilEvent(ActivityEvent.DESTROY)).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(archiveEvent -> {
                            if (archiveEvent != null) {
                                Uri uri = Uri.parse(archiveEvent.magnet);
                                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                                i.putExtra(WebVideoViewActivity.EXTRA_KEYWORD, keyword);
                                i.putExtra(WebVideoViewActivity.EXTRA_TITLE, title);
                                startActivity(i);
                            }
                        });
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
            case R.id.action_mosaic:
                Intent i = new Intent(this, MosaicActivity.class);
                i.putExtra(MosaicActivity.EXTRA_TITLE, title);
                i.putExtra(MosaicActivity.EXTRA_URL, galleryAdapter.getData().get(viewPager.getCurrentItem()).getAttr("thumbnail"));
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViewPager() {
        SimpleDataSource galleryDataSource = new SimpleDataSource(this, this.galleryDataSource, href);
        galleryDataSource.setCache(cache);
        galleryAdapter = new GalleryAdapter(this);

        MVCHelper mvcHelper = new MVCNormalHelper(viewPager, MVCHelper.loadViewFactory.madeLoadView(), new ILoadViewFactory.ILoadMoreView() {
            @Override
            public void init(ILoadViewFactory.FootViewAdder footViewHolder, View.OnClickListener onClickLoadMoreListener) {
            }

            @Override
            public void showNormal() {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void showNomore() {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void showLoading() {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void showFail(Exception e) {
                progressBar.setVisibility(View.GONE);
            }
        });

        mvcHelper.setDataSource(galleryDataSource);
        mvcHelper.setAdapter(galleryAdapter, new ViewPagerViewHandler());
        mvcHelper.refresh();
    }

    public void hideOrShowToolbar() {
        appBar.animate()
                .translationY(isAppBarHidden ? 0 : -appBar.getHeight())
                .setInterpolator(new DecelerateInterpolator(2))
                .start();
        tagCloudView.animate()
                .translationY(isAppBarHidden ? 0 : tagCloudView.getHeight())
                .setInterpolator(new DecelerateInterpolator(2))
                .start();
        isAppBarHidden = !isAppBarHidden;
    }
}
