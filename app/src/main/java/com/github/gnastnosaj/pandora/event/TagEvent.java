package com.github.gnastnosaj.pandora.event;

import com.github.gnastnosaj.pandora.model.JSoupLink;

import java.util.List;

/**
 * Created by jasontsang on 5/26/17.
 */

public class TagEvent {
    public List<JSoupLink> tags;

    public TagEvent(List<JSoupLink> tags) {
        this.tags = tags;
    }
}
