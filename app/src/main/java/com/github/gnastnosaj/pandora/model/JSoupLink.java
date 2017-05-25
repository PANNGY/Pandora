package com.github.gnastnosaj.pandora.model;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/5/17.
 */

public class JSoupLink extends RealmObject implements Parcelable {
    public String title;
    public String url;

    public JSoupLink() {
    }

    @Override
    protected JSoupLink clone() {
        JSoupLink link = new JSoupLink();
        link.title = title;
        link.url = url;
        return link;
    }

    public JSoupLink(String title, String url) {
        this.title = title;
        this.url = url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.url);
    }

    protected JSoupLink(Parcel in) {
        this.title = in.readString();
        this.url = in.readString();
    }

    public static final Parcelable.Creator<JSoupLink> CREATOR = new Parcelable.Creator<JSoupLink>() {
        @Override
        public JSoupLink createFromParcel(Parcel source) {
            return new JSoupLink(source);
        }

        @Override
        public JSoupLink[] newArray(int size) {
            return new JSoupLink[size];
        }
    };
}
