package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.Pandora;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.GankService;
import com.github.gnastnosaj.pandora.datasource.GithubService;
import com.github.gnastnosaj.pandora.datasource.Retrofit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by jasontsang on 4/21/17.
 */

public class SplashActivity extends BaseActivity {
    public final static String PRE_SPLASH_IMAGE = "SPLASH_IMAGE";

    @BindView(R.id.splash_image)
    SimpleDraweeView splashImage;

    @BindView(R.id.splash_slogan)
    TextView splashSlogan;

    @BindView(R.id.splash_version)
    TextView splashVersion;

    @BindView(R.id.splash_copyright)
    TextView splashCopyright;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        initSystemBar();

        Single<String> splashImageSingle;
        if (Pandora.pro) {
            GithubService githubService = Retrofit.newSimpleService(GithubService.BASE_URL, GithubService.class);
            splashImageSingle = githubService.getDataSource(GithubService.DATE_SOURCE_JAVLIB_TAB)
                    .flatMap(jsoupDataSource -> jsoupDataSource.loadData())
                    .map(data -> data.get(new Random().nextInt(data.size() - 1)).attrs.get("url"))
                    .flatMap(url -> githubService.getDataSource(GithubService.DATE_SOURCE_JAVLIB_GALLERY).flatMap(jsoupDataSource -> jsoupDataSource.loadData(url)))
                    .flatMap(data -> Observable.fromIterable(data))
                    .lastOrError()
                    .map(data -> data.attrs.get("cover"));
        } else {
            splashImageSingle = Retrofit.newSimpleService(GankService.BASE_URL, GankService.class)
                    .getGankData("福利", 1, 1)
                    .flatMap(gankData -> Observable.fromIterable(gankData.results))
                    .lastOrError()
                    .map(result -> result.url);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        splashImageSingle
                .timeout(5, TimeUnit.SECONDS, Single.create(subscriber -> {
                    if (sharedPreferences.contains(PRE_SPLASH_IMAGE)) {
                        subscriber.onSuccess(sharedPreferences.getString(PRE_SPLASH_IMAGE, null));
                    } else {
                        subscriber.onError(new TimeoutException());
                    }
                }))
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uriString -> {
                    DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                            .setUri(uriString)
                            .setOldController(splashImage.getController())
                            .setControllerListener(new BaseControllerListener<ImageInfo>() {
                                @Override
                                public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable anim) {
                                    Animation animation = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.splash);
                                    animation.setAnimationListener(new Animation.AnimationListener() {
                                        @Override
                                        public void onAnimationStart(Animation animation) {
                                        }

                                        @Override
                                        public void onAnimationEnd(Animation animation) {
                                            start();
                                        }

                                        @Override
                                        public void onAnimationRepeat(Animation animation) {
                                        }
                                    });
                                    splashImage.startAnimation(animation);
                                }

                                @Override
                                public void onFailure(String id, Throwable throwable) {
                                    start();
                                }
                            }).build();
                    splashImage.setController(draweeController);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(PRE_SPLASH_IMAGE, uriString);
                    editor.apply();
                }, throwable -> {
                    Timber.w(throwable, "load splash image exception");
                    start();
                });

        splashVersion.setText(getResources().getString(R.string.splash_version, Boilerplate.versionName));
        splashCopyright.setText(getResources().getString(R.string.splash_copyright, new SimpleDateFormat("yyyy").format(new Date())));
    }

    private void start() {
        startActivity(new Intent(this, PandoraActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
