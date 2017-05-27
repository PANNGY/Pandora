package com.github.gnastnosaj.pandora.adapter;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.ui.widget.RatioImageView;
import com.shizhefei.mvc.IDataAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.trinea.android.common.util.ListUtils;

/**
 * Created by Jason on 7/17/2015.
 */
public class SimpleTabAdapter extends RecyclerView.Adapter implements IDataAdapter<List<JSoupData>> {

    private Context context;

    private List<JSoupData> data = new ArrayList<>();

    public SimpleTabAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_card_contains_simple_drawee, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        JSoupData jsoupData = data.get(position);
        ViewHolder holder = (ViewHolder) viewHolder;
        DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                .setUri(jsoupData.getAttr("thumbnail"))
                .setOldController(holder.thumbnail.getController())
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable anim) {
                        holder.thumbnail.setOriginalSize(imageInfo.getWidth(), imageInfo.getHeight());
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {

                    }
                }).build();
        holder.thumbnail.setController(draweeController);
        holder.title.setText(jsoupData.getAttr("title"));
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
        @BindView(R.id.radio_image_view)
        RatioImageView thumbnail;

        @BindView(R.id.text_view)
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
