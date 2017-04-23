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
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.GankService;
import com.github.gnastnosaj.pandora.datasource.Retrofit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by jasontsang on 4/21/17.
 */

public class SplashActivity extends BaseActivity {
    public final static String PRE_SPLASH_IMAGE = "SPLASH_IMAGE";

    private SimpleDraweeView splashImage;
    private TextView splashSlogan;
    private TextView splashVersion;
    private TextView splashCopyright;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initSystemBar();

        splashImage = (SimpleDraweeView) findViewById(R.id.splash_image);
        splashSlogan = (TextView) findViewById(R.id.splash_slogan);
        splashVersion = (TextView) findViewById(R.id.splash_version);
        splashCopyright = (TextView) findViewById(R.id.splash_copyright);

        Retrofit.newSimpleService(GankService.BASE_URL, GankService.class)
                .getGankData("福利", 1, 1)
                .map(gankData -> gankData.results.get(0).url)
                .timeout(3, TimeUnit.SECONDS, Observable.create(subscriber -> {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                    if (sharedPreferences.contains(PRE_SPLASH_IMAGE)) {
                        subscriber.onNext(sharedPreferences.getString(PRE_SPLASH_IMAGE, null));
                    } else {
                        subscriber.onError(new TimeoutException());
                    }
                    subscriber.onComplete();
                }))
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uriString -> {
                    DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                            .setUri(uriString)
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
                                            startActivity(new Intent(SplashActivity.this, PandoraActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                                        }

                                        @Override
                                        public void onAnimationRepeat(Animation animation) {
                                        }
                                    });
                                    splashImage.startAnimation(animation);
                                }

                                @Override
                                public void onFailure(String id, Throwable throwable) {
                                    startActivity(new Intent(SplashActivity.this, PandoraActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                                }
                            }).build();
                    splashImage.setController(draweeController);

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(PRE_SPLASH_IMAGE, uriString);
                    editor.apply();
                }, throwable -> startActivity(new Intent(this, PandoraActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)));

        splashVersion.setText(getResources().getString(R.string.splash_version, Boilerplate.versionName));
        splashCopyright.setText(getResources().getString(R.string.splash_copyright, new SimpleDateFormat("yyyy").format(new Date())));
    }
}
