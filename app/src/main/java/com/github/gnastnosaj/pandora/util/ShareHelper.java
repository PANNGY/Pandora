package com.github.gnastnosaj.pandora.util;

import android.app.Activity;

import com.bilibili.socialize.share.core.BiliShare;
import com.bilibili.socialize.share.core.SocializeListeners;
import com.bilibili.socialize.share.core.SocializeMedia;
import com.bilibili.socialize.share.core.shareparam.BaseShareParam;

/**
 * Created by jasontsang on 5/25/17.
 */

public class ShareHelper implements SocializeListeners.ShareListener {
    private final static ShareHelper instance = new ShareHelper();

    public static void share(Activity activity, BaseShareParam param) {
        BiliShare.global().share(activity, SocializeMedia.GENERIC, param, instance);
    }

    public static void share(Activity activity, BaseShareParam param, ShareHelper shareHelper) {
        BiliShare.global().share(activity, SocializeMedia.GENERIC, param, shareHelper);
    }

    @Override
    public void onStart(SocializeMedia type) {

    }

    @Override
    public void onProgress(SocializeMedia type, String progressDesc) {

    }

    @Override
    public void onSuccess(SocializeMedia type, int code) {

    }

    @Override
    public void onError(SocializeMedia type, int code, Throwable error) {

    }

    @Override
    public void onCancel(SocializeMedia type) {

    }
}
