package com.github.gnastnosaj.pandora.datasource.service;

import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.model.PluginData;
import com.github.gnastnosaj.pandora.model.UpdateData;
import com.github.gnastnosaj.pandora.network.Request;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

/**
 * Created by jasontsang on 4/23/17.
 */

public interface GithubService {
    String BASE_URL = "https://raw.githubusercontent.com/";

    String DATE_SOURCE_GIRL_ATLAS_TAB = "girl-atlas-tab";
    String DATE_SOURCE_GIRL_ATLAS_GALLERY = "girl-atlas-gallery";
    String DATE_SOURCE_NANRENCD_TAB = "nanrencd-tab";
    String DATE_SOURCE_NANRENCD_GALLERY = "nanrencd-gallery";
    String DATE_SOURCE_JAVLIB_TAB = "javlib-tab";
    String DATE_SOURCE_JAVLIB_GALLERY = "javlib-gallery";
    String DATE_SOURCE_JAVLIB_MODEL_RANK = "javlib-model-rank";
    String DATE_SOURCE_AVSOX_TAB = "avsox-tab";
    String DATE_SOURCE_AVSOX_GALLERY = "avsox-gallery";
    String DATE_SOURCE_AVSOX_MODEL = "avsox-model";
    String DATE_SOURCE_JIANDANTOP_2016 = "jiandantop-2016";
    String DATE_SOURCE_JIANDANTOP_2017 = "jiandantop-2017";
    String DATE_SOURCE_BTDB = "btdb";
    String DATE_SOURCE_BTCHERRY = "btcherry";
    String DATE_SOURCE_K8DY_HOME = "k8dy-home";
    String DATE_SOURCE_K8DY_TAB = "k8dy-tab";
    String DATE_SOURCE_LEEEBO_SLIDE = "leeebo-slide";
    String DATE_SOURCE_LEEEBO_HOME = "leeebo-home";
    String DATE_SOURCE_LEEEBO_TAB = "leeebo-tab";

    @Headers("Cache-Control: public, max-age=3600")
    @GET("/gnastnosaj/Pandora/master/app/service/update.json")
    Observable<UpdateData> getUpdateData();

    @Headers("Cache-Control: public, max-age=3600")
    @GET("/gnastnosaj/Pandora/master/app/service/datasource/{label}.json")
    Observable<JSoupDataSource> getJSoupDataSource(@Path("label") String label);

    @Headers("Cache-Control: public, max-age=3600")
    @GET("/gnastnosaj/Pandora/master/app/service/plugins/plugins.json")
    Observable<PluginData> getPluginData();

    @Headers("Cache-Control: public, max-age=3600")
    @GET("/gnastnosaj/Pandora/master/app/service/request-enhancer.json")
    Observable<List<Request.Enhancer>> getRequestConfigs();
}
