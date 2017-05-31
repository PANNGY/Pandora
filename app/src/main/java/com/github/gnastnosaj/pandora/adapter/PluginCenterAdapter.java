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
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * Created by jasontsang on 12/24/16.
 */

public class PluginCenterAdapter extends RecyclerView.Adapter implements IDataAdapter<List<Plugin>> {

    private Context context;
    private List<Plugin> pluginList = new ArrayList<>();

    public int state = 0;

    public PluginCenterAdapter(Context context) {
        this.context = context;
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

            if (state == 1) {
                itemPlugin.setBackgroundResource(R.drawable.item_plugin_border);
                remove.setVisibility(View.VISIBLE);
            }

            stateDisposable = PluginEvent.observable.subscribe(pluginEvent -> {
                if (pluginEvent.type == 0) {
                    itemPlugin.setBackgroundResource(R.drawable.item_plugin_border);
                    remove.setVisibility(View.VISIBLE);
                } else if (pluginEvent.type == 1) {
                    itemPlugin.setBackgroundResource(R.drawable.item_plugin_selector);
                    remove.setVisibility(View.INVISIBLE);
                } else if (pluginEvent.type == 3) {
                    plugin.icon(context, icon);
                }
            }, throwable -> Timber.e(throwable, "stateDisposable exception"));
        }
    }
}
