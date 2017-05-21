package com.github.gnastnosaj.pandora.datasource;

import android.text.TextUtils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.trinea.android.common.util.ArrayUtils;

/**
 * Created by jasontsang on 5/3/17.
 */

public class JSoupFilter {
    public JSoupFilter[] filters;
    public String notQuery;
    public String regexp;
    public int[] indexes;
    public boolean first;
    public boolean last;

    public Elements filter(Elements elements) {
        if (!TextUtils.isEmpty(notQuery)) {
            elements = elements.not(notQuery);
        }
        if (!TextUtils.isEmpty(regexp)) {
            Pattern pattern = Pattern.compile(regexp);
            List<Element> filtered = new ArrayList<>();
            for (Element element : elements) {
                Matcher matcher = pattern.matcher(element.text());
                if (matcher.find()) {
                    filtered.add(element);
                }
            }
            elements = new Elements(filtered);
        }
        if (indexes != null && indexes.length != 0) {
            List<Element> filtered = new ArrayList<>();
            for (int index : indexes) {
                if (index < elements.size()) {
                    filtered.add(elements.get(index));
                }
            }
            elements = new Elements(filtered);
        }
        if (first) {
            elements = new Elements(elements.first());
        }
        if (last) {
            elements = new Elements(elements.last());
        }
        if (!ArrayUtils.isEmpty(filters)) {
            for (JSoupFilter filter : filters) {
                elements = filter.filter(elements);
            }
        }
        return elements;
    }
}