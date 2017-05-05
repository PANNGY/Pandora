package com.github.gnastnosaj.pandora.datasource;

import android.text.TextUtils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Map;

import cn.trinea.android.common.util.MapUtils;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupSelector {
    public final static int METHOD_GET = 0;
    public final static int METHOD_POST = 1;

    public final static int DEFAULT_TIMEOUT = 10000;

    public String url;
    public Map<String, String> headers;
    public Map<String, String> data;
    public int method;
    public int timeout;

    public String label;
    public boolean global;
    public String placeholder;
    public JSoupPreTreat preTreat;
    public String cssQuery;
    public JSoupFilter filter;
    public JSoupAnalyzer analyzer;

    public String parse(Document document, Element element) {
        return analyze(call(document, element));
    }

    public String parse(Document document) {
        return analyze(call(document));
    }

    public Elements call(Document document, Element element) {
        Element el = global ? document : element;
        if (preTreat != null) {
            el = preTreat.treat(el);
        }
        if (!TextUtils.isEmpty(cssQuery)) {
            Elements elements = el.select(cssQuery);
            if (filter != null) {
                elements = filter.filter(elements);
            }
            return elements;
        } else {
            return new Elements(el);
        }
    }

    public Elements call(Document document) {
        Element el = document;
        if (preTreat != null) {
            el = preTreat.treat(el);
        }
        if (!TextUtils.isEmpty(cssQuery)) {
            Elements elements = el.select(cssQuery);
            if (filter != null) {
                elements = filter.filter(elements);
            }
            return elements;
        } else {
            return new Elements(el);
        }
    }

    public String analyze(Elements elements) {
        if (analyzer != null) {
            return analyzer.analyze(elements);
        } else {
            return null;
        }
    }

    public Document loadDocument() throws IOException {
        return loadDocument(url);
    }

    public Document loadDocument(String url) throws IOException {
        Connection connection = Jsoup.connect(url);
        if (!MapUtils.isEmpty(headers)) {
            connection.headers(headers);
        }
        if (!MapUtils.isEmpty(data)) {
            connection.data(data);
        }
        connection.timeout(timeout == 0 ? JSoupSelector.DEFAULT_TIMEOUT : timeout);
        Document document;
        if (method == JSoupSelector.METHOD_GET) {
            document = connection.get();
        } else {
            document = connection.post();
        }
        return document;
    }
}
