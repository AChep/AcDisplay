package com.achep.acdisplay.services.headsup;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by Artem Chepurnoy on 16.09.2014.
 */
public class HeadsUpView extends FrameLayout {

    private HeadsUpManager mManager;

    public HeadsUpView(Context context) {
        super(context);
    }

    public HeadsUpView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HeadsUpView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setHeadsUpManager(HeadsUpManager manager) {
        mManager = manager;
    }

}
