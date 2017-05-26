package com.github.gnastnosaj.pandora.event;

import com.github.gnastnosaj.boilerplate.rxbus.RxBus;

import io.reactivex.Observable;

/**
 * Created by jasontsang on 5/26/17.
 */

public class ToolbarEvent {
    public final static Observable<ToolbarEvent> observable = RxBus.getInstance().register(ToolbarEvent.class, ToolbarEvent.class);
}
