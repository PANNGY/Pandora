package com.github.gnastnosaj.pandora.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;

/**
 * Created by Jason on 8/17/2015.
 */
public class RatioImageView extends SimpleDraweeView {

    private int originalWidth;
    private int originalHeight;

    public RatioImageView(Context context) {
        super(context);
    }

    public RatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOriginalSize(int originalWidth, int originalHeight) {
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            float ratio = (float) originalWidth / (float) originalHeight;

            int width = layoutParams.width;

            if (width > 0) {
                layoutParams.height = (int) ((float) width / ratio);
            }

            setLayoutParams(layoutParams);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (originalWidth > 0 && originalHeight > 0) {
            float ratio = (float) originalWidth / (float) originalHeight;

            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            if (width > 0) {
                height = (int) ((float) width / ratio);
            }

            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

}