package com.github.gnastnosaj.pandora.network;

import com.github.gnastnosaj.pandora.datasource.service.Retrofit;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static List<Enhancer> ehancers;

    static {
        countDownLatch = new CountDownLatch(1);
        Retrofit.newGithubServicePlus()
                .getRequestConfigs()
                .subscribeOn(Schedulers.newThread())
                .subscribe(data -> {
                    ehancers = data;
                    countDownLatch.countDown();
                }, throwable -> countDownLatch.countDown());
    }

    public static class Enhancer {
        public String filterRegexp;
        public boolean replaceWithIP;
        public Map<String, String> headers;
    }

    public static class Builder extends okhttp3.Request.Builder {
        @Override
        public okhttp3.Request.Builder url(String url) {
            if (ListUtils.isEmpty(ehancers)) {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    Timber.w(e);
                }
            }
            if (!ListUtils.isEmpty(ehancers)) {
                for (Enhancer enhancer : ehancers) {
                    try {
                        Pattern pattern = Pattern.compile(enhancer.filterRegexp);
                        Matcher matcher = pattern.matcher(url);
                        if (matcher.find()) {
                            if (enhancer.replaceWithIP) {
                                String host = HttpUrl.parse(url).host();
                                String address = InetAddress.getByName(host).toString().split("/")[1];
                                url = url.replace(host, address);
                            }
                            if (!MapUtils.isEmpty(enhancer.headers)) {
                                headers(Headers.of(enhancer.headers));
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
