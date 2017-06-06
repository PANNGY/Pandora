package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.bilibili.socialize.share.core.shareparam.ShareParamText;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.request.ImageRequest;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.PandoraAdapter;
import com.github.gnastnosaj.pandora.datasource.service.PluginService;
import com.github.gnastnosaj.pandora.datasource.service.SearchService;
import com.github.gnastnosaj.pandora.datasource.service.SplashService;
import com.github.gnastnosaj.pandora.datasource.service.UpdateService;
import com.github.gnastnosaj.pandora.event.TabEvent;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.octicons_typeface_library.Octicons;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.io.File;
import java.util.List;

import br.com.mauker.materialsearchview.MaterialSearchView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Created by jasontsang on 4/23/17.
 */

public class PandoraActivity extends BaseActivity {
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

    Drawer drawer;

    private SharedPreferences sharedPreferences;
    private Observable<TabEvent> tabEventObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_view_pager_with_tab);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        tabEventObservable = RxBus.getInstance().register(TabEvent.TAG_PANDORA_TAB, TabEvent.class);

        initDrawer();
        initViewPager();
        initSearchView();
        UpdateService.checkForUpdate(this);
        prepareSplashImage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDrawer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.getInstance().unregister(TabEvent.TAG_PANDORA_TAB, tabEventObservable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pandora, menu);
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
            case R.id.action_search:
                searchView.openSearch();
                return true;
            case R.id.action_share:
                ShareHelper.share(this, new ShareParamText(getResources().getString(R.string.action_share), getResources().getString(R.string.share_pandora)));
                return true;
            case R.id.action_favourite:
                Intent i = new Intent(this, PandoraTabActivity.class);
                i.putExtra(PandoraTabActivity.EXTRA_TYPE, PandoraTabActivity.TYPE_FAVOURITE);
                i.putExtra(PandoraTabActivity.EXTRA_TITLE, getResources().getString(R.string.action_favourite));
                startActivity(i);
                return true;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (searchView.isOpen()) {
            searchView.closeSearch();
            return true;
        } else if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void initDrawer() {
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withDrawerLayout(new DrawerLayout(this) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                                MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY);

                        heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                                MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY);

                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    }
                })
                .withHeader(new View(this))
                .withTranslucentStatusBar(false)
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.drawer_item_plugin_center).withIdentifier(R.string.drawer_item_plugin_center).withIcon(Octicons.Icon.oct_package).withSelectable(false),
                        new SectionDrawerItem().withName(R.string.drawer_item_section_plugins).withIdentifier(R.string.drawer_item_section_plugins)
                )
                .addStickyDrawerItems(
                        new SecondaryDrawerItem().withName(R.string.drawer_item_help).withIdentifier(R.string.drawer_item_help).withIcon(MaterialDesignIconic.Icon.gmi_help).withSelectable(false),
                        new SecondaryDrawerItem().withName(R.string.drawer_item_open_source).withIdentifier(R.string.drawer_item_open_source).withIcon(MaterialDesignIconic.Icon.gmi_github).withSelectable(false),
                        //new SecondaryDrawerItem().withName(R.string.drawer_item_terminal).withIdentifier(R.string.drawer_item_terminal).withIcon(Octicons.Icon.oct_terminal).withSelectable(false),
                        //new SecondaryDrawerItem().withName(R.string.drawer_item_settings).withIdentifier(R.string.drawer_item_settings).withIcon(MaterialDesignIconic.Icon.gmi_settings).withSelectable(false),
                        new SwitchDrawerItem().withName(R.string.drawer_item_nsw).withIdentifier(R.string.drawer_item_nsw).withIcon(Octicons.Icon.oct_eye).withSelectable(false).withChecked(sharedPreferences.getBoolean(Pandora.PRE_PRO_VERSION, false)).withOnCheckedChangeListener((drawerItem, buttonView, isChecked) -> {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(Pandora.PRE_PRO_VERSION, isChecked);
                            editor.apply();
                            updateDrawer();
                        })
                )
                .withOnDrawerNavigationListener((clickedView) -> {
                    finish();
                    return true;
                })
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    if (drawerItem != null) {
                        Intent i = null;
                        switch ((int) drawerItem.getIdentifier()) {
                            case R.string.drawer_item_plugin_center:
                                i = new Intent(this, PluginCenterActivity.class);
                                break;
                            case R.string.drawer_item_help:
                                i = new Intent(this, WebViewActivity.class);
                                i.putExtra(WebViewActivity.EXTRA_TITLE, getResources().getString(R.string.drawer_item_help));
                                i.putExtra(WebViewActivity.EXTRA_HREF, getResources().getString(R.string.url_help_and_feedback));
                                break;
                            case R.string.drawer_item_open_source:
                                i = new Intent(this, OpenSourceActivity.class);
                                break;
                        }
                        if (i != null) {
                            startActivity(i);
                        }
                    }
                    return false;
                })
                .build();
    }

    private void updateDrawer() {
        Realm.getDefaultInstance().executeTransactionAsync(bgRealm -> {
            RealmResults<Plugin> results = bgRealm.where(Plugin.class).findAll();
            Observable.just(Plugin.from(results))
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(plugins -> {
                        while (drawer.getPosition(R.string.drawer_item_section_plugins) != drawer.getDrawerItems().size()) {
                            drawer.removeItemByPosition(drawer.getPosition(R.string.drawer_item_section_plugins) + 1);
                        }
                        for (Plugin plugin : plugins) {
                            boolean nsw = sharedPreferences.getBoolean(Pandora.PRE_PRO_VERSION, false);
                            if (nsw || !plugin.desc.contains(Plugin.NSW)) {
                                File icon = plugin.getIcon(this);
                                drawer.addItemAtPosition(new SecondaryDrawerItem().withIdentifier(plugin.id.hashCode()).withName(plugin.name).withIcon(icon.exists() ? new ImageHolder(Uri.fromFile(icon)) : new ImageHolder(MaterialDesignIconic.Icon.gmi_widgets)).withSelectable(false).withOnDrawerItemClickListener((view, position, drawerItem) -> {
                                    PluginService.start(this, plugin);
                                    return false;
                                }), drawer.getPosition(R.string.drawer_item_section_plugins) + 1);
                            }
                        }
                    });
        });
    }

    private void initViewPager() {
        PandoraAdapter pandoraAdapter = new PandoraAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(pandoraAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabEventObservable
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(tabEvent -> viewPager.setCurrentItem(tabEvent.tab));
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
            ListView suggestionsListView = (ListView) searchView.findViewById(R.id.suggestion_list);
            if (suggestionsListView.getHeaderViewsCount() == 0) {
                View deleteIconView = getLayoutInflater().inflate(R.layout.view_search_delete, null);
                suggestionsListView.addHeaderView(deleteIconView);
            }
        } catch (Exception e) {
            Timber.e(e, "initSearchView exception");
        }
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

    private void prepareSplashImage() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int splashImageDataSource = sharedPreferences.getInt(SplashService.PRE_SPLASH_IMAGE_DATA_SOURCE, SplashService.SPLASH_IMAGE_DATA_SOURCE_GIRL_ATLAS);

        Single<String> splashImageSingle = null;

        switch (splashImageDataSource) {
            case SplashService.SPLASH_IMAGE_DATA_SOURCE_GANK:
                splashImageSingle = SplashService.gankSingle();
                break;
            case SplashService.SPLASH_IMAGE_DATA_SOURCE_GIRL_ATLAS:
                splashImageSingle = SplashService.girlAtlasSingle();
                break;
            case SplashService.SPLASH_IMAGE_DATA_SOURCE_NANRENCD:
                splashImageSingle = SplashService.nanrencdSingle();
                break;
            case SplashService.SPLASH_IMAGE_DATA_SOURCE_JAVLIB:
                splashImageSingle = SplashService.javlibSingle();
                break;
        }

        splashImageSingle
                .retry(3)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.newThread())
                .subscribe(uriString -> {
                    Timber.d("next time splash image: %s", uriString);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(SplashService.PRE_SPLASH_IMAGE, uriString);
                    editor.apply();
                    Fresco.getImagePipeline().prefetchToDiskCache(ImageRequest.fromUri(uriString), this);
                });
    }
}
