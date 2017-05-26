package com.github.gnastnosaj.pandora.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.PandoraHomeDataSource.Model;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.ui.widget.RatioImageView;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.shizhefei.mvc.IDataAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.trinea.android.common.util.ListUtils;

/**
 * Created by Jason on 7/17/2015.
 */
public class PandoraHomeAdapter extends RecyclerView.Adapter implements IDataAdapter<List<Model>> {
    public final static String SLIDE_BUNDLE_TITILE = "title";
    public final static String SLIDE_BUNDLE_THUMBNAIL = "thumbnail";
    public final static String SLIDE_BUNDLE_HREF = "href";

    private Context context;

    private List<Model> models = new ArrayList<>();

    public PandoraHomeAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = null;
        if (viewType == 0) {
            v = new SliderLayout(context);
        } else if (viewType == 1) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_pandora_home_group, parent, false);
        } else if (viewType == 2) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_card_contains_simple_drawee, parent, false);
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Model model = models.get(position);
        ViewHolder holder = (ViewHolder) viewHolder;
        if (model.type == Model.TYPE_VALUE_SLIDE) {
            SliderLayout sliderLayout = (SliderLayout) holder.itemView;
            int sliderLayoutWidth = ((Activity) context).findViewById(R.id.recycler_view).getWidth();
            int sliderLayoutHeight = sliderLayoutWidth * 9 / 21;
            StaggeredGridLayoutManager.LayoutParams itemViewParams = new StaggeredGridLayoutManager.LayoutParams(sliderLayoutWidth, sliderLayoutHeight);
            itemViewParams.setFullSpan(true);
            sliderLayout.setLayoutParams(itemViewParams);
            sliderLayout.removeAllSliders();
            for (JSoupData data : (List<JSoupData>) model.data) {
                String title = data.getAttr("title");
                String thumbnail = data.getAttr("thumbnail");
                String href = data.getAttr("href");

                Bundle bundle = new Bundle();
                bundle.putString(SLIDE_BUNDLE_TITILE, title);
                bundle.putString(SLIDE_BUNDLE_THUMBNAIL, thumbnail);
                bundle.putString(SLIDE_BUNDLE_HREF, href);

                TextSliderView textSliderView = new TextSliderView(context);
                textSliderView.description(title).image(thumbnail).bundle(bundle)
                        .setScaleType(BaseSliderView.ScaleType.Fit);
                sliderLayout.addSlider(textSliderView);
            }
            sliderLayout.setPresetTransformer(SliderLayout.Transformer.Accordion);
            sliderLayout.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
            sliderLayout.setCustomAnimation(new DescriptionAnimation());
            sliderLayout.setDuration(4000);
        } else if (model.type == Model.TYPE_VALUE_GROUP) {
            StaggeredGridLayoutManager.LayoutParams itemViewParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            itemViewParams.setFullSpan(true);
            holder.itemView.setLayoutParams(itemViewParams);
            holder.groupName.setText((String) model.data);
            holder.iconMore.setImageDrawable(new IconicsDrawable(context)
                    .icon(MaterialDesignIconic.Icon.gmi_chevron_right)
                    .color(Color.BLACK).sizeDp(18));
        } else if (model.type == Model.TYPE_VALUE_DATA) {
            DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                    .setUri(((JSoupData) model.data).getAttr("thumbnail"))
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
            holder.title.setText(((JSoupData) model.data).getAttr("title"));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return models.get(position).type;
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    @Override
    public void notifyDataChanged(List<Model> models, boolean isRefresh) {
        if (isRefresh) {
            this.models.clear();
        }
        this.models.addAll(models);
        notifyDataSetChanged();
    }

    @Override
    public List<Model> getData() {
        return models;
    }

    @Override
    public boolean isEmpty() {
        return ListUtils.isEmpty(models);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @Nullable
        @BindView(R.id.radio_image_view)
        RatioImageView thumbnail;

        @Nullable
        @BindView(R.id.text_view)
        TextView title;

        @Nullable
        @BindView(R.id.group_name)
        TextView groupName;

        @Nullable
        @BindView(R.id.icon_more)
        ImageView iconMore;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
