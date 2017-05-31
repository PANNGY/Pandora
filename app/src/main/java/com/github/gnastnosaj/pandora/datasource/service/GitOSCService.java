package com.github.gnastnosaj.pandora.datasource.service;

import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.model.Plugin;
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

public interface GitOSCService extends GithubService{
    String BASE_URL = "https://git.oschina.net/";

    @Headers("Cache-Control: public, max-age=3600")
    @GET("/jasontsang/Pandora/raw/master/app/service/update.json")
    Observable<UpdateData> getUpdateData();

    @Headers("Cache-Control: public, max-age=3600")
    @GET("/jasontsang/Pandora/raw/master/app/service/datasource/{label}.json")
    Observable<JSoupDataSource> getJSoupDataSource(@Path("label") String label);

    @Headers("Cache-Control: public, max-age=3600")
    @GET("/jasontsang/Pandora/raw/master/app/service/plugins/plugins.json")
    Observable<List<Plugin>> getPlugins();

    @Headers("Cache-Control: public, max-age=3600")
    @GET("/jasontsang/Pandora/raw/master/app/service/request-enhancer.json")
    Observable<List<Request.Enhancer>> getRequestConfigs();
}