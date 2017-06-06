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

public class JSoupCatalog extends RealmObject implements Parcelable {
    public JSoupLink link;
    public RealmList<JSoupLink> tags;

    @Override
    protected JSoupCatalog clone() {
        JSoupCatalog catalog = new JSoupCatalog();
        catalog.link = link.clone();
        catalog.tags = new RealmList<>();
        for (JSoupLink jsoupLink : tags) {
            catalog.tags.add(jsoupLink.clone());
        }
        return catalog;
    }

    public static List<JSoupCatalog> from(List<JSoupCatalog> results) {
        List<JSoupCatalog> catalog = new ArrayList<>();
        for (JSoupCatalog jsoupCatalog : results) {
            catalog.add(jsoupCatalog.clone());
        }
        return catalog;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.link, flags);
        dest.writeList(this.tags);
    }

    public JSoupCatalog() {
    }

    protected JSoupCatalog(Parcel in) {
        this.link = in.readParcelable(JSoupLink.class.getClassLoader());
        this.tags = new RealmList<>();
        in.readList(this.tags, JSoupLink.class.getClassLoader());
    }

    public static final Parcelable.Creator<JSoupCatalog> CREATOR = new Parcelable.Creator<JSoupCatalog>() {
        @Override
        public JSoupCatalog createFromParcel(Parcel source) {
            return new JSoupCatalog(source);
        }

        @Override
        public JSoupCatalog[] newArray(int size) {
            return new JSoupCatalog[size];
        }
    };
}