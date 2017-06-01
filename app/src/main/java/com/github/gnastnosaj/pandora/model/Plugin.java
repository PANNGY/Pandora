package com.github.gnastnosaj.pandora.model;

import android.content.Context;
import android.net.Uri;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;
import timber.log.Timber;

/**
 * Created by jasontsang on 12/10/16.
 */

public class Plugin extends RealmObject {
    public final static int TYPE_PYTHON_VIDEO = 0;
    public final static int TYPE_JSOUP_GALLERY = 1;

    @PrimaryKey
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
        if (type == TYPE_PYTHON_VIDEO) {
            File plugins = new File(context.getFilesDir(), "plugins");
            if (!plugins.exists()) {
                plugins.mkdir();
            }
            File python = new File(plugins, "python");
            if (!python.exists()) {
                python.mkdir();
            }
            return new File(python, reference);
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

    @Override
    protected Plugin clone() {
        Plugin plugin = new Plugin();
        plugin.id = id;
        plugin.name = name;
        plugin.desc = desc;
        plugin.type = type;
        plugin.reference = reference;
        plugin.icon = icon;
        plugin.args = args;
        plugin.versionCode = versionCode;
        plugin.versionName = versionName;
        plugin.author = author;
        plugin.license = license;
        plugin.licenseUrl = licenseUrl;
        return plugin;
    }

    public static List<Plugin> from(RealmResults<Plugin> results) {
        List<Plugin> plugins = new ArrayList<>();
        for (Plugin plugin : results) {
            plugins.add(plugin.clone());
        }
        return plugins;
    }
}
