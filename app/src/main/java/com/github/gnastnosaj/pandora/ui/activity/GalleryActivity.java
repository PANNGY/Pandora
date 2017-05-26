package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bilibili.socialize.share.core.shareparam.ShareImage;
import com.bilibili.socialize.share.core.shareparam.ShareParamImage;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.GalleryAdapter;
import com.github.gnastnosaj.pandora.datasource.SimpleDataSource;
import com.github.gnastnosaj.pandora.event.TagEvent;
import com.github.gnastnosaj.pandora.model.JSoupData;
import com.github.gnastnosaj.pandora.model.JSoupLink;
import com.github.gnastnosaj.pandora.util.ShareHelper;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;
import com.shizhefei.mvc.ILoadViewFactory;
import com.shizhefei.mvc.MVCHelper;
import com.shizhefei.mvc.MVCNormalHelper;
import com.shizhefei.mvc.viewhandler.ViewHandler;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import me.next.tagview.TagCloudView;

/**
 * Created by jasontsang on 5/26/17.
 */

public class GalleryActivity extends BaseActivity {
    public final static String EXTRA_DATASOURCE = "datasource";
    public final static String EXTRA_TITLE = "title";
    public final static String EXTRA_HREF = "href";
    public static final String EXTRA_CACHE = "cache";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.view_pager)
    ViewPager viewPager;

    @BindView(R.id.tag_cloud_view)
    TagCloudView tagCloudView;

    private String datasource;
    private String title;
    private String href;
    private List<JSoupData> cache;

    private GalleryAdapter galleryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_view_pager_with_tag_cloud);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        datasource = getIntent().getStringExtra(EXTRA_DATASOURCE);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        href = getIntent().getStringExtra(EXTRA_HREF);
        cache = getIntent().getParcelableArrayListExtra(EXTRA_CACHE);

        if (TextUtils.isEmpty(title)) {
            setTitle("");
        } else {
            setTitle(title);
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        RxBus.getInstance().register(TagEvent.class, TagEvent.class)
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tagEvent -> {
                    List<String> tags = new ArrayList<>();
                    for (JSoupLink link : tagEvent.tags) {
                        tags.add(link.title);
                    }
                    tagCloudView.setTags(tags);
                });

        initViewPager();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gallery, menu);
        menu.findItem(R.id.action_share).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_share)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_favourite).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_label_heart)
                .color(Color.WHITE).sizeDp(18));
        menu.findItem(R.id.action_mosaic).setIcon(new IconicsDrawable(this)
                .icon(MaterialDesignIconic.Icon.gmi_blur_linear)
                .color(Color.WHITE).sizeDp(18));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_share:
                String cover = galleryAdapter.getData().get(0).getAttr("cover");
                ShareParamImage shareParamImage = new ShareParamImage(title, cover, href);
                shareParamImage.setImage(new ShareImage(cover));
                ShareHelper.share(this, shareParamImage);
                return true;
            case R.id.action_mosaic:
                Intent i = new Intent(this, MosaicActivity.class);
                i.putExtra(MosaicActivity.EXTRA_IMAGE_TITLE, title);
                i.putExtra(MosaicActivity.EXTRA_IMAGE_URL, galleryAdapter.getData().get(viewPager.getCurrentItem()).getAttr("thumbnail"));
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViewPager() {
        SimpleDataSource galleryDataSource = new SimpleDataSource(this, datasource, href);
        galleryDataSource.setCache(cache);
        galleryAdapter = new GalleryAdapter(this);

        MVCHelper mvcHelper = new MVCNormalHelper(viewPager);

        mvcHelper.setDataSource(galleryDataSource);
        mvcHelper.setAdapter(galleryAdapter, new ViewHandler() {
            @Override
            public boolean handleSetAdapter(View contentView, Object viewAdapter, ILoadViewFactory.ILoadMoreView loadMoreView, View.OnClickListener onClickLoadMoreListener) {
                ViewPager viewPager = (ViewPager) contentView;
                boolean hasInit = false;
                if (loadMoreView != null) {
                    loadMoreView.init(new ILoadViewFactory.FootViewAdder() {
                        @Override
                        public View addFootView(View view) {
                            return view;
                        }

                        @Override
                        public View addFootView(int layoutId) {
                            View view = LayoutInflater.from(viewPager.getContext()).inflate(layoutId, viewPager, false);
                            return addFootView(view);
                        }

                        @Override
                        public View getContentView() {
                            return viewPager;
                        }
                    }, onClickLoadMoreListener);
                    hasInit = true;
                }
                viewPager.setAdapter((PagerAdapter) viewAdapter);
                return hasInit;
            }

            @Override
            public void setOnScrollBottomListener(View view, MVCHelper.OnScrollBottomListener onScrollBottomListener) {
            }
        });
        mvcHelper.refresh();
    }
}
