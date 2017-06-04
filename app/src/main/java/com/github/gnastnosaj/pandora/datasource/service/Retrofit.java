package com.github.gnastnosaj.pandora.datasource.service;

import android.support.annotation.NonNull;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.model.PluginData;
import com.github.gnastnosaj.pandora.model.UpdateData;
import com.github.gnastnosaj.pandora.network.Request;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Path;

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

    public static GithubService newGithubServicePlus() {
        return new GithubServicePlus();
    }

    private final static class GithubServicePlus implements GithubService {
        final static int TYPE_GITHUB = 0;
        final static int TYPE_GITOSC = 1;

        private int type;
        private GithubService githubService;
        private GitOSCService gitOSCService;

        private GithubServicePlus() {
            if (Boilerplate.getInstance().getResources().getString(R.string.area).equals("cn")) {
                type = TYPE_GITOSC;
                gitOSCService = Retrofit.newSimpleService(GitOSCService.BASE_URL, GitOSCService.class);
            } else {
                type = TYPE_GITHUB;
                githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);
            }
        }

        @Override
        public Observable<UpdateData> getUpdateData() {
            if (type == TYPE_GITHUB) {
                return githubService.getUpdateData();
            } else {
                return gitOSCService.getUpdateData();
            }
        }

        @Override
        public Observable<JSoupDataSource> getJSoupDataSource(@Path("label") String label) {
            if (type == TYPE_GITHUB) {
                return githubService.getJSoupDataSource(label);
            } else {
                return gitOSCService.getJSoupDataSource(label);
            }
        }

        @Override
        public Observable<PluginData> getPluginData() {
            if (type == TYPE_GITHUB) {
                return githubService.getPluginData();
            } else {
                return gitOSCService.getPluginData();
            }
        }

        @Override
        public Observable<List<Request.Enhancer>> getRequestConfigs() {
            if (type == TYPE_GITHUB) {
                return githubService.getRequestConfigs();
            } else {
                return gitOSCService.getRequestConfigs();
            }
        }
    }
}
