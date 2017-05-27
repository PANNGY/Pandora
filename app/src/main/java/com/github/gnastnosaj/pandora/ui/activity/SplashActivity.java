package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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
import com.github.gnastnosaj.pandora.datasource.service.GithubService;
import com.github.gnastnosaj.pandora.datasource.service.SplashService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by jasontsang on 4/21/17.
 */

public class SplashActivity extends BaseActivity {
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String uriString = sharedPreferences.getString(SplashService.PRE_SPLASH_IMAGE, null);

        if (!TextUtils.isEmpty(uriString)) {
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
                            Timber.w(throwable, "load splash image exception");
                            start();
                        }
                    }).build();
            splashImage.setController(draweeController);
        } else {
            Observable.timer(3, TimeUnit.SECONDS).subscribeOn(Schedulers.computation()).subscribe(aLong -> start());
        }

        splashVersion.setText(getResources().getString(R.string.splash_version, Boilerplate.versionName));
        splashCopyright.setText(getResources().getString(R.string.splash_copyright, new SimpleDateFormat("yyyy").format(new Date())));
    }

    private void start() {
        startActivity(new Intent(this, SimpleViewPagerActivity.class).putExtra(SimpleViewPagerActivity.EXTRA_DATASOURCE, GithubService.DATE_SOURCE_JAVLIB_TAB).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
