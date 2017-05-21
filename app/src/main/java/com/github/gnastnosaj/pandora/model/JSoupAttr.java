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

    public JSoupAttr(String label, String content) {
        this.label = label;
        this.content = content;
    }
}
