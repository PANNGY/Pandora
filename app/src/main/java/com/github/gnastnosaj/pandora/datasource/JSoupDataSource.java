package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupCatalog;
import com.shizhefei.mvc.IDataCacheLoader;
import com.shizhefei.mvc.IDataSource;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.trinea.android.common.util.ArrayUtils;
import cn.trinea.android.common.util.MapUtils;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupDataSource implements IDataSource<List<JSoupData>>, IDataCacheLoader<List<JSoupData>> {

    public String baseUrl;
    public String[] pages;
    public Map<String, String> areas;

    public CatalogSelector catalogSelector;
    public String firstPageSelector;
    public String nextPageSelecttor;
    public String previousPageSelector;

    private String currentUrl;

    public Observable<List<JSoupCatalog>> loadCatalogs() {
        return Observable.<List<JSoupCatalog>>create(subscriber -> {
            if (baseUrl != null) {
                catalogSelector.url = catalogSelector.url.replace("{baseUrl}", baseUrl);
            }
            if (!ArrayUtils.isEmpty(pages)) {
                Matcher matcher = Pattern.compile("\\{pages\\[\\d+\\]\\}").matcher(catalogSelector.url);
                if (matcher.find()) {
                    int offset = Integer.parseInt(matcher.group().substring(7, 8));
                    if (pages.length > offset) {
                        catalogSelector.url = catalogSelector.url.replace(matcher.group(), pages[offset]);
                    }
                }
            }
            if (!MapUtils.isEmpty(areas) && areas.containsKey(Boilerplate.getInstance().getString(R.string.area))) {
                catalogSelector.url = catalogSelector.url.replace("{area}", areas.get(Boilerplate.getInstance().getString(R.string.area)));
            }
            Connection connection = Jsoup.connect(catalogSelector.url);
            if (catalogSelector.headers != null) {
                connection.headers(catalogSelector.headers);
            }
            if (catalogSelector.data != null) {
                connection.data(catalogSelector.data);
            }
            connection.timeout(catalogSelector.timeout == 0 ? JSoupSelector.DEFAULT_TIMEOUT : catalogSelector.timeout);
            Document document = null;
            if (catalogSelector.method == JSoupSelector.METHOD_GET) {
                document = connection.get();
            } else {
                document = connection.post();
            }

            List<JSoupCatalog> catalogs = new ArrayList<>();
            if (catalogSelector.selector != null) {
                Elements typeElements = document.select(catalogSelector.selector);
                for (Element typeElement : typeElements) {
                    JSoupCatalog catalog = new JSoupCatalog();
                    if (catalogSelector.titleSelector != null) {
                        String catalogTitle = null;
                        if (catalogSelector.titleSelector.selector == null) {
                            catalogTitle = catalogSelector.titleSelector.analyzer.analyze(typeElement);
                        } else {
                            catalogTitle = catalogSelector.titleSelector.analyzer.analyze(typeElement.select(catalogSelector.titleSelector.selector));
                        }
                        catalog.title = catalogTitle;
                    }
                    if (catalogSelector.urlSelector != null) {
                        String catalogUrl = null;
                        if (catalogSelector.urlSelector.selector == null) {
                            catalogUrl = catalogSelector.urlSelector.analyzer.analyze(typeElement);
                        } else {
                            catalogUrl = catalogSelector.urlSelector.analyzer.analyze(typeElement.select(catalogSelector.urlSelector.selector));
                        }
                        catalog.url = catalogUrl;
                    }
                    if (catalogSelector.tagSelector != null) {
                        catalog.tags = new ArrayList<>();
                        Elements tagElements = document.select(catalogSelector.tagSelector.selector);
                        for (Element tagElement : tagElements) {
                            JSoupCatalog tag = new JSoupCatalog();
                            if (catalogSelector.tagSelector.titleSelector != null) {
                                String tagTitle = null;
                                if (catalogSelector.tagSelector.titleSelector.selector == null) {
                                    tagTitle = catalogSelector.tagSelector.titleSelector.analyzer.analyze(tagElement);
                                } else {
                                    tagTitle = catalogSelector.tagSelector.titleSelector.analyzer.analyze(tagElement.select(catalogSelector.tagSelector.titleSelector.selector));
                                }
                                tag.title = tagTitle;
                            }
                            if (catalogSelector.tagSelector.urlSelector != null) {
                                String tagUrl = null;
                                if (catalogSelector.tagSelector.urlSelector.selector == null) {
                                    tagUrl = catalogSelector.tagSelector.urlSelector.analyzer.analyze(tagElement);
                                } else {
                                    tagUrl = catalogSelector.tagSelector.urlSelector.analyzer.analyze(tagElement.select(catalogSelector.tagSelector.urlSelector.selector));
                                }
                                tag.url = tagUrl;
                            }
                            catalog.tags.add(tag);
                        }
                    }
                    catalogs.add(catalog);
                }
                subscriber.onNext(catalogs);
            } else if (catalogSelector.tagSelector != null) {
                Elements tagElements = document.select(catalogSelector.tagSelector.selector);
                for (Element tagElement : tagElements) {
                    JSoupCatalog tag = new JSoupCatalog();
                    if (catalogSelector.tagSelector.titleSelector != null) {
                        String tagTitle = null;
                        if (catalogSelector.tagSelector.titleSelector.selector == null) {
                            tagTitle = catalogSelector.tagSelector.titleSelector.analyzer.analyze(tagElement);
                        } else {
                            tagTitle = catalogSelector.tagSelector.titleSelector.analyzer.analyze(tagElement.select(catalogSelector.tagSelector.titleSelector.selector));
                        }
                        tag.title = tagTitle;
                    }
                    if (catalogSelector.tagSelector.urlSelector != null) {
                        String tagUrl = null;
                        if (catalogSelector.tagSelector.urlSelector.selector == null) {
                            tagUrl = catalogSelector.tagSelector.urlSelector.analyzer.analyze(tagElement);
                        } else {
                            tagUrl = catalogSelector.tagSelector.urlSelector.analyzer.analyze(tagElement.select(catalogSelector.tagSelector.urlSelector.selector));
                        }
                        tag.url = tagUrl;
                    }
                    catalogs.add(tag);
                }
                subscriber.onNext(catalogs);
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

    public static class CatalogSelector extends JSoupSelector {
        public JSoupSelector titleSelector;
        public JSoupSelector urlSelector;
        public CatalogSelector tagSelector;
    }
}
