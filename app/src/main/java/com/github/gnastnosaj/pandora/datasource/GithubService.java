package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.model.UpdateData;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by jasontsang on 4/23/17.
 */

public interface GithubService {
    String BASE_URL = "https://raw.githubusercontent.com/";
    String DATA_SOURCE_LABEL_JAVLIB = "javlib";

    @GET("/gnastnosaj/Pandora/master/app/service/update.json")
    Observable<UpdateData> getUpdateData();

    @GET("/gnastnosaj/Pandora/master/app/service/datasource/{label}.json")
    Observable<JSoupDataSource> getDataSource(@Path("label") String label);
}
