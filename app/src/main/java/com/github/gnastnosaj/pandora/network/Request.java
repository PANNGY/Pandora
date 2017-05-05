package com.github.gnastnosaj.pandora.network;

import com.github.gnastnosaj.pandora.datasource.GitOSCService;
import com.github.gnastnosaj.pandora.datasource.GithubService;
import com.github.gnastnosaj.pandora.datasource.Retrofit;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cn.trinea.android.common.util.ListUtils;
import cn.trinea.android.common.util.MapUtils;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import timber.log.Timber;

/**
 * Created by jasontsang on 5/5/17.
 */

public class Request {
    public static CountDownLatch countDownLatch;
    public static List<Decorator> decorators;

    static {
        countDownLatch = new CountDownLatch(1);
        Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class)
                .getRequestConfigs()
                .timeout(5, TimeUnit.SECONDS, Retrofit.newSimpleService(GitOSCService.BASE_URL, GitOSCService.class).getRequestConfigs())
                .subscribeOn(Schedulers.newThread())
                .subscribe(data -> {
                    decorators = data;
                    countDownLatch.countDown();
                }, throwable -> countDownLatch.countDown());
    }

    public static class Decorator {
        public String filter;
        public boolean forceIP;
        public Map<String, String> headers;
    }

    public static class Builder extends okhttp3.Request.Builder {
        @Override
        public okhttp3.Request.Builder url(String url) {
            if (ListUtils.isEmpty(decorators)) {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    Timber.w(e);
                }
            }
            if (!ListUtils.isEmpty(decorators)) {
                for (Decorator decorator : decorators) {
                    try {
                        if (url.contains(decorator.filter)) {
                            if (decorator.forceIP) {
                                String host = HttpUrl.parse(url).host();
                                String address = InetAddress.getByName(host).toString().split("/")[1];
                                url = url.replace(host, address);
                            }
                            if (!MapUtils.isEmpty(decorator.headers)) {
                                headers(Headers.of(decorator.headers));
                            }
                            break;
                        }
                    } catch (Exception e) {
                        Timber.w(e, "url exception");
                    }
                }
            }
            return super.url(url);
        }
    }
}
