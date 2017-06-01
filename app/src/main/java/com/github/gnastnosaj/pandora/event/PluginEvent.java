package com.github.gnastnosaj.pandora.event;

import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.pandora.model.Plugin;

import io.reactivex.Observable;

/**
 * Created by jasontsang on 5/31/17.
 */

public class PluginEvent {
    public final static Observable<PluginEvent> observable = RxBus.getInstance().register(PluginEvent.class, PluginEvent.class);

    public final static int TYPE_MANAGE = 0;
    public final static int TYPE_COMPLETE = 1;
    public final static int TYPE_REFRESH = 2;
    public final static int TYPE_UPDATE = 3;

    public int type;
    public Plugin plugin;

    public PluginEvent(int type, Plugin plugin) {
        this.type = type;
        this.plugin = plugin;
    }
}
