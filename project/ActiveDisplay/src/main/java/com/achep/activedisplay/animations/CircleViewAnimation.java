package com.achep.activedisplay.animations;

import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.achep.activedisplay.widgets.CircleView;

/**
 * Created by achep on 19.04.14.
 */
public class CircleViewAnimation extends Animation {

    private final CircleView mCircleView;
    private float from;
    private float to;

    public CircleViewAnimation(CircleView circleView, float from, float to) {
        super();
        mCircleView = circleView;
        this.from = from;
        this.to = to;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float value = (from + (to - from) * interpolatedTime);

        mCircleView.setRadius(value);
    }

    public void setRange(float from, float to) {
        this.from = from;
        this.to = to;
    }

}
