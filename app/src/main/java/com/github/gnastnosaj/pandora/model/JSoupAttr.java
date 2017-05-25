package com.github.gnastnosaj.pandora.model;

import android.os.Parcel;
import android.os.Parcelable;

import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/14/17.
 */

public class JSoupAttr extends RealmObject implements Parcelable {
    public String label;
    public String content;

    public JSoupAttr() {
    }

    @Override
    protected JSoupAttr clone() {
        JSoupAttr attr = new JSoupAttr();
        attr.label = label;
        attr.content = content;
        return attr;
    }

    public JSoupAttr(String label, String content) {
        this.label = label;
        this.content = content;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.label);
        dest.writeString(this.content);
    }

    protected JSoupAttr(Parcel in) {
        this.label = in.readString();
        this.content = in.readString();
    }

    public static final Parcelable.Creator<JSoupAttr> CREATOR = new Parcelable.Creator<JSoupAttr>() {
        @Override
        public JSoupAttr createFromParcel(Parcel source) {
            return new JSoupAttr(source);
        }

        @Override
        public JSoupAttr[] newArray(int size) {
            return new JSoupAttr[size];
        }
    };
}
