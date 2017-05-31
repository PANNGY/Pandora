package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.URLSpan;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jasontsang on 5/31/17.
 */

public class OpenSourceActivity extends BaseActivity implements View.OnTouchListener {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.application_info)
    TextView applicationInfo;

    @BindView(R.id.developer_info)
    TextView developerInfo;

    @BindView(R.id.license)
    TextView license;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opensource);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        setTitle(R.string.drawer_item_open_source);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        applicationInfo.setText(Html.fromHtml(getResources().getString(R.string.application_info_text, Boilerplate.versionName)));
        developerInfo.setText(Html.fromHtml(getResources().getString(R.string.developer_info_text)));
        license.setText(Html.fromHtml(getResources().getString(R.string.license_text, new SimpleDateFormat("yyyy").format(new Date()))));

        applicationInfo.setOnTouchListener(this);
        developerInfo.setOnTouchListener(this);
        license.setOnTouchListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            TextView widget = (TextView) view;
            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            URLSpan[] link = Spannable.Factory.getInstance()
                    .newSpannable(widget.getText())
                    .getSpans(off, off, URLSpan.class);

            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    Intent i = new Intent(this, WebViewActivity.class);
                    i.putExtra(WebViewActivity.EXTRA_HREF, Uri.parse(link[0].getURL()));
                    startActivity(i);
                }
                return true;
            }
        }
        return false;
    }
}
