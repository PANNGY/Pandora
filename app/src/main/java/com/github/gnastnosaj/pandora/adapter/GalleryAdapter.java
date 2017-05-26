package com.github.gnastnosaj.pandora.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.bilibili.socialize.share.download.IImageDownloader;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.event.ToolbarEvent;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.util.FileTypeUtil;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.shizhefei.mvc.IDataAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.relex.photodraweeview.PhotoDraweeView;
import timber.log.Timber;

/**
 * Created by Jason on 8/21/2016.
 */
public class GalleryAdapter extends PagerAdapter implements IDataAdapter<List<JSoupData>> {

    public static final String TRANSIT_PIC = "Gallery";

    private List<JSoupData> data = new ArrayList<>();
    private Context context;

    public GalleryAdapter(Context context) {
        this.context = context;
    }

    @Override
    public void notifyDataChanged(List<JSoupData> data, boolean isRefresh) {
        if (isRefresh) {
            this.data.clear();
        }
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public List<JSoupData> getData() {
        return data;
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        JSoupData jsoupData = data.get(position);

        PhotoDraweeView draweeView = new PhotoDraweeView(context);
        DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                .setUri(jsoupData.getAttr("thumbnail"))
                .setOldController(draweeView.getController())
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String id, @Nullable ImageInfo imageInfo, @Nullable Animatable anim) {
                        draweeView.update(imageInfo.getWidth(), imageInfo.getHeight());
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {

                    }
                }).build();
        draweeView.setController(draweeController);
        draweeView.setOnViewTapListener((view, v, v1) -> RxBus.getInstance().post(ToolbarEvent.class, new ToolbarEvent()));
        draweeView.setOnLongClickListener((v) -> {
            new AlertDialog.Builder(context)
                    .setMessage(R.string.save_to_phone)
                    .setNegativeButton(R.string.save_cancel, (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(R.string.save_do, (dialog, which) -> {
                        try {
                            ShareHelper.configuration.getImageDownloader().download(context, jsoupData.getAttr("thumbnail"), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), new IImageDownloader.OnImageDownloadListener() {
                                @Override
                                public void onStart() {
                                }

                                @Override
                                public void onSuccess(final String filePath) {
                                    Observable.<String>create(subscriber -> {
                                        String extension = FileTypeUtil.getFileType(filePath);
                                        if (!TextUtils.isEmpty(extension)) {
                                            File origin = new File(filePath);
                                            File now = new File(filePath + "." + extension);
                                            origin.renameTo(now);
                                            subscriber.onNext(now.getAbsolutePath());
                                        } else {
                                            subscriber.onNext(filePath);
                                        }
                                        subscriber.onComplete();
                                    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(newPath -> {
                                        Snackbar.make(draweeView, context.getResources().getString(R.string.save_picture_success, newPath), Snackbar.LENGTH_SHORT).show();
                                        Intent scannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(newPath));
                                        context.sendBroadcast(scannerIntent);
                                    });
                                }

                                @Override
                                public void onFailed(String s) {
                                    Snackbar.make(draweeView, R.string.save_picture_fail, Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            Timber.e(e, "save picture exception");
                        }
                        dialog.dismiss();
                    })
                    .show();

            return true;
        });

        ViewCompat.setTransitionName(draweeView, TRANSIT_PIC);
        container.addView(draweeView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        return draweeView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

}
