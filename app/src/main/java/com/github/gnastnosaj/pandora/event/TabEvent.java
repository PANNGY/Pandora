package com.github.gnastnosaj.pandora.event;

/**
 * Created by jason on 10/17/2016.
 */

public class TabEvent {
    public final static String TAG_PANDORA_TAB = "pandora_tab";

    public int tab;

    public TabEvent(int tab) {
        this.tab = tab;
    }
}
