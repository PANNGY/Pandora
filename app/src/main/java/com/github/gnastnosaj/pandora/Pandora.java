package com.github.gnastnosaj.pandora;

import android.app.Application;

import com.github.gnastnosaj.boilerplate.Boilerplate;

/**
 * Created by jasontsang on 4/21/17.
 */

public class Pandora extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Boilerplate.initialize(this);
    }
}
