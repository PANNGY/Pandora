package com.github.gnastnosaj.pandora.model;

import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/14/17.
 */

public class JSoupAttr extends RealmObject {
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
}
