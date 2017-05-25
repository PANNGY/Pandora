package com.github.gnastnosaj.pandora.model;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupData extends RealmObject {
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

    public static List<JSoupData> from(RealmResults<JSoupData> results) {
        List<JSoupData> data = new ArrayList<>();
        for (JSoupData jsoupData : results) {
            data.add(jsoupData.clone());
        }
        return data;
    }

    @Override
    protected JSoupData clone() {
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
}