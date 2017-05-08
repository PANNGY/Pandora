package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.model.UpdateData;
import com.github.gnastnosaj.pandora.network.Request;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
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
    String DATE_SOURCE_AVSOX_TAB = "avsox-tab";
    String DATE_SOURCE_AVSOX_GALLERY = "avsox-gallery";

    @GET("/gnastnosaj/Pandora/master/app/service/update.json")
    Observable<UpdateData> getUpdateData();

    @GET("/gnastnosaj/Pandora/master/app/service/datasource/{label}.json")
    Observable<JSoupDataSource> getJSoupDataSource(@Path("label") String label);

    @GET("/gnastnosaj/Pandora/master/app/service/datasource/include.json")
    Observable<List<String>> getDataSources();

    @GET("/gnastnosaj/Pandora/master/app/service/request-decoration.json")
    Observable<List<Request.Decorator>> getRequestConfigs();

}
