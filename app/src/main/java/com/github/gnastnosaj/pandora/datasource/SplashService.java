package com.github.gnastnosaj.pandora.datasource;

import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Created by jasontsang on 5/16/17.
 */

public class SplashService {
    public final static String PRE_SPLASH_IMAGE = "SPLASH_IMAGE";
    public final static String PRE_SPLASH_IMAGE_DATA_SOURCE = "SPLASH_IMAGE_DATA_SOURCE";

    public final static int SPLASH_IMAGE_DATA_SOURCE_GANK = 0;
    public final static int SPLASH_IMAGE_DATA_SOURCE_GIRL_ATLAS = 1;
    public final static int SPLASH_IMAGE_DATA_SOURCE_NANRENCD = 2;
    public final static int SPLASH_IMAGE_DATA_SOURCE_JAVLIB = 3;

    public static Single<String> gankSingle() {
        return Retrofit.newSimpleService(GankService.BASE_URL, GankService.class)
                .getGankData("福利", 1, 1)
                .flatMap(gankData -> Observable.fromIterable(gankData.results))
                .firstOrError()
                .map(result -> result.url);
    }

    public static Single<String> girlAtlasSingle() {
        GithubService girlAtlasService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);
        return girlAtlasService.getJSoupDataSource(GithubService.DATE_SOURCE_GIRL_ATLAS_TAB)
                .flatMap(jsoupDataSource -> jsoupDataSource.loadData())
                .map(data -> data.get(new Random().nextInt(data.size() - 1)).getAttr("url"))
                .flatMap(url -> girlAtlasService.getJSoupDataSource(GithubService.DATE_SOURCE_GIRL_ATLAS_GALLERY).flatMap(jsoupDataSource -> jsoupDataSource.loadData(url)))
                .map(data -> data.get(new Random().nextInt(data.size() - 1)).getAttr("thumbnail"))
                .singleOrError()
                .onErrorResumeNext(gankSingle());
    }

    public static Single<String> nanrencdSingle() {
        GithubService nanrencdService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);
        return nanrencdService.getJSoupDataSource(GithubService.DATE_SOURCE_NANRENCD_TAB)
                .flatMap(jsoupDataSource -> jsoupDataSource.loadData())
                .map(data -> data.get(new Random().nextInt(data.size() - 1)).getAttr("url"))
                .flatMap(url ->
                        nanrencdService.getJSoupDataSource(GithubService.DATE_SOURCE_NANRENCD_GALLERY)
                                .flatMap(jsoupDataSource -> jsoupDataSource.loadData(url)
                                        .flatMap(data -> {
                                            int pageTotal = Integer.parseInt(data.get(0).getAttr("page-total"));
                                            String nextpage = url + "/" + new Random().nextInt(pageTotal);
                                            return jsoupDataSource.loadData(nextpage);
                                        }))
                )
                .map(data -> data.get(data.size() > 1 ? new Random().nextInt(data.size() - 1) : 0).getAttr("thumbnail"))
                .singleOrError()
                .onErrorResumeNext(girlAtlasSingle());
    }

    public static Single<String> javlibSingle() {
        GithubService javlibService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);
        return javlibService.getJSoupDataSource(GithubService.DATE_SOURCE_JAVLIB_TAB)
                .flatMap(jsoupDataSource -> jsoupDataSource.loadData())
                .map(data -> data.get(new Random().nextInt(data.size() - 1)).getAttr("url"))
                .flatMap(url -> javlibService.getJSoupDataSource(GithubService.DATE_SOURCE_JAVLIB_GALLERY).flatMap(jsoupDataSource -> jsoupDataSource.loadData(url)))
                .flatMap(data -> Observable.fromIterable(data))
                .firstOrError()
                .map(data -> data.getAttr("cover"))
                .onErrorResumeNext(nanrencdSingle());
    }
}
