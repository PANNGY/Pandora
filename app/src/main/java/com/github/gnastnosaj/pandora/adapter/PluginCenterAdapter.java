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
        if (holder.stateDisposable == null || holder.stateDisposable.isDisposed()) {
            if (type == TYPE_MY_PLUGINS) {
                holder.stateDisposable = PluginEvent.observable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pluginEvent -> {
                            if (pluginEvent.type == PluginEvent.TYPE_MANAGE) {
                                holder.itemPlugin.setBackgroundResource(R.drawable.item_plugin_border);
                                holder.remove.setVisibility(View.VISIBLE);
                            } else if (pluginEvent.type == PluginEvent.TYPE_COMPLETE) {
                                holder.itemPlugin.setBackgroundResource(R.drawable.item_plugin_selector);
                                holder.remove.setVisibility(View.INVISIBLE);
                            } else if (pluginEvent.type == PluginEvent.TYPE_UPDATE) {
                                if (holder.plugin.id == pluginEvent.plugin.id) {
                                    holder.plugin = pluginEvent.plugin;
                                    holder.plugin.icon(context, holder.icon);
                                }
                            }
                        }, throwable -> Timber.e(throwable, "stateDisposable exception"));
            } else if (type == TYPE_PLUGIN_CENTER) {
                holder.stateDisposable = PluginEvent.observable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(pluginEvent -> {
                            if (pluginEvent.type == PluginEvent.TYPE_MANAGE) {
                                holder.itemPlugin.setBackgroundResource(R.drawable.item_plugin_border);
                                Realm realm = Realm.getDefaultInstance();
                                RealmResults realmResults = realm.where(Plugin.class).equalTo("id", plugin.id).findAll();
                                if (realmResults.isEmpty()) {
                                    holder.add.setVisibility(View.VISIBLE);
                                } else {
                                    holder.added.setVisibility(View.VISIBLE);
                                }
                                realm.close();
                            } else if (pluginEvent.type == PluginEvent.TYPE_COMPLETE) {
                                holder.itemPlugin.setBackgroundResource(R.drawable.item_plugin_selector);
                                holder.add.setVisibility(View.INVISIBLE);
                                holder.added.setVisibility(View.INVISIBLE);
                            } else if (pluginEvent.type == PluginEvent.TYPE_REFRESH) {
                                if (pluginEvent.plugin.id.equals(plugin.id)) {
                                    if (holder.add.getVisibility() == View.VISIBLE) {
                                        holder.add.setVisibility(View.INVISIBLE);
                                        holder.added.setVisibility(View.VISIBLE);
                                    } else if (holder.added.getVisibility() == View.VISIBLE) {
                                        holder.added.setVisibility(View.INVISIBLE);
                                        holder.add.setVisibility(View.VISIBLE);
                                    }
                                }
                            } else if (pluginEvent.type == PluginEvent.TYPE_UPDATE) {
                                if (holder.plugin.id == pluginEvent.plugin.id) {
                                    holder.plugin = pluginEvent.plugin;
                                    holder.plugin.icon(context, holder.icon);
                                }
                            }
                        }, throwable -> Timber.e(throwable, "stateDisposable exception"));
            }
        }

        holder.plugin = plugin;
        plugin.icon(context, holder.icon);
        holder.title.setText(plugin.name);

        if (type == TYPE_MY_PLUGINS && state == STATE_MANAGE) {
            holder.itemPlugin.setBackgroundResource(R.drawable.item_plugin_border);
            holder.remove.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        try {
            ViewHolder holder = (ViewHolder) viewHolder;
            holder.plugin = null;
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
        }
    }
}
