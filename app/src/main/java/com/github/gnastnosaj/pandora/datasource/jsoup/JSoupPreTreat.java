package com.github.gnastnosaj.pandora.datasource.jsoup;

import org.jsoup.nodes.Element;

import cn.trinea.android.common.util.ArrayUtils;

/**
 * Created by jasontsang on 5/5/17.
 */

public class JSoupPreTreat {
    public JSoupPreTreat[] preTreats;
    public boolean nextElementSibling;

    public Element treat(Element element) {
        if (nextElementSibling) {
            Element el = element.nextElementSibling();
            if (el != null) {
                element = el;
            }
        }
        if (!ArrayUtils.isEmpty(preTreats)) {
            for (JSoupPreTreat preTreat : preTreats) {
                element = preTreat.treat(element);
            }
        }
        return element;
    }
}
