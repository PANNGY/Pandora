package com.github.gnastnosaj.pandora.datasource;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jasontsang on 5/3/17.
 */

public class JSoupFilter {
    public int[] indexes;

    public Elements filter(Elements elements) {
        List<Element> filtered = new ArrayList<>();
        for (int index : indexes) {
            if (index < elements.size()) {
                filtered.add(elements.get(index));
            }
        }
        return new Elements(filtered);
    }
}