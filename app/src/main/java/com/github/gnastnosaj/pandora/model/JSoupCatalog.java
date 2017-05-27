package com.github.gnastnosaj.pandora.model;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupCatalog extends RealmObject {
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
}