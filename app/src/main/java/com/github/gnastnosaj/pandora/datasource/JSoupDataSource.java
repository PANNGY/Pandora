package com.github.gnastnosaj.pandora.datasource;

import android.text.TextUtils;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupCatalog;
import com.github.gnastnosaj.pandora.model.JSoupTab;
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

    public TabSelector tabSelector;
    public CatalogSelector catalogSelector;
    public DataSelector dataSelector;

    private String currentPage;
    private String nextPage;

    public Observable<List<JSoupCatalog>> loadCatalogs() {
        return Observable.<List<JSoupCatalog>>create(subscriber -> {
            try {
                catalogSelector.url = betterData(catalogSelector.url);
                Document document = catalogSelector.loadDocument();

                List<JSoupCatalog> catalogs = new ArrayList<>();
                if (catalogSelector.cssQuery != null) {
                    Elements typeElements = catalogSelector.call(document);
                    for (Element typeElement : typeElements) {
                        JSoupCatalog catalog = new JSoupCatalog();
                        if (catalogSelector.titleSelector != null) {
                            String catalogTitle = catalogSelector.titleSelector.parse(document, typeElement);
                            catalog.title = catalogTitle;
                        }
                        if (catalogSelector.urlSelector != null) {
                            String catalogUrl = catalogSelector.urlSelector.parse(document, typeElement);
                            catalog.url = betterData(catalogUrl);
                        }
                        if (catalogSelector.tagSelector != null) {
                            catalog.tags = new ArrayList<>();
                            Elements tagElements = catalogSelector.tagSelector.call(document, typeElement);
                            for (Element tagElement : tagElements) {
                                JSoupCatalog tag = new JSoupCatalog();
                                if (catalogSelector.tagSelector.titleSelector != null) {
                                    String tagTitle = catalogSelector.tagSelector.titleSelector.parse(document, tagElement);
                                    tag.title = tagTitle;
                                }
                                if (catalogSelector.tagSelector.urlSelector != null) {
                                    String tagUrl = catalogSelector.tagSelector.urlSelector.parse(document, tagElement);
                                    tag.url = betterData(tagUrl);
                                }
                                catalog.tags.add(tag);
                            }
                        }
                        catalogs.add(catalog);
                    }
                    subscriber.onNext(catalogs);
                } else if (catalogSelector.tagSelector != null) {
                    Elements tagElements = catalogSelector.tagSelector.call(document);
                    for (Element tagElement : tagElements) {
                        JSoupCatalog tag = new JSoupCatalog();
                        if (catalogSelector.tagSelector.titleSelector != null) {
                            String tagTitle = catalogSelector.tagSelector.titleSelector.parse(document, tagElement);
                            tag.title = tagTitle;
                        }
                        if (catalogSelector.tagSelector.urlSelector != null) {
                            String tagUrl = catalogSelector.tagSelector.urlSelector.parse(document, tagElement);
                            tag.url = betterData(tagUrl);
                        }
                        catalogs.add(tag);
                    }
                    subscriber.onNext(catalogs);
                } else {
                    subscriber.onError(new Throwable("both cssQuery and tagSelector is empty"));
                }
            } catch (Exception e) {
                Timber.e(e, "loadCatalogs exception");
                subscriber.onError(e);
            }
            subscriber.onComplete();
        }).subscribeOn(Schedulers.newThread());
    }

    public Observable<List<JSoupTab>> loadTabs() {
        return Observable.<List<JSoupTab>>create(subscriber -> {
            try {
                tabSelector.url = betterData(tabSelector.url);
                Document document = tabSelector.loadDocument();

                List<JSoupTab> tabs = new ArrayList<>();
                Elements tabElements = tabSelector.call(document);
                for (Element tabElement : tabElements) {
                    JSoupTab tab = new JSoupTab();
                    if (tabSelector.titleSelector != null) {
                        String tabTitle = tabSelector.titleSelector.parse(document, tabElement);
                        tab.title = tabTitle;
                    }
                    if (tabSelector.urlSelector != null) {
                        String tabUrl = tabSelector.urlSelector.parse(document, tabElement);
                        tab.url = betterData(tabUrl);
                    }
                    tabs.add(tab);
                }
                subscriber.onNext(tabs);
            } catch (Exception e) {
                Timber.e(e, "loadTabs exception");
                subscriber.onError(e);
            }
            subscriber.onComplete();
        }).subscribeOn(Schedulers.newThread());
    }

    public Observable<List<JSoupData>> loadData() {
        if (nextPage != null) {
            return loadData(nextPage);
        } else {
            return loadData(dataSelector.url);
        }
    }

    public Observable<List<JSoupData>> loadData(String page) {
        return Observable.<List<JSoupData>>create(subscriber -> {
            try {
                if (page == null) {
                    subscriber.onError(new Throwable("page is empty"));
                } else {
                    currentPage = betterData(page);
                    Document document = dataSelector.loadDocument(currentPage);

                    List<JSoupData> data = new ArrayList<>();
                    Elements dataElements = dataSelector.call(document);
                    for (Element dataElement : dataElements) {
                        JSoupData jsoupData = new JSoupData();
                        jsoupData.attrs = new HashMap<>();
                        for (JSoupSelector attrSelector : dataSelector.attrSelectors) {
                            String attr = attrSelector.parse(document, dataElement);
                            attr = betterData(attr);
                            jsoupData.attrs.put(attrSelector.label, attr);
                        }
                        if (dataSelector.tagSelector != null) {
                            jsoupData.tags = new ArrayList<>();
                            Elements tagElements = dataSelector.tagSelector.call(document, dataElement);
                            for (Element tagElement : tagElements) {
                                JSoupCatalog tag = new JSoupCatalog();
                                if (dataSelector.tagSelector.titleSelector != null) {
                                    String tagTitle = dataSelector.tagSelector.titleSelector.parse(document, tagElement);
                                    tag.title = tagTitle;
                                }
                                if (dataSelector.tagSelector.urlSelector != null) {
                                    String tagUrl = dataSelector.tagSelector.urlSelector.parse(document, tagElement);
                                    tag.url = betterData(tagUrl);
                                }
                                jsoupData.tags.add(tag);
                            }
                        }
                        data.add(jsoupData);
                    }
                    if (data.isEmpty()) {
                        JSoupData jsoupData = new JSoupData();
                        jsoupData.attrs = new HashMap<>();
                        for (JSoupSelector attrSelector : dataSelector.attrSelectors) {
                            if (attrSelector.global) {
                                String attr = attrSelector.parse(document, null);
                                jsoupData.attrs.put(attrSelector.label, attr);
                            }
                        }
                        if (dataSelector.tagSelector != null && dataSelector.tagSelector.global) {
                            jsoupData.tags = new ArrayList<>();
                            Elements tagElements = dataSelector.tagSelector.call(document, null);
                            for (Element tagElement : tagElements) {
                                JSoupCatalog tag = new JSoupCatalog();
                                if (dataSelector.tagSelector.titleSelector != null) {
                                    String tagTitle = dataSelector.tagSelector.titleSelector.parse(document, tagElement);
                                    tag.title = tagTitle;
                                }
                                if (dataSelector.tagSelector.urlSelector != null) {
                                    String tagUrl = dataSelector.tagSelector.urlSelector.parse(document, tagElement);
                                    tag.url = betterData(tagUrl);
                                }
                                jsoupData.tags.add(tag);
                            }
                        }
                        data.add(jsoupData);
                    }

                    if (dataSelector.nextPageSelector != null) {
                        nextPage = dataSelector.nextPageSelector.parse(document, document);
                        nextPage = betterData(nextPage);
                        Timber.d("nextPage", nextPage);
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

    public void setNextPage(String nextPage) {
        this.nextPage = nextPage;
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

    public static class TabSelector extends JSoupSelector {
        public JSoupSelector titleSelector;
        public JSoupSelector urlSelector;
    }

    public static class CatalogSelector extends TabSelector {
        public CatalogSelector tagSelector;
    }

    public static class DataSelector extends JSoupSelector {
        public JSoupSelector[] attrSelectors;
        public JSoupSelector nextPageSelector;
        public CatalogSelector tagSelector;
    }

    private String betterData(String data) {
        if (data != null) {
            data = data.trim();
            if (baseUrl != null) {
                data = data.replace("{baseUrl}", baseUrl);
            }
            if (!ArrayUtils.isEmpty(pages)) {
                Matcher matcher = Pattern.compile("\\{pages\\[\\d+\\]\\}").matcher(data);
                if (matcher.find()) {
                    int offset = Integer.parseInt(matcher.group().substring(7, 8));
                    if (pages.length > offset) {
                        data = data.replace(matcher.group(), pages[offset]);
                    }
                }
            }
            if (!MapUtils.isEmpty(areas) && areas.containsKey(Boilerplate.getInstance().getString(R.string.area))) {
                data = data.replace("{area}", areas.get(Boilerplate.getInstance().getString(R.string.area)));
            }
            data = data.replace("/./", "/");
        }
        return data;
    }
}
