package com.github.gnastnosaj.pandora.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.event.PluginEvent;
import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.ui.widget.RatioImageView;
import com.shizhefei.mvc.IDataAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Created by jasontsang on 12/24/16.
 */

public class PluginCenterAdapter extends RecyclerView.Adapter implements IDataAdapter<List<Plugin>> {

    public final static int TYPE_MY_PLUGINS = 0;
    public final static int TYPE_PLUGIN_CENTER = 1;

    public final static int STATE_COMPLETE = 0;
    public final static int STATE_MANAGE = 1;

    private Context context;
    public int type = TYPE_MY_PLUGINS;

    private List<Plugin> pluginList = new ArrayList<>();

    public int state = STATE_COMPLETE;

    public PluginCenterAdapter(Context context, int type) {
        this.context = context;
        this.type = type;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plugin, parent, false);
        return new PluginCenterAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Plugin plugin = pluginList.get(position);
        ViewHolder holder = (ViewHolder) viewHolder;

        holder.plugin = plugin;
        plugin.icon(context, holder.icon);
        holder.title.setText(plugin.name);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        try {
            ViewHolder holder = (ViewHolder) viewHolder;
            holder.stateDisposable.dispose();
        } catch (Exception e) {
            Timber.e(e, "onViewRecycled exception");
        }
        super.onViewRecycled(viewHolder);
    }

    @Override
    public int getItemCount() {
        return pluginList.size();
    }

    @Override
    public void notifyDataChanged(List<Plugin> plugins, boolean isRefresh) {
        if (isRefresh) {
            pluginList.clear();
        }
        pluginList.addAll(plugins);
        notifyDataSetChanged();
    }

    @Override
    public List<Plugin> getData() {
        return pluginList;
    }

    @Override
    public boolean isEmpty() {
        return pluginList.isEmpty();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.item_plugin)
        LinearLayout itemPlugin;

        @BindView(R.id.remove)
        View remove;

        @BindView(R.id.added)
        View added;

        @BindView(R.id.add)
        View add;

        @BindView(R.id.icon_plugin)
        RatioImageView icon;

        @BindView(R.id.title_plugin)
        TextView title;

        Plugin plugin;
        Disposable stateDisposable;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            if (type == TYPE_MY_PLUGINS) {
                if (state == STATE_MANAGE) {
                    itemPlugin.setBackgroundResource(R.drawable.item_plugin_border);
                    remove.setVisibility(View.VISIBLE);
                }

                stateDisposable = PluginEvent.observable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pluginEvent -> {
                            if (pluginEvent.type == PluginEvent.TYPE_MANAGE) {
                                itemPlugin.setBackgroundResource(R.drawable.item_plugin_border);
                                remove.setVisibility(View.VISIBLE);
                            } else if (pluginEvent.type == PluginEvent.TYPE_COMPLETE) {
                                itemPlugin.setBackgroundResource(R.drawable.item_plugin_selector);
                                remove.setVisibility(View.INVISIBLE);
                            } else if (pluginEvent.type == PluginEvent.TYPE_UPDATE) {

                            }
                        }, throwable -> Timber.e(throwable, "stateDisposable exception"));
            } else if (type == TYPE_PLUGIN_CENTER) {
                stateDisposable = PluginEvent.observable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pluginEvent -> {
                            if (pluginEvent.type == PluginEvent.TYPE_MANAGE) {
                                itemPlugin.setBackgroundResource(R.drawable.item_plugin_border);
                                Realm realm = Realm.getDefaultInstance();
                                RealmResults realmResults = realm.where(Plugin.class).equalTo("id", plugin.id).findAll();
                                if (realmResults.isEmpty()) {
                                    add.setVisibility(View.VISIBLE);
                                } else {
                                    added.setVisibility(View.VISIBLE);
                                }
                                realm.close();
                            } else if (pluginEvent.type == PluginEvent.TYPE_COMPLETE) {
                                itemPlugin.setBackgroundResource(R.drawable.item_plugin_selector);
                                add.setVisibility(View.INVISIBLE);
                                added.setVisibility(View.INVISIBLE);
                            } else if (pluginEvent.type == PluginEvent.TYPE_REFRESH) {
                                if (pluginEvent.plugin.id.equals(plugin.id)) {
                                    if (add.getVisibility() == View.VISIBLE) {
                                        add.setVisibility(View.INVISIBLE);
                                        added.setVisibility(View.VISIBLE);
                                    } else if (added.getVisibility() == View.VISIBLE) {
                                        added.setVisibility(View.INVISIBLE);
                                        add.setVisibility(View.VISIBLE);
                                    }
                                }
                            } else if (pluginEvent.type == PluginEvent.TYPE_UPDATE) {

                            }
                        }, throwable -> Timber.e(throwable, "stateDisposable exception"));
            }
        }
    }
}
