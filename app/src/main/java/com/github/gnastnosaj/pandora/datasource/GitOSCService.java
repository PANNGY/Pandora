package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.model.RequestConfig;
import com.github.gnastnosaj.pandora.model.UpdateData;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by jasontsang on 4/23/17.
 */

public interface GitOSCService {
    String BASE_URL = "https://git.oschina.net/";

    @GET("/jasontsang/Pandora/raw/master/app/service/update.json")
    Observable<UpdateData> getUpdateData();

    @GET("/jasontsang/Pandora/raw/master/app/service/datasource/{label}.json")
    Observable<JSoupDataSource> getDataSource(@Path("label") String label);

    @GET("/jasontsang/Pandora/raw/master/app/service/datasource/include.json")
    Observable<List<String>> getDataSources();

    @GET("/jasontsang/Pandora/raw/master/app/service/request-config.json")
    Observable<List<RequestConfig>> getRequestConfigs();
}