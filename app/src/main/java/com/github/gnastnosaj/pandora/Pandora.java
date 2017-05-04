package com.github.gnastnosaj.pandora;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.github.gnastnosaj.boilerplate.Boilerplate;

import java.io.IOException;
import java.net.SocketException;

import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import timber.log.Timber;

/**
 * Created by jasontsang on 4/21/17.
 */

public class Pandora extends Application {
    public final static String PRE_PRO_VERSION = "PRO_VERSION";

    public static boolean pro;

    @Override
    public void onCreate() {
        super.onCreate();

        Boilerplate.initialize(this);

        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if ((e instanceof IOException) || (e instanceof SocketException)) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
                return;
            }
            if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
                return;
            }
            Timber.w(e, "Undeliverable exception received, not sure what to do");
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        pro = sharedPreferences.getBoolean(PRE_PRO_VERSION, true);
    }
}
