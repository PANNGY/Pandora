package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.github.gnastnosaj.pandora.model.Plugin;
import com.github.gnastnosaj.pandora.model.VideoInfo;
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

public class PythonVideoDataSource implements IDataSource<List<VideoInfo>> {
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

    private List<VideoInfo> videoInfoList;
    private int pages;
    private int currentPage = 1;
    private int pageSize = 6;

    private boolean remote;
    private boolean checkSource;
    private VideoInfo next;

    public PythonVideoDataSource(Context context, Plugin plugin, int type, @Nullable String id) {
        this.context = context;
        this.plugin = plugin;
        this.type = type;
        this.id = id;
        if (type == TYPE_CATEGORY) {
            cmd = CMD_LIST_CATEGORIES;
        } else if (type == TYPE_VIDEO) {
            cmd = CMD_LIST_VIDEOS;
        } else if (type == TYPE_VIDEO_INFO) {
            cmd = CMD_LIST_VIDEO_INFOS;
        }
    }

    @Override
    public List<VideoInfo> refresh() throws Exception {
        videoInfoList = new ArrayList<>();

        next = null;

        CountDownLatch latch = new CountDownLatch(1);

        List<String> args = new ArrayList<>();
        args.add(plugin.getPluginEntry(context).getAbsolutePath());
        args.add(cmd);
        if (!TextUtils.isEmpty(id)) {
            args.add(id);
        }

        Terminal.getInstance().exec(plugin.getPluginDirectory(context).getAbsolutePath(), args, new TerminalInterface() {
            @Override
            public void onReceive(List<Object> args) {
                String[] data = ((String) args.get(0)).split("\n");
                for (String str : data) {
                    VideoInfo videoInfo;
                    try {
                        videoInfo = new Gson().fromJson(str, VideoInfo.class);
                        if (videoInfo.title.equals("next")) {
                            next = videoInfo;
                        } else {
                            videoInfoList.add(videoInfo);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "videoInfo data parse exception");
                        continue;
                    }
                }
                latch.countDown();
            }

            @Override
            public void onTimeout() {
                latch.countDown();
            }
        }, 5000);

        latch.await();

        pages = videoInfoList.size() / pageSize + (videoInfoList.size() % pageSize == 0 ? 0 : 1);
        currentPage = 1;

        if (next != null) {
            remote = true;
            if (type == TYPE_VIDEO && videoInfoList.size() <= 10) {
                checkSource = true;
            }
            if (checkSource) {
                for (VideoInfo videoInfo : videoInfoList) {
                    try {
                        PythonVideoDataSource videoSourceDataSource = new PythonVideoDataSource(context, plugin, PythonVideoDataSource.TYPE_VIDEO_INFO, videoInfo.id);
                        List<VideoInfo> videoSourceList = videoSourceDataSource.refresh();
                        if (videoSourceList.size() == 1) {
                            videoInfo.url = videoSourceList.get(0).url;
                        }
                    } catch (Exception e) {
                        Timber.e(e, "check videoSource exception");
                    }
                }
            }
            return videoInfoList;
        } else {
            if (type == TYPE_VIDEO && videoInfoList.size() <= 30) {
                checkSource = true;
            }
            return loadMore();
        }
    }

    @Override
    public List<VideoInfo> loadMore() throws Exception {
        List<VideoInfo> videoInfoList = new ArrayList<>();
        try {
            if (remote) {
                CountDownLatch latch = new CountDownLatch(1);

                List<String> args = new ArrayList<>();
                args.add(plugin.getPluginEntry(context).getAbsolutePath());
                args.add(cmd);
                if (!TextUtils.isEmpty(next.id)) {
                    args.add(next.id);
                }

                Terminal.getInstance().exec(plugin.getPluginDirectory(context).getAbsolutePath(), args, new TerminalInterface() {
                    @Override
                    public void onReceive(List<Object> args) {
                        String[] data = ((String) args.get(0)).split("\n");
                        for (String str : data) {
                            VideoInfo videoInfo;
                            try {
                                videoInfo = new Gson().fromJson(str, VideoInfo.class);
                                if (videoInfo.title.equals("next")) {
                                    next = videoInfo;
                                } else {
                                    videoInfoList.add(videoInfo);
                                }
                            } catch (Exception e) {
                                Timber.e(e, "videoInfo data parse error");
                                continue;
                            }
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onTimeout() {
                        latch.countDown();
                    }
                }, 5000);

                latch.await();
            } else {
                int i = (currentPage - 1) * pageSize;
                for (int j = 0; j < pageSize; j++) {
                    if (i < this.videoInfoList.size()) {
                        videoInfoList.add(this.videoInfoList.get(i));
                        i++;
                    } else {
                        break;
                    }
                }
                currentPage++;
            }
        } catch (Exception e) {
            Timber.e(e, "PythonVideoDataSource loadMore error");
        }

        if (checkSource) {
            for (VideoInfo videoInfo : videoInfoList) {
                try {
                    PythonVideoDataSource videoSourceDataSource = new PythonVideoDataSource(context, plugin, PythonVideoDataSource.TYPE_VIDEO_INFO, videoInfo.id);
                    List<VideoInfo> videoSourceList = videoSourceDataSource.refresh();
                    if (videoSourceList.size() == 1) {
                        videoInfo.url = videoSourceList.get(0).url;
                    }
                } catch (Exception e) {
                    Timber.e(e, "check videoSource exception");
                }
            }
        }
        return videoInfoList;
    }

    @Override
    public boolean hasMore() {
        if (remote) {
            return next != null;
        } else {
            return currentPage <= pages;
        }
    }
}

