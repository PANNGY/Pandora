package com.github.gnastnosaj.pandora.model;

import android.content.Context;
import android.net.Uri;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;

import io.realm.RealmObject;
import timber.log.Timber;

/**
 * Created by jasontsang on 12/10/16.
 */

public class Plugin extends RealmObject {
    public final static int PLUGIN_TYPE_PYTHON_VIDEO = 0;
    public final static int PLUGIN_TYPE_JSOUP_GALLERY = 1;

    public String id;
    public String name;
    public String desc;
    public int type;
    public String reference;
    public String icon;
    public String args;
    public int versionCode;
    public String versionName;
    public String author;
    public String license;
    public String licenseUrl;

    public File getPluginDirectory(Context context) {
        if (type == PLUGIN_TYPE_PYTHON_VIDEO) {
            return new File(context.getFilesDir(), "plugins/python/" + reference);
        } else {
            return null;
        }
    }

    public void icon(Context context, SimpleDraweeView draweeView) {
        try {
            if (icon.startsWith("http")) {
                draweeView.setImageURI(icon);
            } else {
                File file = new File(getPluginDirectory(context), icon);
                draweeView.setImageURI(Uri.fromFile(file));
            }
        } catch (Exception e) {
            Timber.e(e, "plugin icon exception");
        }
    }
}
