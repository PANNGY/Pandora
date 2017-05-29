package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bilibili.socialize.share.core.shareparam.ShareParamText;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.BuildConfig;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.SimpleViewPagerAdapter;
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.Retrofit;
import com.github.gnastnosaj.pandora.datasource.service.SearchService;
import com.github.gnastnosaj.pandora.model.JSoupCatalog;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.mauker.materialsearchview.MaterialSearchView;
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
import timber.log.Timber;

/**
 * Created by jasontsang on 5/26/17.
 */

public class SimpleViewPagerActivity extends BaseActivity {
    public final static String EXTRA_TITLE = "title";
    public final static String EXTRA_TAB_DATASOURCE = "tab_datasource";
    public final static String EXTRA_GALLERY_DATASOURCE = "gallery_datasource";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    @BindView(R.id.tab)
    TabLayout tabLayout;

    @BindView(R.id.view_pager)
    ViewPager viewPager;

    @BindView(R.id.search_view)
    MaterialSearchView searchView;

    ListView suggestionsListView;

    private String title;
    private String tabDataSource;
    private String galleryDataSource;

    private static Map<String, List> catalogMap = new HashMap<>();
    private static Map<String, List<JSoupLink>> tabMap = new HashMap<>();

    private GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);
    private RealmConfiguration tabCacheRealmConfig;
    private RealmConfiguration catalogCacheRealmConfig;

    @Override
    public void onBackPressed() {
        if (searchView.isOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_view_pager_with_tab);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        title = getIntent().getStringExtra(EXTRA_TITLE);
        tabDataSource = getIntent().getStringExtra(EXTRA_TAB_DATASOURCE);
        galleryDataSource = getIntent().getStringExtra(EXTRA_GALLERY_DATASOURCE);

        setTitle(TextUtils.isEmpty(title) ? "" : title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initViewPager();
        initSearchView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_simple_view_pager, menu);
        menu.findItem(R.id.action_search).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_search)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_share).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_favourite).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                .color(Color.WHITE).sizeDp(18));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_search:
                searchView.openSearch();
                return true;
            case R.id.action_share:
                ShareHelper.share(this, new ShareParamText(getResources().getString(R.string.action_share), getResources().getString(R.string.share_pandora)));
                return true;
            case R.id.action_favourite:
                Intent i = new Intent(this, SimpleTabActivity.class);
                i.putExtra(SimpleTabActivity.EXTRA_TAB_DATASOURCE, tabDataSource);
                i.putExtra(SimpleTabActivity.EXTRA_GALLERY_DATASOURCE, galleryDataSource);
                i.putExtra(SimpleTabActivity.EXTRA_TYPE, SimpleTabActivity.TYPE_FAVOURITE);
                i.putExtra(SimpleTabActivity.EXTRA_TITLE, getResources().getString(R.string.action_favourite));
                startActivity(i);
                return true;
            case R.id.action_about:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViewPager() {
        initTabs().compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tabs -> {
                    SimpleViewPagerAdapter simplePagerAdapter = new SimpleViewPagerAdapter(this, getSupportFragmentManager(), tabs, tabDataSource, galleryDataSource);
                    viewPager.setAdapter(simplePagerAdapter);
                    tabLayout.setupWithViewPager(viewPager);
                });
    }

    private Observable<List<JSoupLink>> initTabs() {
        List<JSoupLink> tabs = tabMap.containsKey(tabDataSource) ? tabMap.get(tabDataSource) : new ArrayList<>();
        if(!tabMap.containsKey(tabDataSource)) {
            tabMap.put(tabDataSource, tabs);
        }
        if (ListUtils.isEmpty(tabs)) {
            tabCacheRealmConfig = new RealmConfiguration.Builder().name(tabDataSource + "_TAB_CACHE").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

            Realm realm = Realm.getInstance(tabCacheRealmConfig);
            RealmResults<JSoupLink> results = realm.where(JSoupLink.class).findAll();
            tabs.addAll(JSoupLink.from(results));
            realm.close();

            if (ListUtils.isEmpty(tabs)) {
                return initTabs(tabs);
            } else {
                initTabs(tabs).compose(bindUntilEvent(ActivityEvent.DESTROY)).subscribeOn(Schedulers.newThread()).subscribe();
                return Observable.just(tabs);
            }
        } else {
            return Observable.just(tabs);
        }
    }

    private Observable<List<JSoupLink>> initTabs(List<JSoupLink> tabs) {
        return githubService.getJSoupDataSource(tabDataSource)
                .flatMap(jsoupDataSource -> jsoupDataSource.loadTabs())
                .flatMap(data -> {
                    tabs.clear();
                    tabs.addAll(data);
                    Realm bgRealm = Realm.getInstance(tabCacheRealmConfig);
                    bgRealm.executeTransactionAsync(bg -> {
                        bg.delete(JSoupLink.class);
                        bg.insertOrUpdate(data);
                    });
                    bgRealm.close();
                    return Observable.just(data);
                });
    }

    private Observable<List> initCatalog() {
        List catalog = catalogMap.containsKey(tabDataSource) ? catalogMap.get(tabDataSource) : new ArrayList<>();
        if(!catalogMap.containsKey(tabDataSource)) {
            catalogMap.put(tabDataSource, catalog);
        }

        if (ListUtils.isEmpty(catalog)) {
            catalogCacheRealmConfig = new RealmConfiguration.Builder().name(tabDataSource + "_CATALOG_CACHE").schemaVersion(BuildConfig.VERSION_CODE).migration(Pandora.getRealmMigration()).build();

            Realm realm = Realm.getInstance(catalogCacheRealmConfig);
            RealmResults results = realm.where(JSoupLink.class).findAll();
            catalog.addAll(JSoupLink.from(results));
            if (ListUtils.isEmpty(catalog)) {
                results = realm.where(JSoupCatalog.class).findAll();
                catalog.addAll(JSoupLink.from(results));
            }
            realm.close();

            if (ListUtils.isEmpty(catalog)) {
                return initCatalog(catalog);
            } else {
                initCatalog(catalog).compose(bindUntilEvent(ActivityEvent.DESTROY)).subscribeOn(Schedulers.newThread()).subscribe();
                return Observable.just(catalog);
            }
        } else {
            return Observable.just(catalog);
        }
    }

    private Observable<List> initCatalog(List catalog) {
        return githubService.getJSoupDataSource(tabDataSource)
                .flatMap(jsoupDataSource -> jsoupDataSource.loadCatalogs())
                .flatMap(data -> {
                    catalog.clear();
                    catalog.addAll(data);
                    Realm bgRealm = Realm.getInstance(catalogCacheRealmConfig);
                    bgRealm.executeTransactionAsync(bg -> {
                        if (!ListUtils.isEmpty(data)) {
                            if (data.get(0) instanceof JSoupCatalog) {
                                bg.delete(JSoupCatalog.class);
                            } else {
                                bg.delete(JSoupLink.class);
                            }
                            bg.insertOrUpdate(data);
                        }
                    });
                    bgRealm.close();
                    return Observable.just(data);
                });
    }

    private void setCatalog(List _catalog) {
        if (!ListUtils.isEmpty(_catalog)) {
            if (_catalog.get(0) instanceof JSoupCatalog) {
                List<JSoupCatalog> catalog = (List<JSoupCatalog>) _catalog;
                for (JSoupCatalog jsoupCatalog : catalog) {
                    View tagGroupView = getLayoutInflater().inflate(R.layout.item_tag_cloud, null);
                    TextView tagGroupTitle = (TextView) tagGroupView.findViewById(R.id.tag_group_title);
                    tagGroupTitle.setText(jsoupCatalog.link.title);

                    TagCloudView tagCloudView = (TagCloudView) tagGroupView.findViewById(R.id.tag_cloud_view);

                    List<String> tags = new ArrayList<>();
                    for (JSoupLink link : jsoupCatalog.tags) {
                        tags.add(link.title);
                    }
                    tagCloudView.setTags(tags);
                    tagCloudView.setOnTagClickListener(position -> {
                        Intent intent = new Intent(this, SimpleTabActivity.class);
                        intent.putExtra(SimpleTabActivity.EXTRA_TAB_DATASOURCE, tabDataSource);
                        intent.putExtra(SimpleTabActivity.EXTRA_GALLERY_DATASOURCE, galleryDataSource);
                        intent.putExtra(SimpleTabActivity.EXTRA_TITLE, tags.get(position));
                        intent.putExtra(SimpleTabActivity.EXTRA_HREF, jsoupCatalog.tags.get(position).url);
                        startActivity(intent);
                    });
                    suggestionsListView.addFooterView(tagGroupView);
                }
            } else {
                List<JSoupLink> catalog = (List<JSoupLink>) _catalog;
                View tagGroupView = getLayoutInflater().inflate(R.layout.item_tag_cloud, null);
                TagCloudView tagCloudView = (TagCloudView) tagGroupView.findViewById(R.id.tag_cloud_view);

                List<String> tags = new ArrayList<>();
                for (JSoupLink link : catalog) {
                    tags.add(link.title);
                }
                tagCloudView.setTags(tags);
                tagCloudView.setOnTagClickListener(position -> {
                    Intent intent = new Intent(this, SimpleTabActivity.class);
                    intent.putExtra(SimpleTabActivity.EXTRA_TAB_DATASOURCE, tabDataSource);
                    intent.putExtra(SimpleTabActivity.EXTRA_GALLERY_DATASOURCE, galleryDataSource);
                    intent.putExtra(SimpleTabActivity.EXTRA_TITLE, tags.get(position));
                    intent.putExtra(SimpleTabActivity.EXTRA_HREF, catalog.get(position).url);
                    startActivity(intent);
                });
                suggestionsListView.addFooterView(tagGroupView);
            }
        }
    }

    private void initSearchView() {
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    search(query);
                } catch (Exception e) {
                    Timber.e(e, "searchView onQueryText exception");
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnItemClickListener((adapterView, view, i, l) -> {
            try {
                ListView suggestionsListView = (ListView) searchView.findViewById(R.id.suggestion_list);
                if (suggestionsListView.getHeaderViewsCount() > 0) {
                    if (i == 0) {
                        searchView.clearAll();
                    } else {
                        String keyword = searchView.getSuggestionAtPosition(i - 1);
                        if (!TextUtils.isEmpty(keyword)) {
                            search(keyword);
                        }
                    }
                } else {
                    String keyword = searchView.getSuggestionAtPosition(i);
                    if (!TextUtils.isEmpty(keyword)) {
                        search(keyword);
                    }
                }
            } catch (Exception e) {
                Timber.e(e, "searchView onItemClick exception");
            } finally {
                searchView.closeSearch();
            }
        });
        try {
            suggestionsListView = (ListView) searchView.findViewById(R.id.suggestion_list);
            if (suggestionsListView.getHeaderViewsCount() == 0) {
                View deleteIconView = getLayoutInflater().inflate(R.layout.view_search_delete, null);
                suggestionsListView.addHeaderView(deleteIconView);
            }
        } catch (Exception e) {
            Timber.e(e, "init searchView exception");
        }
        initCatalog().compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(catalog -> setCatalog(catalog));
    }

    private void search(String keyword) {
        SearchService.search(keyword, this, new SearchService.SearchListener() {
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
    }
}
