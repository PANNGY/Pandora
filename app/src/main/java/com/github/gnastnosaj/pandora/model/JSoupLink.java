package com.github.gnastnosaj.pandora.model;

import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/5/17.
 */

public class JSoupLink extends RealmObject {
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
}
