package com.github.gnastnosaj.pandora.datasource;

import android.text.TextUtils;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.model.JSoupAttr;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupCatalog;
import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.shizhefei.mvc.IDataCacheLoader;
import com.shizhefei.mvc.IDataSource;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.trinea.android.common.util.ArrayUtils;
import cn.trinea.android.common.util.ListUtils;
import cn.trinea.android.common.util.MapUtils;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.realm.RealmList;
import timber.log.Timber;

/**
 * Created by jasontsang on 5/2/17.
 */

public class JSoupDataSource implements IDataSource<List<JSoupData>>, IDataCacheLoader<List<JSoupData>> {

    public String id;
    public String baseUrl;
    public String[] pages;
    public Map<String, String> areas;

    public TabSelector tabSelector;
    public CatalogSelector catalogSelector;
    public DataSelector dataSelector;

    private List<String> history = new ArrayList<>();
    private String currentPage;
    private String nextPage;

    public Observable<List<JSoupLink>> loadTabs() {
        return Observable.create(subscriber -> {
            try {
                tabSelector.url = betterData(tabSelector.url);
                Document document = tabSelector.loadDocument();

                List<JSoupLink> tabs = new ArrayList<>();
                Elements tabElements = tabSelector.call(document);
                for (Element tabElement : tabElements) {
                    JSoupLink tab = new JSoupLink();
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
                Timber.w(e, "loadTabs exception");
                subscriber.onError(e);
            }
            subscriber.onComplete();
        });
    }

    public Observable loadCatalogs() {
        return Observable.create(subscriber -> {
            try {
                catalogSelector.url = betterData(catalogSelector.url);
                Document document = catalogSelector.loadDocument();


                if (!TextUtils.isEmpty(catalogSelector.cssQuery)) {
                    List<JSoupCatalog> catalogs = new ArrayList<>();
                    Elements typeElements = catalogSelector.call(document);
                    for (Element typeElement : typeElements) {
                        JSoupCatalog catalog = new JSoupCatalog();
                        catalog.link = new JSoupLink();
                        if (catalogSelector.titleSelector != null) {
                            String catalogTitle = catalogSelector.titleSelector.parse(document, typeElement);
                            catalog.link.title = catalogTitle;
                        }
                        if (catalogSelector.urlSelector != null) {
                            String catalogUrl = catalogSelector.urlSelector.parse(document, typeElement);
                            catalog.link.url = betterData(catalogUrl);
                        }
                        if (catalogSelector.tagSelector != null) {
                            catalog.tags = new RealmList<>();
                            Elements tagElements = catalogSelector.tagSelector.call(document, typeElement);
                            for (Element tagElement : tagElements) {
                                JSoupLink tag = new JSoupLink();
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
                    List<JSoupLink> catalogs = new ArrayList<>();
                    Elements tagElements = catalogSelector.tagSelector.call(document);
                    for (Element tagElement : tagElements) {
                        JSoupLink tag = new JSoupLink();
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
                Timber.w(e, "loadCatalogs exception");
                subscriber.onError(e);
            }
            subscriber.onComplete();
        });
    }

    public Observable<List<JSoupData>> loadData() {
        return Observable.create(subscriber -> {
            List<JSoupData> data = new ArrayList<>();
            try {
                if (nextPage != null) {
                    data.addAll(_loadData(nextPage));
                } else {
                    data.addAll(_loadData(dataSelector.url));
                }
                subscriber.onNext(data);
            } catch (Exception e) {
                Timber.e(e, "loadData exception");
                subscriber.onError(e);
            }
            subscriber.onComplete();
        });
    }

    public Observable<List<JSoupData>> loadData(String page) {
        nextPage = page;
        return loadData();
    }

    public List<JSoupData> _loadData(String page) throws Exception {
        if (page == null) {
            throw new Exception("page is empty");
        } else {
            if (history.contains(page)) {
                throw new Exception("page is loaded");
            }
            history.add(page);
            currentPage = betterData(page);
            Document document = dataSelector.loadDocument(currentPage);

            List<JSoupData> data = new ArrayList<>();

            JSoupData globalData = new JSoupData();
            globalData.attrs = new RealmList<>();
            for (JSoupSelector attrSelector : dataSelector.attrSelectors) {
                if (attrSelector.global) {
                    String attrContent = attrSelector.parse(document, null);
                    globalData.attrs.add(new JSoupAttr(attrSelector.label, attrContent));
                }
            }
            for (JSoupSelector attrSelector : dataSelector.attrSelectors) {
                if (!TextUtils.isEmpty(attrSelector.placeholder)) {
                    if (!data.contains(globalData)) {
                        data.add(globalData);
                    }
                    for (JSoupAttr attr : globalData.attrs) {
                        if (attr.label.equals(attrSelector.placeholder)) {
                            globalData.attrs.add(new JSoupAttr(attrSelector.label, attr.content));
                            break;
                        }
                    }
                }
            }
            if (dataSelector.tagSelector != null && dataSelector.tagSelector.global) {
                globalData.tags = new RealmList<>();
                Elements tagElements = dataSelector.tagSelector.call(document, null);
                for (Element tagElement : tagElements) {
                    JSoupLink tag = new JSoupLink();
                    if (dataSelector.tagSelector.titleSelector != null) {
                        String tagTitle = dataSelector.tagSelector.titleSelector.parse(document, tagElement);
                        tag.title = tagTitle;
                    }
                    if (dataSelector.tagSelector.urlSelector != null) {
                        String tagUrl = dataSelector.tagSelector.urlSelector.parse(document, tagElement);
                        tag.url = betterData(tagUrl);
                    }
                    globalData.tags.add(tag);
                }
            }

            Elements dataElements = dataSelector.call(document);
            for (Element dataElement : dataElements) {
                JSoupData jsoupData = new JSoupData();
                jsoupData.attrs = new RealmList<>();
                for (JSoupSelector attrSelector : dataSelector.attrSelectors) {
                    if (!attrSelector.global) {
                        String attrContent = attrSelector.parse(document, dataElement);
                        attrContent = betterData(attrContent);
                        jsoupData.attrs.add(new JSoupAttr(attrSelector.label, attrContent));
                    }
                }
                jsoupData.tags = new RealmList<>();
                if (dataSelector.tagSelector != null && !dataSelector.tagSelector.global) {
                    Elements tagElements = dataSelector.tagSelector.call(document, dataElement);
                    for (Element tagElement : tagElements) {
                        JSoupLink tag = new JSoupLink();
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
                if (!ListUtils.isEmpty(globalData.attrs)) {
                    jsoupData.attrs.addAll(globalData.attrs);
                }
                if (!ListUtils.isEmpty(globalData.tags)) {
                    jsoupData.tags.addAll(globalData.tags);
                }
                data.add(jsoupData);
            }

            if (dataSelector.nextPageSelector != null) {
                nextPage = dataSelector.nextPageSelector.parse(document);
                nextPage = betterData(nextPage);
                Timber.w("next page: %s", nextPage);

                if (dataSelector.nextPageSelector.autoLoad) {
                    try {
                        data.addAll(_loadData(nextPage));
                    } catch (Exception e) {
                        Timber.w(e, "loadData exception");
                    }
                }
            }

            return data;
        }
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
        history.clear();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        List<JSoupData> data = new ArrayList<>();
        loadData().subscribeOn(Schedulers.newThread()).subscribe(jsoupData -> {
            data.addAll(jsoupData);
            countDownLatch.countDown();
        }, throwable -> countDownLatch.countDown());
        countDownLatch.await();
        return data;
    }

    @Override
    public List<JSoupData> loadMore() throws Exception {
        return refresh();
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
            if (data.startsWith("javascript")) {
                return null;
            }
            if (!TextUtils.isEmpty(baseUrl)) {
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
