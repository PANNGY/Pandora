package com.github.gnastnosaj.pandora.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.IDataAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.trinea.android.common.util.ListUtils;

/**
 * Created by Jason on 7/17/2015.
 */
public class BTDBAdapter extends RecyclerView.Adapter implements IDataAdapter<List<JSoupData>> {

    private List<JSoupData> data = new ArrayList<>();

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_btdb, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        JSoupData jsoupData = data.get(position);
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.titleView.setText(jsoupData.getAttr("title"));
        holder.sizeView.setText(jsoupData.getAttr("size"));
        holder.filesView.setText(jsoupData.getAttr("files"));
        holder.addTimeView.setText(jsoupData.getAttr("addTime"));
        holder.popularityView.setText(jsoupData.getAttr("popularity"));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void notifyDataChanged(List<JSoupData> data, boolean isRefresh) {
        if (isRefresh) {
            this.data.clear();
        }
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public List<JSoupData> getData() {
        return data;
    }

    @Override
    public boolean isEmpty() {
        return ListUtils.isEmpty(data);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_btdb_title)
        TextView titleView;

        @BindView(R.id.tv_btdb_size)
        TextView sizeView;

        @BindView(R.id.tv_btdb_files)
        TextView filesView;

        @BindView(R.id.tv_btdb_addTime)
        TextView addTimeView;

        @BindView(R.id.tv_btdb_popularity)
        TextView popularityView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

}
