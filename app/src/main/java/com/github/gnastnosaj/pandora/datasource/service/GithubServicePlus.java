package com.github.gnastnosaj.pandora.datasource.service;

import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
import com.github.gnastnosaj.pandora.model.PluginData;
import com.github.gnastnosaj.pandora.model.UpdateData;
import com.github.gnastnosaj.pandora.network.Request;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import retrofit2.http.Path;

/**
 * Created by Jason on 5/30/2017.
 */

public class GithubServicePlus implements GithubService {
    public final static long DEFAULT_TIMEOUT = 5;

    private GithubService githubService;
    private GitOSCService gitOSCService;
    private long timeout;

    public GithubServicePlus(GithubService githubService, GitOSCService gitOSCService, long timeout) {
        this.githubService = githubService;
        this.gitOSCService = gitOSCService;
        this.timeout = timeout;
    }

    public GithubServicePlus(GithubService githubService, GitOSCService gitOSCService) {
        this(githubService, gitOSCService, DEFAULT_TIMEOUT);
    }

    @Override
    public Observable<UpdateData> getUpdateData() {
        return githubService.getUpdateData().timeout(timeout, TimeUnit.SECONDS, gitOSCService.getUpdateData());
    }

    @Override
    public Observable<JSoupDataSource> getJSoupDataSource(@Path("label") String label) {
        return githubService.getJSoupDataSource(label).timeout(timeout, TimeUnit.SECONDS, gitOSCService.getJSoupDataSource(label));
    }

    @Override
    public Observable<PluginData> getPluginData() {
        return githubService.getPluginData().timeout(timeout, TimeUnit.SECONDS, gitOSCService.getPluginData());
    }

    @Override
    public Observable<List<Request.Enhancer>> getRequestConfigs() {
        return githubService.getRequestConfigs().timeout(timeout, TimeUnit.SECONDS, gitOSCService.getRequestConfigs());
    }
}
