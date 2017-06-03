package com.github.gnastnosaj.pandora.event;

/**
 * Created by jasontsang on 1/3/17.
 */

public class VideoEvent {
    public final static int TYPE_ON_AUTO_COMPLETION = 0;
    public final static int TYPE_ON_ERROR = 1;
    public final static int TYPE_ON_FULLSCREEN = 2;

    public int type;

    public VideoEvent(int type) {
        this.type = type;
    }
}
