package com.achep.acdisplay.animations;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.achep.acdisplay.widgets.CircleView;

/**
 * Created by achep on 19.04.14.
 */
public class CircleRadiusAnimation extends Animation {

    private final CircleView mCircleView;
    private final boolean mChangeDrawnRadius;
    private float from;
    private float to;

    public CircleRadiusAnimation(CircleView circleView, float from, float to, boolean real) {
        super();
        mCircleView = circleView;
        mChangeDrawnRadius = real;
        this.from = from;
        this.to = to;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float value = (from + (to - from) * interpolatedTime);

        if (mChangeDrawnRadius) {
            mCircleView.setRadiusDrawn(value);
        } else {
            mCircleView.setRadius(value);
        }
    }

    public void setRange(float from, float to) {
        this.from = from;
        this.to = to;
    }

}
