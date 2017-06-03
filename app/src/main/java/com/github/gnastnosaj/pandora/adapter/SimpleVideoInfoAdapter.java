package com.github.gnastnosaj.pandora.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.VideoInfo;
import com.shizhefei.mvc.IDataAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Jason on 7/17/2015.
 */
public class SimpleVideoInfoAdapter extends RecyclerView.Adapter implements IDataAdapter<List<VideoInfo>> {

    private Context context;
    private List<VideoInfo> videoInfoList = new ArrayList<>();

    public SimpleVideoInfoAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v;
        if (i == 0) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_thumbnail_title_rank, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_title, parent, false);
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        VideoInfo videoInfo = videoInfoList.get(position);
        ViewHolder holder = (ViewHolder) viewHolder;
        if (getItemViewType(position) == 0) {
            holder.title.setText(videoInfo.title);
            holder.thumbnail.setImageURI(videoInfo.thumbnail);
        } else {
            holder.title.setText(videoInfo.title);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (videoInfoList.get(position).thumbnail == null) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return videoInfoList.size();
    }

    @Override
    public void notifyDataChanged(List<VideoInfo> videoInfoList, boolean isRefresh) {
        if (isRefresh) {
            this.videoInfoList.clear();
        }
        this.videoInfoList.addAll(videoInfoList);
        notifyDataSetChanged();
    }

    @Override
    public List<VideoInfo> getData() {
        return videoInfoList;
    }

    @Override
    public boolean isEmpty() {
        return videoInfoList == null || videoInfoList.size() == 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @Nullable
        @BindView(R.id.thumbnail)
        SimpleDraweeView thumbnail;

        @BindView(R.id.title)
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
