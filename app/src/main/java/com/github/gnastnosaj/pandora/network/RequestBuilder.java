package com.github.gnastnosaj.pandora.network;

import com.github.gnastnosaj.pandora.datasource.GithubService;
import com.github.gnastnosaj.pandora.datasource.Retrofit;
import com.github.gnastnosaj.pandora.model.RequestConfig;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import cn.trinea.android.common.util.ListUtils;
import cn.trinea.android.common.util.MapUtils;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import timber.log.Timber;

/**
 * Created by jasontsang on 5/4/17.
 */

public class RequestBuilder extends Request.Builder {
    public static CountDownLatch countDownLatch;
    public static List<RequestConfig> requestConfigs;

    static {
        countDownLatch = new CountDownLatch(1);
        Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class)
                .getRequestConfigs().subscribeOn(Schedulers.newThread()).subscribe(configs -> {
            RequestBuilder.requestConfigs = configs;
            countDownLatch.countDown();
        }, throwable -> {
            countDownLatch.countDown();
        });
    }

    @Override
    public Request.Builder url(String url) {
        if (ListUtils.isEmpty(requestConfigs)) {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Timber.w(e);
            }
        }
        if (ListUtils.isEmpty(requestConfigs)) {
            for (RequestConfig requestConfig : requestConfigs) {
                try {
                    if (url.contains(requestConfig.host)) {
                        if (requestConfig.forceIP) {
                            String host = HttpUrl.parse(url).host();
                            String address = InetAddress.getByName(host).toString().split("/")[1];
                            url = url.replace(host, address);
                        }
                        if (!MapUtils.isEmpty(requestConfig.headers)) {
                            headers(Headers.of(requestConfig.headers));
                        }
                        break;
                    }
                } catch (Exception e) {
                    Timber.w(e, "Request Builder url exception");
                }
            }
        }
        return super.url(url);
    }
}
