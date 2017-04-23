package com.github.gnastnosaj.pandora.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by jasontsang on 10/18/16.
 */

public class Gank {
    public String _id;
    public String createdAt;
    public String desc;
    public List<String> images;
    public String publishedAt;
    public String source;
    public String type;
    public String url;
    public String used;
    public String who;

    @SerializedName("休息视频")
    public List<Gank> restVideoList;

    @SerializedName("Android")
    public List<Gank> androidList;

    @SerializedName("iOS")
    public List<Gank> iOSList;

    @SerializedName("前端")
    public List<Gank> f2List;

    @SerializedName("瞎推荐")
    public List<Gank> recommandList;

    @SerializedName("拓展资源")
    public List<Gank> extendList;
}
