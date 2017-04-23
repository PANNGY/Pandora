package com.github.gnastnosaj.pandora.datasource;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by jasontsang on 4/23/17.
 */

public class Retrofit {
    public final static long DEFAULT_TIMEOUT = 30;

    public static <T> T newSimpleService(@NonNull String baseUrl, @NonNull Class<T> baseService, long timeout) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true).addInterceptor(logging).build();

        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(baseService);
    }

    public static <T> T newSimpleService(@NonNull String baseUrl, @NonNull Class<T> baseService) {
        return newSimpleService(baseUrl, baseService, DEFAULT_TIMEOUT);
    }
}
