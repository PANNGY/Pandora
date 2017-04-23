package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.model.UpdateData;

import io.reactivex.Observable;
import retrofit2.http.GET;

/**
 * Created by jasontsang on 4/23/17.
 */

public interface GithubService {
    String BASE_URL = "https://github.com/";

    @GET("/jasontsang/Ashley/raw/master/pandora/hosts.json")
    Observable<UpdateData> getUpdateData();
}