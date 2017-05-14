package com.github.gnastnosaj.pandora.model;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupData extends RealmObject {
    public RealmList<JSoupAttr> attrs;
    public RealmList<JSoupLink> tags;
}