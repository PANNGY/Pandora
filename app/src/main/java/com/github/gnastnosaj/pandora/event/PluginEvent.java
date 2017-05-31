package com.github.gnastnosaj.pandora.event;

import com.github.gnastnosaj.boilerplate.rxbus.RxBus;

import io.reactivex.Observable;

/**
 * Created by jasontsang on 5/31/17.
 */

public class PluginEvent {
    public final static Observable<PluginEvent> observable = RxBus.getInstance().register(PluginEvent.class, PluginEvent.class);

    public int type;
}
