package com.github.gnastnosaj.pandora.model;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupData extends RealmObject {
    public JSoupData group;
    public RealmList<JSoupAttr> attrs;
    public RealmList<JSoupLink> tags;

    public String getAttr(String label) {
        for(JSoupAttr attr: attrs) {
            if(attr.label.equals(label)) {
                return attr.content;
            }
        }
        return null;
    }
}