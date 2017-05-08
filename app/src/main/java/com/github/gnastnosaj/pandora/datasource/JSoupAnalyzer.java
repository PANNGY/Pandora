package com.github.gnastnosaj.pandora.datasource;

import android.text.TextUtils;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.trinea.android.common.util.ArrayUtils;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupAnalyzer {
    public final static int METHOD_TEXT = 0;
    public final static int METHOD_ATTR = 1;

    public int method;
    public String[] args;
    public String format;
    public JSoupRegexp regexp;

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
                        data = matcher.group().trim();
                    }
                }
            }
        }
        return data;
    }

    public static class JSoupRegexp {
        public String pattern;
        public String replace;
    }
}
