package com.github.gnastnosaj.pandora.datasource.service;

import android.support.annotation.NonNull;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.pandora.R;

import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
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

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .cache(new Cache(Boilerplate.getInstance().getCacheDir(), 1024 * 1024 * 128))
                .retryOnConnectionFailure(true);

        if (Boilerplate.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }

        OkHttpClient okHttpClient = builder.build();

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

    public static GithubService newGithubService(long timeout) {
        if (Boilerplate.getInstance().getResources().getString(R.string.area).equals("cn")) {
            return newSimpleService(GitOSCService.BASE_URL, GitOSCService.class, timeout);
        } else {
            return newSimpleService(GithubService.BASE_URL, GithubService.class, timeout);
        }
    }

    public static GithubService newGithubService() {
        return newGithubService(DEFAULT_TIMEOUT);
    }

    public static GithubService newGithubServicePlus(long timeout) {
        return new GithubServicePlus(newSimpleService(GithubService.BASE_URL, GithubService.class, timeout), newSimpleService(GitOSCService.BASE_URL, GitOSCService.class, timeout));
    }

    public static GithubService newGithubServicePlus() {
        return newGithubServicePlus(DEFAULT_TIMEOUT);
    }
}
