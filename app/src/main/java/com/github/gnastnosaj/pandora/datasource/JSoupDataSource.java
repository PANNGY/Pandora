package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupTag;
import com.github.gnastnosaj.pandora.model.JSoupType;
import com.shizhefei.mvc.IDataCacheLoader;
import com.shizhefei.mvc.IDataSource;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupDataSource implements IDataSource<List<JSoupData>>, IDataCacheLoader<List<JSoupData>> {

    public TypeSelector typeSelector;
    public String firstPageSelector;
    public String nextPageSelecttor;
    public String previousPageSelector;

    private String currentUrl;

    public Observable loadType() {
        return Observable.create(subscriber -> {
            Connection connection = Jsoup.connect(typeSelector.url);
            if (typeSelector.headers != null) {
                connection.headers(typeSelector.headers);
            }
            if (typeSelector.data != null) {
                connection.data(typeSelector.data);
            }
            connection.timeout(typeSelector.timeout == 0 ? JSoupSelector.DEFAULT_TIMEOUT : typeSelector.timeout);
            Document document = null;
            if (typeSelector.method == JSoupSelector.METHOD_GET) {
                document = connection.get();
            } else {
                document = connection.post();
            }

            if (typeSelector.selector != null) {
                List<JSoupType> types = new ArrayList<>();
                Elements typeElements = document.select(typeSelector.selector);
                for (Element typeElement : typeElements) {
                    JSoupType jsoupType = new JSoupType();
                    if (typeSelector.titleSelector != null) {
                        String typeTitle = null;
                        if (typeSelector.titleSelector.selector == null) {
                            typeTitle = typeSelector.titleSelector.analyzer.analyze(typeElement);
                        } else {
                            typeTitle = typeSelector.titleSelector.analyzer.analyze(typeElement.select(typeSelector.titleSelector.selector));
                        }
                        jsoupType.title = typeTitle;
                    }
                    if (typeSelector.urlSelector != null) {
                        String typeUrl = null;
                        if (typeSelector.urlSelector.selector == null) {
                            typeUrl = typeSelector.urlSelector.analyzer.analyze(typeElement);
                        } else {
                            typeUrl = typeSelector.urlSelector.analyzer.analyze(typeElement.select(typeSelector.urlSelector.selector));
                        }
                        jsoupType.url = typeUrl;
                    }
                    if (typeSelector.tagSelector != null) {
                        jsoupType.tags = new ArrayList<>();
                        Elements tagElements = document.select(typeSelector.tagSelector.selector);
                        for (Element tagElement : tagElements) {
                            JSoupTag jsoupTag = new JSoupTag();
                            if (typeSelector.tagSelector.titleSelector != null) {
                                String tagTitle = null;
                                if (typeSelector.tagSelector.titleSelector.selector == null) {
                                    tagTitle = typeSelector.tagSelector.titleSelector.analyzer.analyze(tagElement);
                                } else {
                                    tagTitle = typeSelector.tagSelector.titleSelector.analyzer.analyze(tagElement.select(typeSelector.tagSelector.titleSelector.selector));
                                }
                                jsoupTag.title = tagTitle;
                            }
                            if (typeSelector.tagSelector.urlSelector != null) {
                                String tagUrl = null;
                                if (typeSelector.tagSelector.urlSelector.selector == null) {
                                    tagUrl = typeSelector.tagSelector.urlSelector.analyzer.analyze(tagElement);
                                } else {
                                    tagUrl = typeSelector.tagSelector.urlSelector.analyzer.analyze(tagElement.select(typeSelector.tagSelector.urlSelector.selector));
                                }
                                jsoupTag.url = tagUrl;
                            }
                            jsoupType.tags.add(jsoupTag);
                        }
                    }
                    types.add(jsoupType);
                }
                subscriber.onNext(types);
            } else if (typeSelector.tagSelector != null) {
                List<JSoupTag> tags = new ArrayList<>();
                Elements tagElements = document.select(typeSelector.tagSelector.selector);
                for (Element tagElement : tagElements) {
                    JSoupTag jsoupTag = new JSoupTag();
                    if (typeSelector.tagSelector.titleSelector != null) {
                        String tagTitle = null;
                        if (typeSelector.tagSelector.titleSelector.selector == null) {
                            tagTitle = typeSelector.tagSelector.titleSelector.analyzer.analyze(tagElement);
                        } else {
                            tagTitle = typeSelector.tagSelector.titleSelector.analyzer.analyze(tagElement.select(typeSelector.tagSelector.titleSelector.selector));
                        }
                        jsoupTag.title = tagTitle;
                    }
                    if (typeSelector.tagSelector.urlSelector != null) {
                        String tagUrl = null;
                        if (typeSelector.tagSelector.urlSelector.selector == null) {
                            tagUrl = typeSelector.tagSelector.urlSelector.analyzer.analyze(tagElement);
                        } else {
                            tagUrl = typeSelector.tagSelector.urlSelector.analyzer.analyze(tagElement.select(typeSelector.tagSelector.urlSelector.selector));
                        }
                        jsoupTag.url = tagUrl;
                    }
                    tags.add(jsoupTag);
                }
                subscriber.onNext(tags);
            } else {
                subscriber.onError(new Throwable("both selector and tagSelector is empty"));
            }

            subscriber.onComplete();
        }).subscribeOn(Schedulers.newThread());
    }

    @Override
    public List<JSoupData> loadCache(boolean isEmpty) {
        return null;
    }

    @Override
    public List<JSoupData> refresh() throws Exception {
        return null;
    }

    @Override
    public List<JSoupData> loadMore() throws Exception {
        return null;
    }

    @Override
    public boolean hasMore() {
        return false;
    }

    public static class TagSelector extends JSoupSelector {
        public JSoupSelector titleSelector;
        public JSoupSelector urlSelector;
    }

    public static class TypeSelector extends JSoupSelector {
        public JSoupSelector titleSelector;
        public JSoupSelector urlSelector;
        public TagSelector tagSelector;
    }
}
