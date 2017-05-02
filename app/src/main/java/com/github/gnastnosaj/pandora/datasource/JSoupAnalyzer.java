package com.github.gnastnosaj.pandora.datasource;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import cn.trinea.android.common.util.ArrayUtils;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupAnalyzer {
    public final static int METHOD_TEXT = 0;
    public final static int METHOD_HTML = 1;
    public final static int METHOD_ATTR = 2;

    public int method;
    public String[] args;

    public String analyze(Elements elements) {
        return analyze(elements.first());
    }

    public String analyze(Element element) {
        String content = null;
        switch (method) {
            case METHOD_TEXT:
                content = element.text();
                break;
            case METHOD_HTML:
                content = element.html();
                break;
            case METHOD_ATTR:
                if (!ArrayUtils.isEmpty(args)) {
                    content = element.attr(args[0]);
                }
                break;
        }
        return content;
    }
}
