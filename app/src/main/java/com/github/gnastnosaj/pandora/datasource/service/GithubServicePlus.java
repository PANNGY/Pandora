package com.github.gnastnosaj.pandora.datasource.service;

import com.github.gnastnosaj.pandora.datasource.jsoup.JSoupDataSource;
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
    private int timeout = 5;
    private GithubService githubService;
    private GitOSCService gitOSCService;

    public GithubServicePlus(int timeout, GithubService githubService, GitOSCService gitOSCService) {
        this.timeout = timeout;
        this.githubService = githubService;
        this.gitOSCService = gitOSCService;
    }

    public GithubServicePlus(GithubService githubService, GitOSCService gitOSCService) {
        this.githubService = githubService;
        this.gitOSCService = gitOSCService;
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
    public Observable<List<String>> getDataSources() {
        return githubService.getDataSources().timeout(timeout, TimeUnit.SECONDS, gitOSCService.getDataSources());
    }

    @Override
    public Observable<List<Request.Enhancer>> getRequestConfigs() {
        return githubService.getRequestConfigs().timeout(timeout, TimeUnit.SECONDS, gitOSCService.getRequestConfigs());
    }
}
