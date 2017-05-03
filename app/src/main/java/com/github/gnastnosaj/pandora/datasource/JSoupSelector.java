package com.github.gnastnosaj.pandora.datasource;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Map;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupSelector {
    public final static int METHOD_GET = 0;
    public final static int METHOD_POST = 1;

    public final static int BASE_ELEMENT = 0;
    public final static int BASE_DOCUMENT = 1;

    public final static int DEFAULT_TIMEOUT = 10000;

    public String url;
    public Map<String, String> headers;
    public String data;
    public int method;
    public int timeout;

    public String label;
    public boolean global;
    public String cssQuery;
    public JSoupFilter filter;
    public JSoupAnalyzer analyzer;

    public String parse(Document document, Element element) {
        return analyze(call(document, element));
    }

    public Elements call(Document document, Element element) {
        if (cssQuery != null) {
            Elements elements = global ? document.select(cssQuery) : element.select(cssQuery);
            if (filter != null) {
                elements = filter.filter(elements);
            }
            return elements;
        } else {
            return new Elements(element);
        }
    }

    public Elements call(Document document) {
        if (cssQuery != null) {
            Elements elements = document.select(cssQuery);
            if (filter != null) {
                elements = filter.filter(elements);
            }
            return elements;
        } else {
            return new Elements(document);
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
        if (headers != null) {
            connection.headers(headers);
        }
        if (data != null) {
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
