package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.alipay.euler.andfix.util.FileUtil;
import com.bilibili.socialize.share.download.IImageDownloader;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.ui.widget.MosaicView;
import com.github.gnastnosaj.pandora.util.FileTypeUtil;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.trello.rxlifecycle2.android.ActivityEvent;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by Jason on 7/22/2015.
 */
public class MosaicActivity extends BaseActivity {
    public static final String EXTRA_IMAGE_TITLE = "image_title";
    public static final String EXTRA_IMAGE_URL = "image_url";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.mosaic_view)
    MosaicView mosaicView;

    private String imageTitle;
    private String imageUrl;

    private String cache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mosaic);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        imageTitle = getIntent().getStringExtra(EXTRA_IMAGE_TITLE);
        imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);

        setTitle(imageTitle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        try {
            ShareHelper.configuration.getImageDownloader().download(this, imageUrl, ShareHelper.configuration.getImageCachePath(this), new IImageDownloader.OnImageDownloadListener() {
                @Override
                public void onStart() {

                }

                @Override
                public void onSuccess(String filePath) {
                    cache = filePath;
                    mosaicView.initMosaicView(BitmapFactory.decodeFile(filePath));
                    mosaicView.clear();
                    mosaicView.setEffect(MosaicView.Effect.GRID);
                    mosaicView.setMode(MosaicView.Mode.PATH);
                }

                @Override
                public void onFailed(String s) {

                }
            });
        } catch (Exception e) {
            Timber.e(e, "download image exception");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mosaic, menu);
        menu.findItem(R.id.action_save).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_download)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_clear).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_undo)
                .color(Color.WHITE).sizeDp(18));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_save:
                saveMosaicView();
                return true;
            case R.id.action_clear:
                mosaicView.clear();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveMosaicView() {
        Bitmap bmp = mosaicView.getBitmapOutput();
        Observable<String> save;
        if (bmp == null) {
            save = Observable.create(subscriber -> {
                File cache = new File(this.cache);
                String extension = FileTypeUtil.getFileType(this.cache);
                File pic = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), TextUtils.isEmpty(extension) ? cache.getName() : (cache.getName() + "." + extension));
                FileUtil.copyFile(cache, pic);
                subscriber.onNext(pic.getAbsolutePath());
                subscriber.onComplete();
            });
        } else {
            save = Observable.create(subscriber -> {
                File cache = new File(this.cache);
                File pic = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), cache.getName() + ".png");
                FileOutputStream fos = new FileOutputStream(pic);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                subscriber.onNext(pic.getAbsolutePath());
                subscriber.onComplete();
            });
        }
        save.compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filePath -> {
                    Snackbar.make(mosaicView, getResources().getString(R.string.save_picture_success, filePath), Snackbar.LENGTH_SHORT).show();
                    Intent scannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filePath)));
                    sendBroadcast(scannerIntent);
                });
    }
}
