package com.github.gnastnosaj.pandora.datasource;

import com.github.gnastnosaj.pandora.model.GankData;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by jasontsang on 10/18/16.
 */

public interface GankService {
    public final static String BASE_URL = "http://gank.io/";

    @GET("/api/data/{type}/{size}/{page}")
    Observable<GankData> getGankData(@Path("type") String type, @Path("size") int size, @Path("page") int page);

    @GET("/api/day/{year}/{month}/{day}")
    Observable<GankData> getGankData(@Path("year") String year, @Path("month") String month, @Path("day") String day);

    @GET("/api/random/data/{type}/{size}")
    Observable<GankData> getGankData(@Path("type") String type, @Path("size") int size);
}