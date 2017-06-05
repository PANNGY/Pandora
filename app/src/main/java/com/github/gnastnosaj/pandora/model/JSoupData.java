package com.github.gnastnosaj.pandora.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupData extends RealmObject implements Parcelable {
    public JSoupData group;
    public RealmList<JSoupAttr> attrs;
    public RealmList<JSoupLink> tags;

    public String getAttr(String label) {
        for (JSoupAttr attr : attrs) {
            if (attr.label.equals(label)) {
                return attr.content;
            }
        }
        return null;
    }

    public static List<JSoupData> from(List<JSoupData> results) {
        List<JSoupData> data = new ArrayList<>();
        for (JSoupData jsoupData : results) {
            data.add(jsoupData.clone());
        }
        return data;
    }

    @Override
    public JSoupData clone() {
        JSoupData data = new JSoupData();
        if (group != null) {
            data.group = group.clone();
        }
        if (attrs != null) {
            data.attrs = new RealmList<>();
            for (JSoupAttr attr : attrs) {
                data.attrs.add(attr.clone());
            }
        }
        if (tags != null) {
            data.tags = new RealmList<>();
            for (JSoupLink tag : tags) {
                data.tags.add(tag.clone());
            }
        }
        return data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.group, flags);
        dest.writeList(this.attrs);
        dest.writeList(this.tags);
    }

    public JSoupData() {
    }

    protected JSoupData(Parcel in) {
        this.group = in.readParcelable(JSoupData.class.getClassLoader());
        this.attrs = new RealmList<>();
        in.readList(this.attrs, JSoupAttr.class.getClassLoader());
        this.tags = new RealmList<>();
        in.readList(this.tags, JSoupLink.class.getClassLoader());
    }

    public static final Parcelable.Creator<JSoupData> CREATOR = new Parcelable.Creator<JSoupData>() {
        @Override
        public JSoupData createFromParcel(Parcel source) {
            return new JSoupData(source);
        }

        @Override
        public JSoupData[] newArray(int size) {
            return new JSoupData[size];
        }
    };
}