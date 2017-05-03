package com.github.gnastnosaj.pandora.datasource;

import android.text.TextUtils;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupCatalog;
import com.shizhefei.mvc.IDataCacheLoader;
import com.shizhefei.mvc.IDataSource;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.trinea.android.common.util.ArrayUtils;
import cn.trinea.android.common.util.MapUtils;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupDataSource implements IDataSource<List<JSoupData>>, IDataCacheLoader<List<JSoupData>> {

    public String baseUrl;
    public String[] pages;
    public Map<String, String> areas;

    public CatalogSelector catalogSelector;
    public DataSelector dataSelector;

    private String currentPage;
    private String nextPage;
    private String previousPage;

    public Observable<List<JSoupCatalog>> loadCatalogs() {
        return Observable.<List<JSoupCatalog>>create(subscriber -> {
            try {
                catalogSelector.url = betterUrl(catalogSelector.url);
                Document document = catalogSelector.loadDocument();

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
                            catalog.url = betterUrl(catalogUrl);
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
                                    tag.url = betterUrl(tagUrl);
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
                            tag.url = betterUrl(tagUrl);
                        }
                        catalogs.add(tag);
                    }
                    subscriber.onNext(catalogs);
                } else {
                    subscriber.onError(new Throwable("both selector and tagSelector is empty"));
                }
            } catch (Exception e) {
                Timber.e(e, "loadCatalogs exception");
                subscriber.onError(e);
            }
            subscriber.onComplete();
        }).subscribeOn(Schedulers.newThread());
    }

    public Observable<List<JSoupData>> loadData() {
        return loadData(dataSelector.url);
    }

    public Observable<List<JSoupData>> loadData(String page) {
        return Observable.<List<JSoupData>>create(subscriber -> {
            try {
                if (page == null) {
                    subscriber.onError(new Throwable("page is empty"));
                } else {
                    currentPage = betterUrl(page);
                    Document document = dataSelector.loadDocument(currentPage);

                    List<JSoupData> data = new ArrayList<>();
                    Elements dataElements = document.select(dataSelector.selector);
                    for (Element dataElement : dataElements) {
                        JSoupData jsoupData = new JSoupData();
                        jsoupData.attrs = new HashMap<>();
                        for (JSoupSelector attrSelector : dataSelector.attrSelectors) {
                            String attr = null;
                            if (attrSelector.selector == null) {
                                attr = attrSelector.analyzer.analyze(dataElement);
                            } else {
                                attr = attrSelector.analyzer.analyze(dataElement.select(attrSelector.selector));
                            }
                            attr = betterUrl(attr);
                            jsoupData.attrs.put(attrSelector.label, attr);
                        }
                        data.add(jsoupData);
                    }

                    if (dataSelector.nextPageSelector != null) {
                        nextPage = dataSelector.nextPageSelector.analyzer.analyze(document.select(dataSelector.nextPageSelector.selector));
                        nextPage = betterUrl(nextPage);
                        Timber.d("nextPage", nextPage);
                    }

                    if (dataSelector.previousPageSelector != null) {
                        previousPage = dataSelector.previousPageSelector.analyzer.analyze(document.select(dataSelector.previousPageSelector.selector));
                        previousPage = betterUrl(previousPage);
                        Timber.d("previousPage", previousPage);
                    }

                    subscriber.onNext(data);
                }
            } catch (Exception e) {
                Timber.e(e, "loadData exception");
                subscriber.onError(e);
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
        return !TextUtils.isEmpty(nextPage);
    }

    public static class CatalogSelector extends JSoupSelector {
        public JSoupSelector titleSelector;
        public JSoupSelector urlSelector;
        public CatalogSelector tagSelector;
    }

    public static class DataSelector extends JSoupSelector {
        public JSoupSelector[] attrSelectors;
        public JSoupSelector nextPageSelector;
        public JSoupSelector previousPageSelector;
    }

    private String betterUrl(String url) {
        if (url != null) {
            if (baseUrl != null) {
                url = url.replace("{baseUrl}", baseUrl);
            }
            if (!ArrayUtils.isEmpty(pages)) {
                Matcher matcher = Pattern.compile("\\{pages\\[\\d+\\]\\}").matcher(url);
                if (matcher.find()) {
                    int offset = Integer.parseInt(matcher.group().substring(7, 8));
                    if (pages.length > offset) {
                        url = url.replace(matcher.group(), pages[offset]);
                    }
                }
            }
            if (!MapUtils.isEmpty(areas) && areas.containsKey(Boilerplate.getInstance().getString(R.string.area))) {
                url = url.replace("{area}", areas.get(Boilerplate.getInstance().getString(R.string.area)));
            }
        }
        return url;
    }
}
