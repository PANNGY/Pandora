package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.PluginCenterAdapter;
import com.github.gnastnosaj.pandora.datasource.MyPluginsDataSource;
import com.github.gnastnosaj.pandora.datasource.PluginCenterDataSource;
import com.github.gnastnosaj.pandora.event.PluginEvent;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.shizhefei.mvc.ILoadViewFactory;
import com.shizhefei.mvc.MVCNormalHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Created by jasontsang on 12/16/16.
 */

public class PluginCenterActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.my_plugins)
    RecyclerView myPlugins;

    @BindView(R.id.plugin_center)
    RecyclerView pluginCenter;

    Menu menu;

    PluginCenterAdapter myPluginsDataAdapter;
    PluginCenterAdapter pluginCenterDataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_center);
        ButterKnife.bind(this);

        createDynamicBox(findViewById(R.id.scroll_view));

        setSupportActionBar(toolbar);
        initSystemBar();

        setTitle(R.string.drawer_item_plugin_center);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        GridLayoutManager myPluginsLayoutManager = new GridLayoutManager(this, getResources().getInteger(R.integer.pluginCenterSpanCount));
        myPlugins.setLayoutManager(myPluginsLayoutManager);

        myPluginsDataAdapter = new PluginCenterAdapter(this, PluginCenterAdapter.TYPE_MY_PLUGINS);

        GestureDetector myPluginsGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent event) {
                View childView = myPlugins.findChildViewUnder(event.getX(), event.getY());
                int childPosition = myPlugins.getChildAdapterPosition(childView);
                if (-1 < childPosition && childPosition < myPluginsDataAdapter.getData().size()) {
                    RxBus.getInstance().post(PluginEvent.class, new PluginEvent(PluginEvent.TYPE_MANAGE, myPluginsDataAdapter.getData().get(childPosition)));
                }
            }
        });

        myPlugins.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (myPluginsGestureDetector.onTouchEvent(e)) {
                    View childView = rv.findChildViewUnder(e.getX(), e.getY());
                    int childPosition = rv.getChildAdapterPosition(childView);
                    if (-1 < childPosition && childPosition < myPluginsDataAdapter.getData().size()) {
                        Plugin plugin = myPluginsDataAdapter.getData().get(childPosition);
                        if (myPluginsDataAdapter.state == PluginCenterAdapter.STATE_MANAGE) {
                            Realm.getDefaultInstance().executeTransactionAsync(bgRealm -> {
                                RealmResults<Plugin> results = bgRealm.where(Plugin.class).equalTo("id", plugin.id).findAll();
                                if (!results.isEmpty()) {
                                    results.deleteAllFromRealm();
                                    Observable.just(myPluginsDataAdapter).observeOn(AndroidSchedulers.mainThread()).subscribe(adapter -> {
                                        myPluginsDataAdapter.getData().remove(plugin);
                                        myPluginsDataAdapter.notifyDataSetChanged();
                                    });
                                    RxBus.getInstance().post(PluginEvent.class, new PluginEvent(PluginEvent.TYPE_REFRESH, plugin));
                                }
                            });
                        } else {
                            if (plugin.type == Plugin.TYPE_JSOUP_GALLERY) {
                                startActivity(new Intent(PluginCenterActivity.this, SimpleViewPagerActivity.class)
                                        .putExtra(SimpleViewPagerActivity.EXTRA_TITLE, plugin.name)
                                        .putExtra(SimpleViewPagerActivity.EXTRA_TAB_DATASOURCE, plugin.reference + "-tab")
                                        .putExtra(SimpleViewPagerActivity.EXTRA_GALLERY_DATASOURCE, plugin.reference + "-gallery"));
                            }
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        MyPluginsDataSource myPluginsDataSource = new MyPluginsDataSource();

        MVCNormalHelper myPluginsMVCHelper = new MVCNormalHelper<>(myPlugins, new ILoadViewFactory.ILoadView() {
            @Override
            public void init(View switchView, View.OnClickListener onClickRefreshListener) {
            }

            @Override
            public void showLoading() {
            }

            @Override
            public void showFail(Exception e) {
            }

            @Override
            public void showEmpty() {
            }

            @Override
            public void tipFail(Exception e) {
            }

            @Override
            public void restore() {
            }
        }, new ILoadViewFactory.ILoadMoreView() {
            @Override
            public void init(ILoadViewFactory.FootViewAdder footViewHolder, View.OnClickListener onClickLoadMoreListener) {

            }

            @Override
            public void showNormal() {

            }

            @Override
            public void showNomore() {

            }

            @Override
            public void showLoading() {

            }

            @Override
            public void showFail(Exception e) {

            }
        });
        myPluginsMVCHelper.setDataSource(myPluginsDataSource);
        myPluginsMVCHelper.setAdapter(myPluginsDataAdapter);
        myPluginsMVCHelper.refresh();


        GridLayoutManager pluginCenterLayoutManager = new GridLayoutManager(this, getResources().getInteger(R.integer.pluginCenterSpanCount)) {

            @Override
            public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {
                super.onMeasure(recycler, state, widthSpec, heightSpec);
            }
        };
        pluginCenter.setLayoutManager(pluginCenterLayoutManager);

        pluginCenterDataAdapter = new PluginCenterAdapter(this, PluginCenterAdapter.TYPE_PLUGIN_CENTER);

        GestureDetector pluginCenterGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent event) {
                try {
                    View childView = pluginCenter.findChildViewUnder(event.getX(), event.getY());
                    int childPosition = pluginCenter.getChildAdapterPosition(childView);
                    if (-1 < childPosition && childPosition < pluginCenterDataAdapter.getData().size()) {
                        RxBus.getInstance().post(PluginEvent.class, new PluginEvent(PluginEvent.TYPE_MANAGE, pluginCenterDataAdapter.getData().get(childPosition)));
                    }
                } catch (Exception e) {
                    Timber.e(e, "pluginCenter touch exception");
                }
            }
        });

        pluginCenter.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (pluginCenterGestureDetector.onTouchEvent(e)) {
                    View childView = rv.findChildViewUnder(e.getX(), e.getY());
                    int childPosition = rv.getChildAdapterPosition(childView);
                    if (-1 < childPosition && childPosition < pluginCenterDataAdapter.getData().size()) {
                        Plugin plugin = pluginCenterDataAdapter.getData().get(childPosition);
                        if (pluginCenterDataAdapter.state == PluginCenterAdapter.STATE_MANAGE) {
                            Realm.getDefaultInstance().executeTransactionAsync(bgRealm -> {
                                RealmResults<Plugin> results = bgRealm.where(Plugin.class).equalTo("id", plugin.id).findAll();
                                if (results.isEmpty()) {
                                    bgRealm.insertOrUpdate(plugin);
                                    Observable.just(myPluginsDataAdapter).observeOn(AndroidSchedulers.mainThread()).subscribe(adapter -> {
                                        myPluginsDataAdapter.getData().add(plugin);
                                        myPluginsDataAdapter.notifyDataSetChanged();
                                    });
                                    RxBus.getInstance().post(PluginEvent.class, new PluginEvent(PluginEvent.TYPE_REFRESH, plugin));
                                }
                            });
                        } else {
                            if (plugin.type == Plugin.TYPE_JSOUP_GALLERY) {
                                startActivity(new Intent(PluginCenterActivity.this, SimpleViewPagerActivity.class)
                                        .putExtra(SimpleViewPagerActivity.EXTRA_TITLE, plugin.name)
                                        .putExtra(SimpleViewPagerActivity.EXTRA_TAB_DATASOURCE, plugin.reference + "-tab")
                                        .putExtra(SimpleViewPagerActivity.EXTRA_GALLERY_DATASOURCE, plugin.reference + "-gallery"));
                            }
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        PluginCenterDataSource pluginCenterDataSource = new PluginCenterDataSource(this);

        MVCNormalHelper pluginCenterMVCHelper = new MVCNormalHelper<>(pluginCenter, new ILoadViewFactory.ILoadView() {
            @Override
            public void init(View switchView, View.OnClickListener onClickRefreshListener) {
            }

            @Override
            public void showLoading() {
                showDynamicBoxCustomView(BaseActivity.DYNAMIC_BOX_AV_BALLGRIDPULSE, PluginCenterActivity.this);
            }

            @Override
            public void showFail(Exception e) {
                showDynamicBoxExceptionLayout(PluginCenterActivity.this);
            }

            @Override
            public void showEmpty() {
                restore();
            }

            @Override
            public void tipFail(Exception e) {
            }

            @Override
            public void restore() {
                dismissDynamicBox(PluginCenterActivity.this);
            }
        }, new ILoadViewFactory.ILoadMoreView() {
            @Override
            public void init(ILoadViewFactory.FootViewAdder footViewHolder, View.OnClickListener onClickLoadMoreListener) {

            }

            @Override
            public void showNormal() {

            }

            @Override
            public void showNomore() {

            }

            @Override
            public void showLoading() {

            }

            @Override
            public void showFail(Exception e) {

            }
        });
        pluginCenterMVCHelper.setDataSource(pluginCenterDataSource);
        pluginCenterMVCHelper.setAdapter(pluginCenterDataAdapter);
        pluginCenterMVCHelper.refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_plugin_center, menu);
        this.menu = menu;
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_manage:
                if(pluginCenterDataAdapter.isEmpty()) {
                    return true;
                }
                if (item.getTitle().toString().equals(getResources().getString(R.string.plugin_center_manage))) {
                    item.setTitle(R.string.plugin_center_complete);
                    myPluginsDataAdapter.state = PluginCenterAdapter.STATE_MANAGE;
                    pluginCenterDataAdapter.state = PluginCenterAdapter.STATE_MANAGE;
                    RxBus.getInstance().post(PluginEvent.class, new PluginEvent(PluginEvent.TYPE_MANAGE, null));
                } else {
                    item.setTitle(R.string.plugin_center_manage);
                    myPluginsDataAdapter.state = PluginCenterAdapter.STATE_COMPLETE;
                    pluginCenterDataAdapter.state = PluginCenterAdapter.STATE_COMPLETE;
                    RxBus.getInstance().post(PluginEvent.class, new PluginEvent(PluginEvent.TYPE_COMPLETE, null));
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
