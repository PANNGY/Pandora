package com.github.gnastnosaj.pandora.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.github.gnastnosaj.pandora.model.JSoupData;
import com.shizhefei.mvc.IDataAdapter;

import java.util.List;

/**
 * Created by jasontsang on 6/13/17.
 */

public class ModelAdapter extends RecyclerView.Adapter implements IDataAdapter<List<JSoupData>> {
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    @Override
    public void notifyDataChanged(List<JSoupData> jSoupDatas, boolean isRefresh) {

    }

    @Override
    public List<JSoupData> getData() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
