package com.github.gnastnosaj.pandora.datasource;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupSelector {
    public final static int METHOD_GET = 0;
    public final static int METHOD_POST = 1;

    public final static int DEFAULT_TIMEOUT = 10000;

    public String url;
    public Map<String, String> headers;
    public String data;
    public int method;
    public int timeout;

    public String label;
    public String selector;
    public JSoupAnalyzer analyzer;

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
