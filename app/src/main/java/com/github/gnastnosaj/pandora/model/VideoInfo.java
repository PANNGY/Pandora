package com.github.gnastnosaj.pandora.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jasontsang on 12/26/16.
 */

public class VideoInfo implements Parcelable {
    public String id;
    public String title;
    public String thumbnail;
    public String url;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.title);
        dest.writeString(this.thumbnail);
        dest.writeString(this.url);
    }

    public VideoInfo() {
    }

    protected VideoInfo(Parcel in) {
        this.id = in.readString();
        this.title = in.readString();
        this.thumbnail = in.readString();
        this.url = in.readString();
    }

    public static final Parcelable.Creator<VideoInfo> CREATOR = new Parcelable.Creator<VideoInfo>() {
        @Override
        public VideoInfo createFromParcel(Parcel source) {
            return new VideoInfo(source);
        }

        @Override
        public VideoInfo[] newArray(int size) {
            return new VideoInfo[size];
        }
    };
}