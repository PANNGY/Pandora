package com.github.gnastnosaj.pandora.datasource.jsoup;

import android.text.TextUtils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.trinea.android.common.util.ArrayUtils;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupAnalyzer {
    public final static int METHOD_TEXT = 0;
    public final static int METHOD_ATTR = 1;
    public final static int METHOD_HTML = 2;

    public int method;
    public String[] args;
    public String format;
    public JSoupRegexp regexp;
    public boolean urlDecode;

    public String analyze(Elements elements) {
        if (elements == null || elements.isEmpty()) {
            return null;
        } else {
            return analyze(elements.first());
        }
    }

    public String analyze(Element element) {
        String content = null;
        switch (method) {
            case METHOD_TEXT:
                content = element.text();
                break;
            case METHOD_ATTR:
                if (!ArrayUtils.isEmpty(args)) {
                    for (String arg : args) {
                        content = element.attr(arg);
                        if (!TextUtils.isEmpty(content)) {
                            break;
                        }
                    }
                }
                break;
            case METHOD_HTML:
                content = element.html();
                break;
        }
        content = betterData(content);
        return content;
    }

    private String betterData(String data) {
        if (!TextUtils.isEmpty(data)) {
            data = data.trim();
            if (data.startsWith("javascript")) {
                data = "";
            }
            if (!TextUtils.isEmpty(format)) {
                data = String.format(format, data);
            }
            if (regexp != null && !TextUtils.isEmpty(regexp.pattern)) {
                if (regexp.replace != null) {
                    data = data.replaceAll(regexp.pattern, regexp.replace);
                } else {
                    Pattern pattern = Pattern.compile(regexp.pattern);
                    Matcher matcher = pattern.matcher(data);
                    if (matcher.find()) {
                        if(TextUtils.isEmpty(regexp.format)) {
                            data = matcher.group().trim();
                        }else {
                            data = String.format(regexp.format, data);
                        }
                    }
                }
            }
            if(urlDecode) {
                data = URLDecoder.decode(data);
            }
        }
        return data;
    }

    public static class JSoupRegexp {
        public String pattern;
        public String replace;
        public String format;
    }
}
