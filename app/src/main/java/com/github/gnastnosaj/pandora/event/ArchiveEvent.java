package com.github.gnastnosaj.pandora.event;

import com.github.gnastnosaj.boilerplate.rxbus.RxBus;

import io.reactivex.Observable;
import io.realm.RealmObject;

/**
 * Created by jasontsang on 5/27/17.
 */

public class ArchiveEvent extends RealmObject {
    public final static Observable<ArchiveEvent> observable = RxBus.getInstance().register(ArchiveEvent.class, ArchiveEvent.class);
    public String keyword;
    public String magnet;

    @Override
    public ArchiveEvent clone() {
        ArchiveEvent archiveEvent = new ArchiveEvent();
        archiveEvent.keyword = keyword;
        archiveEvent.magnet = magnet;
        return archiveEvent;
    }
}
