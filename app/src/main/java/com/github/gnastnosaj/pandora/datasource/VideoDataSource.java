package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.model.VideoData;
import com.github.gnastnosaj.pythonforandroid.Terminal;
import com.github.gnastnosaj.pythonforandroid.TerminalInterface;
import com.google.gson.Gson;
import com.shizhefei.mvc.IDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import timber.log.Timber;

/**
 * Created by jason on 10/21/2016.
 */

public class VideoDataSource implements IDataSource<List<VideoData>> {
    public final static String CMD_LIST_CATEGORIES = "list_categories";
    public final static String CMD_LIST_VIDEOS = "list_videos";
    public final static String CMD_LIST_VIDEO_INFOS = "list_video_infos";

    public final static int TYPE_CATEGORY = 0;
    public final static int TYPE_VIDEO = 1;
    public final static int TYPE_VIDEO_INFO = 2;

    private Context context;
    private Plugin plugin;
    private int type;
    private String cmd;
    private String id;

    private List<VideoData> videoDataList;
    private int pages;
    private int currentPage = 1;
    private int pageSize = 10;

    private boolean remote;
    private VideoData next;

    public VideoDataSource(Context context, Plugin plugin, int type, @Nullable String id) {
        this.context = context;
        this.plugin = plugin;
        this.type = type;
        if (type == TYPE_CATEGORY) {
            cmd = CMD_LIST_CATEGORIES;
        } else if (type == TYPE_VIDEO) {
            cmd = CMD_LIST_VIDEOS;
        } else if (type == TYPE_VIDEO_INFO) {
            cmd = CMD_LIST_VIDEO_INFOS;
        }
    }

    @Override
    public List<VideoData> refresh() throws Exception {
        videoDataList = new ArrayList<>();

        next = null;

        CountDownLatch latch = new CountDownLatch(1);

        List<String> args = new ArrayList<>();
        args.add(plugin.getPluginEntry(context).getAbsolutePath());
        args.add(cmd);
        if (TextUtils.isEmpty(id)) {
            args.add(id);
        }

        Terminal.getInstance().exec(plugin.getPluginDirectory(context).getAbsolutePath(), args, new TerminalInterface() {
            @Override
            public void onReceive(List<Object> args) {
                String[] data = ((String) args.get(0)).split("\n");
                for (String str : data) {
                    VideoData videoData;
                    try {
                        videoData = new Gson().fromJson(str, VideoData.class);
                    } catch (Exception e) {
                        Timber.e(e, "videoData data parse error");
                        continue;
                    }
                    if (videoData.title.equals("next")) {
                        next = videoData;
                    } else {
                        videoDataList.add(videoData);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onTimeout() {
                latch.countDown();
            }
        }, 30000);

        latch.await();

        pages = videoDataList.size() / pageSize + (videoDataList.size() % pageSize == 0 ? 0 : 1);
        currentPage = 1;

        if (next != null) {
            remote = true;
            return videoDataList;
        } else {
            return loadMore();
        }
    }

    @Override
    public List<VideoData> loadMore() throws Exception {
        List<VideoData> videoDataList = new ArrayList<>();
        try {
            if (remote) {
                CountDownLatch latch = new CountDownLatch(1);

                List<String> args = new ArrayList<>();
                args.add(plugin.getPluginEntry(context).getAbsolutePath());
                args.add(cmd);
                if (TextUtils.isEmpty(next.id)) {
                    args.add(next.id);
                }

                Terminal.getInstance().exec(plugin.getPluginDirectory(context).getAbsolutePath(), args, new TerminalInterface() {
                    @Override
                    public void onReceive(List<Object> args) {
                        String[] data = ((String) args.get(0)).split("\n");
                        for (String str : data) {
                            VideoData videoData;
                            try {
                                videoData = new Gson().fromJson(str, VideoData.class);
                            } catch (Exception e) {
                                Timber.e(e, "videoData data parse error");
                                continue;
                            }
                            if (videoData.title.equals("next")) {
                                next = videoData;
                            } else {
                                videoDataList.add(videoData);
                            }
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onTimeout() {
                        latch.countDown();
                    }
                }, 30000);

                latch.await();
            } else {
                int i = (currentPage - 1) * pageSize;
                for (int j = 0; j < pageSize; j++) {
                    if (i < this.videoDataList.size()) {
                        videoDataList.add(this.videoDataList.get(i));
                        i++;
                    } else {
                        break;
                    }
                }
                currentPage++;
            }
        } catch (Exception e) {
            Timber.e(e, "VideoDataSource loadMore error");
        }
        return videoDataList;
    }

    @Override
    public boolean hasMore() {
        if (remote) {
            return next != null;
        } else {
            return currentPage < pages;
        }
    }
}

