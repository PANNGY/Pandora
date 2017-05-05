package com.github.gnastnosaj.pandora.datasource;

import org.jsoup.nodes.Element;

/**
 * Created by jasontsang on 5/5/17.
 */

public class JSoupPreTreat {
    public boolean nextElementSibling;

    public Element treat(Element element) {
        if (nextElementSibling) {
            element = element.nextElementSibling();
        }
        return element;
    }
}