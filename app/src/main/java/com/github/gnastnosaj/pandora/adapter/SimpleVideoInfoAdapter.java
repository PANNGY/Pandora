package com.github.gnastnosaj.pandora.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.VideoInfo;
import com.shizhefei.mvc.IDataAdapter;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.listener.StandardVideoAllCallBack;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Jason on 7/17/2015.
 */
public class SimpleVideoInfoAdapter extends RecyclerView.Adapter implements IDataAdapter<List<VideoInfo>> {
    public final static String PLAY_TAG = "simple_video";

    private Context context;
    private List<VideoInfo> videoInfoList = new ArrayList<>();

    public SimpleVideoInfoAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v;
        if (i == 2) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_player, parent, false);
        } else if (i == 1) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_thumbnail_title_rank, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_title, parent, false);
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int position) {
        VideoInfo videoInfo = videoInfoList.get(position);
        ViewHolder holder = (ViewHolder) viewHolder;
        if (getItemViewType(position) == 2) {
            SimpleDraweeView thumbnail = new SimpleDraweeView(context);
            thumbnail.setImageURI(videoInfo.thumbnail);
            thumbnail.getHierarchy().setPlaceholderImage(R.drawable.ic_source_pandora_light);
            holder.videoPlayer.setThumbImageView(thumbnail);
            if (videoInfo.url.startsWith("stack://")) {
                String[] urls = videoInfo.url.substring(8).split(" , ");
                for (String url : urls) {
                    holder.videoPlayer.setUp(url, true, null, videoInfo.title);
                }
            } else {
                holder.videoPlayer.setUp(videoInfo.url, true, null, videoInfo.title);
            }
            holder.videoPlayer.getBackButton().setVisibility(View.GONE);
            holder.videoPlayer.getFullscreenButton().setOnClickListener(view -> holder.videoPlayer.startWindowFullscreen(context, true, true));
            holder.videoPlayer.setRotateViewAuto(true);
            holder.videoPlayer.setLockLand(true);
            holder.videoPlayer.setShowFullAnimation(true);
            holder.videoPlayer.setNeedLockFull(true);
            holder.videoPlayer.setStandardVideoAllCallBack(new VideoAllCallBack() {
                @Override
                public void onPrepared(String url, Object... objects) {
                    super.onPrepared(url, objects);
                    if (!holder.videoPlayer.isIfCurrentIsFullscreen()) {
                        GSYVideoManager.instance().setNeedMute(true);
                    }

                }

                @Override
                public void onQuitFullscreen(String url, Object... objects) {
                    super.onQuitFullscreen(url, objects);
                    GSYVideoManager.instance().setNeedMute(true);
                }

                @Override
                public void onEnterFullscreen(String url, Object... objects) {
                    super.onEnterFullscreen(url, objects);
                    GSYVideoManager.instance().setNeedMute(false);
                }
            });
        } else if (getItemViewType(position) == 1) {
            holder.title.setText(videoInfo.title);
            holder.thumbnail.setImageURI(videoInfo.thumbnail);
        } else {
            holder.title.setText(videoInfo.title);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (!TextUtils.isEmpty(videoInfoList.get(position).url)) {
            return 2;
        } else if (!TextUtils.isEmpty(videoInfoList.get(position).thumbnail)) {
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

        @Nullable
        @BindView(R.id.title)
        TextView title;

        @Nullable
        @BindView(R.id.video_player)
        StandardGSYVideoPlayer videoPlayer;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public class VideoAllCallBack implements StandardVideoAllCallBack {

        @Override
        public void onClickStartIcon(String url, Object... objects) {

        }

        @Override
        public void onClickStartError(String url, Object... objects) {

        }

        @Override
        public void onClickStop(String url, Object... objects) {

        }

        @Override
        public void onClickStopFullscreen(String url, Object... objects) {

        }

        @Override
        public void onClickResume(String url, Object... objects) {

        }

        @Override
        public void onClickResumeFullscreen(String url, Object... objects) {

        }

        @Override
        public void onClickSeekbar(String url, Object... objects) {

        }

        @Override
        public void onClickSeekbarFullscreen(String url, Object... objects) {

        }

        @Override
        public void onAutoComplete(String url, Object... objects) {

        }

        @Override
        public void onEnterFullscreen(String url, Object... objects) {

        }

        @Override
        public void onQuitFullscreen(String url, Object... objects) {

        }

        @Override
        public void onQuitSmallWidget(String url, Object... objects) {
        }

        @Override
        public void onEnterSmallWidget(String url, Object... objects) {

        }

        @Override
        public void onTouchScreenSeekVolume(String url, Object... objects) {

        }

        @Override
        public void onTouchScreenSeekPosition(String url, Object... objects) {

        }

        @Override
        public void onTouchScreenSeekLight(String url, Object... objects) {

        }

        @Override
        public void onClickStartThumb(String url, Object... objects) {

        }

        @Override
        public void onClickBlank(String url, Object... objects) {

        }

        @Override
        public void onClickBlankFullscreen(String url, Object... objects) {

        }

        @Override
        public void onPrepared(String url, Object... objects) {

        }

        @Override
        public void onPlayError(String url, Object... objects) {

        }
    }
}
