package com.github.gnastnosaj.pandora.ui.widget;

import com.bilibili.socialize.share.download.AbsImageDownloader;
import com.bilibili.socialize.share.util.FileUtil;
import com.facebook.binaryresource.BinaryResource;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

/**
 * Created by jasontsang on 5/25/17.
 */

public class ShareFrescoImageDownloader extends AbsImageDownloader {

    @Override
    protected void downloadDirectly(final String imageUrl, final String filePath, final OnImageDownloadListener listener) {
        if (listener != null)
            listener.onStart();

        final ImageRequest request = ImageRequest.fromUri(imageUrl);
        DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(request, null);

        dataSource.subscribe(new BaseDataSubscriber<CloseableReference<CloseableImage>>() {

            @Override
            protected void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                if (!dataSource.isFinished()) {
                    return;
                }
                CloseableReference<CloseableImage> ref = dataSource.getResult();
                if (ref != null) {
                    try {
                        ImageRequest imageRequest = ImageRequest.fromUri(imageUrl);
                        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance()
                                .getEncodedCacheKey(imageRequest, null);
                        BinaryResource resource = Fresco.getImagePipelineFactory()
                                .getMainFileCache()
                                .getResource(cacheKey);
                        if (resource instanceof FileBinaryResource) {
                            File cacheFile = ((FileBinaryResource) resource).getFile();
                            try {
                                FileUtil.copyFile(cacheFile, new File(filePath));
                                if (listener != null) {
                                    listener.onSuccess(filePath);
                                }
                            } catch (IOException e) {
                                Timber.e(e, "ShareFrescoImageDownloader exception");
                            }
                        }
                    } finally {
                        CloseableReference.closeSafely(ref);
                    }
                } else if (listener != null) {
                    listener.onFailed(imageUrl);
                }
            }

            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                if (listener != null) {
                    listener.onFailed(imageUrl);
                }
            }

        }, UiThreadImmediateExecutorService.getInstance());
    }

}