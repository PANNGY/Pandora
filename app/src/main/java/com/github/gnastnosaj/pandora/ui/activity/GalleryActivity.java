package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.bilibili.socialize.share.core.shareparam.ShareImage;
import com.bilibili.socialize.share.core.shareparam.ShareParamImage;
import com.github.gnastnosaj.boilerplate.rxbus.RxBus;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.adapter.GalleryAdapter;
import com.github.gnastnosaj.pandora.datasource.SimpleDataSource;
import com.github.gnastnosaj.pandora.event.TagEvent;
import com.github.gnastnosaj.pandora.event.ToolbarEvent;
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
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import me.next.tagview.TagCloudView;

/**
 * Created by jasontsang on 5/26/17.
 */

public class GalleryActivity extends BaseActivity {
    public final static String EXTRA_TAB_DATASOURCE = "tab_datasource";
    public final static String EXTRA_GALLERY_DATASOURCE = "gallery_datasource";
    public final static String EXTRA_TITLE = "title";
    public final static String EXTRA_HREF = "href";
    public static final String EXTRA_CACHE = "cache";

    @BindView(R.id.app_bar)
    AppBarLayout appBar;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.view_pager)
    ViewPager viewPager;

    @BindView(R.id.tag_cloud_view)
    TagCloudView tagCloudView;

    private String tabDataSource;
    private String galleryDataSource;
    private String title;
    private String href;
    private List<JSoupData> cache;

    private boolean isAppBarHidden;
    private Observable<TagEvent> tagEventObservable;
    private GalleryAdapter galleryAdapter;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RxBus.getInstance().unregister(href, tagEventObservable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_view_pager_with_tag_cloud);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        tabDataSource = getIntent().getStringExtra(EXTRA_TAB_DATASOURCE);
        galleryDataSource = getIntent().getStringExtra(EXTRA_GALLERY_DATASOURCE);
        title = getIntent().getStringExtra(EXTRA_TITLE);
        href = getIntent().getStringExtra(EXTRA_HREF);
        cache = getIntent().getParcelableArrayListExtra(EXTRA_CACHE);

        setTitle(TextUtils.isEmpty(title) ? "" : title);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        tagEventObservable = RxBus.getInstance().register(href, TagEvent.class);
        tagEventObservable.compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tagEvent -> {
                    List<String> tags = new ArrayList<>();
                    for (JSoupLink link : tagEvent.tags) {
                        tags.add(link.title);
                    }
                    tagCloudView.setTags(tags);
                    tagCloudView.setOnTagClickListener(position -> {
                        Intent intent = new Intent(this, SimpleTabActivity.class);
                        intent.putExtra(SimpleTabActivity.EXTRA_TAB_DATASOURCE, tabDataSource);
                        intent.putExtra(SimpleTabActivity.EXTRA_GALLERY_DATASOURCE, galleryDataSource);
                        intent.putExtra(SimpleTabActivity.EXTRA_TITLE, tags.get(position));
                        intent.putExtra(SimpleTabActivity.EXTRA_HREF, tagEvent.tags.get(position).url);
                        startActivity(intent);
                    });
                });

        initViewPager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ToolbarEvent.observable.compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(toolbarEvent -> hideOrShowToolbar());
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
                String thumbnail = galleryAdapter.getData().get(viewPager.getCurrentItem()).getAttr("thumbnail");
                ShareParamImage shareParamImage = new ShareParamImage(title, thumbnail, href);
                shareParamImage.setImage(new ShareImage(thumbnail));
                ShareHelper.share(this, shareParamImage);
                return true;
            case R.id.action_mosaic:
                Intent i = new Intent(this, MosaicActivity.class);
                i.putExtra(MosaicActivity.EXTRA_TITLE, title);
                i.putExtra(MosaicActivity.EXTRA_URL, galleryAdapter.getData().get(viewPager.getCurrentItem()).getAttr("thumbnail"));
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViewPager() {
        SimpleDataSource galleryDataSource = new SimpleDataSource(this, this.galleryDataSource, href);
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

    public void hideOrShowToolbar() {
        appBar.animate()
                .translationY(isAppBarHidden ? 0 : -appBar.getHeight())
                .setInterpolator(new DecelerateInterpolator(2))
                .start();
        tagCloudView.animate()
                .translationY(isAppBarHidden ? 0 : tagCloudView.getHeight())
                .setInterpolator(new DecelerateInterpolator(2))
                .start();
        isAppBarHidden = !isAppBarHidden;
    }
}
