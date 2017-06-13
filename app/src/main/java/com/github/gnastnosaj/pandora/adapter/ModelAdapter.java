package com.github.gnastnosaj.pandora.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.shizhefei.mvc.IDataAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jasontsang on 6/13/17.
 */

public class ModelAdapter extends RecyclerView.Adapter implements IDataAdapter<List<JSoupData>> {
    private List<JSoupData> data = new ArrayList<>();
    private Context context;

    public ModelAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_thumbnail_title_rank, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        JSoupData jsoupData = data.get(position);
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.thumbnail.setImageURI(jsoupData.getAttr("thumbnail"));
        holder.title.setText(jsoupData.getAttr("title"));
        String rankStr = jsoupData.getAttr("rankStr");
        if (!TextUtils.isEmpty(rankStr)) {
            if (rankStr.contains("▼")) {
                holder.rank.setText(rankStr.substring(0, rankStr.lastIndexOf(" ")));
                holder.rankUpDown.setImageDrawable(new IconicsDrawable(context)
                        .icon(MaterialDesignIconic.Icon.gmi_caret_down)
                        .color(Color.GREEN).sizeDp(18));
            } else if (rankStr.contains("▲")) {
                holder.rank.setText(rankStr.substring(0, rankStr.lastIndexOf(" ")));
                holder.rankUpDown.setImageDrawable(new IconicsDrawable(context)
                        .icon(MaterialDesignIconic.Icon.gmi_caret_up)
                        .color(Color.RED).sizeDp(18));
            } else {
                holder.rank.setText(rankStr);
            }
        }
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
        return data.isEmpty();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @Nullable
        @BindView(R.id.thumbnail)
        SimpleDraweeView thumbnail;

        @Nullable
        @BindView(R.id.title)
        TextView title;

        @Nullable
        @BindView(R.id.rank)
        TextView rank;

        @Nullable
        @BindView(R.id.rank_up_down)
        ImageView rankUpDown;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
