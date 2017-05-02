package com.github.gnastnosaj.pandora.datasource;

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

    public String selector;
    public JSoupAnalyzer analyzer;
}
