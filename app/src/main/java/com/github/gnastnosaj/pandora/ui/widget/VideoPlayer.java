package com.github.gnastnosaj.pandora.ui.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.pandora.event.VideoEvent;

import fm.jiecao.jcvideoplayer_lib.JCMediaManager;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;

/**
 * Created by jasontsang on 1/3/17.
 */

public class VideoPlayer extends JCVideoPlayerStandard {
    private static VideoPlayer videoPlayer;

    public VideoPlayer(Context context) {
        super(context);
        videoPlayer = this;
    }

    public VideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        videoPlayer = this;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == fm.jiecao.jcvideoplayer_lib.R.id.back) {
            destroy();
        } else {
            super.onClick(v);
            if (i == fm.jiecao.jcvideoplayer_lib.R.id.start) {
                if (currentState == CURRENT_STATE_PREPARING) {
                    videoPlayer = this;
                }
            } else if (i == fm.jiecao.jcvideoplayer_lib.R.id.fullscreen) {
                RxBus.getInstance().post(VideoEvent.class, new VideoEvent(VideoEvent.TYPE_ON_FULLSCREEN));
            }
        }
    }

    @Override
    public void onAutoCompletion() {
        super.onAutoCompletion();
        RxBus.getInstance().post(VideoEvent.class, new VideoEvent(VideoEvent.TYPE_ON_AUTO_COMPLETION));
    }

    @Override
    public void onError(int what, int extra) {
        super.onError(what, extra);
        RxBus.getInstance().post(VideoEvent.class, new VideoEvent(VideoEvent.TYPE_ON_ERROR));
    }

    public static boolean resume() {
        if (videoPlayer != null && videoPlayer.startButton != null && !TextUtils.isEmpty(videoPlayer.url)) {
            if (!JCMediaManager.instance().mediaPlayer.isPlaying()) {
                videoPlayer.startButton.performClick();
                return true;
            }
        }
        return false;
    }

    public static boolean pause() {
        if (videoPlayer != null && videoPlayer.startButton != null && !TextUtils.isEmpty(videoPlayer.url)) {
            if (JCMediaManager.instance().mediaPlayer.isPlaying()) {
                videoPlayer.startButton.performClick();
                return true;
            }
        }
        return false;
    }

    public static boolean destroy() {
        if (videoPlayer != null) {
            if (!backPress()) {
                videoPlayer.clearFullscreenLayout();
            }
            releaseAllVideos();
            videoPlayer = null;
            return true;
        } else {
            return false;
        }
    }
}
