package com.github.gnastnosaj.pandora.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.gnastnosaj.boilerplate.Boilerplate;
import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.github.gnastnosaj.pandora.R;
import com.github.gnastnosaj.pandora.datasource.service.UpdateService;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by jasontsang on 1/13/17.
 */

public class AboutActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.about_version)
    TextView version;
    @BindView(R.id.about_features)
    TextView features;
    @BindView(R.id.about_check_for_updates)
    TextView checkForUpdates;
    @BindView(R.id.about_help_and_feedback)
    TextView helpAndFeedback;
    @BindView(R.id.about_copyright)
    TextView copyright;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        initSystemBar();

        setTitle(R.string.about);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        version.setText(String.format(getResources().getString(R.string.about_version), Boilerplate.versionName));
        features.setOnClickListener(this);
        helpAndFeedback.setOnClickListener(this);
        checkForUpdates.setOnClickListener(this);
        copyright.setText(R.string.about_copyright);
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.about_features:
                Intent featuresIntent = new Intent(this, WebViewActivity.class);
                featuresIntent.putExtra(WebViewActivity.EXTRA_TITLE, getResources().getString(R.string.about_features));
                featuresIntent.putExtra(WebViewActivity.EXTRA_HREF, getResources().getString(R.string.url_features));
                startActivity(featuresIntent);
                break;
            case R.id.about_help_and_feedback:
                Intent helpIntent = new Intent(this, WebViewActivity.class);
                helpIntent.putExtra(WebViewActivity.EXTRA_TITLE, getResources().getString(R.string.about_help_and_feedback));
                helpIntent.putExtra(WebViewActivity.EXTRA_HREF, getResources().getString(R.string.url_help_and_feedback));
                startActivity(helpIntent);
                break;
            case R.id.about_check_for_updates:
                UpdateService.checkForUpdate(this);
                break;
        }
    }
}