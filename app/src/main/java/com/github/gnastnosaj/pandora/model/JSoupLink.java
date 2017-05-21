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

    public JSoupLink(String title, String url) {
        this.title = title;
        this.url = url;
    }
}
