package com.achep.acdisplay.ui.drawables;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.achep.acdisplay.Config;
import com.achep.base.content.ConfigBase;

/**
 * Created by Artem Chepurnoy on 13.05.2015.
 */
public class CornerIconDrawable extends Drawable {

    private final String mKey;

    private Drawable mDrawable;
    private int mActionId;
    private int mAlpha;

    /**
     * A config listener.
     */
    @NonNull
    private final ConfigBase.OnConfigChangedListener mConfigListener =
            new ConfigBase.OnConfigChangedListener() {
                @Override
                public void onConfigChanged(@NonNull ConfigBase config,
                                            @NonNull String key,
                                            @NonNull Object value) {
                    if (key.equals(mKey)) update(config.getContext());
                }
            };

    public CornerIconDrawable(@NonNull String key) {
        mKey = key;
    }

    public void start(@NonNull Context context) {
        Config.getInstance().registerListener(mConfigListener);
        update(context);
    }

    public void stop() {
        Config.getInstance().unregisterListener(mConfigListener);
    }

    private void update(@NonNull Context context) {
        /*
        Config config = Config.getInstance();
        and_clear_current_drawable:
        {
            int actionId = (int) config.getOption(mKey).read(config);
            if (mActionId == (mActionId = actionId)) return;
            if (actionId == Config.CORNER_UNLOCK) break and_clear_current_drawable;
            final int iconRes = CornerHelper.getIconResource(actionId);
            Drawable drawable = ResUtils.getDrawable(context, iconRes);
            if (drawable != null) {
                drawable = drawable.mutate();
                drawable.setAlpha(mAlpha);
                drawable.setBounds(0, 0,
                        drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight());
                // Update current bounds.
                setBounds(drawable.getBounds());
            }
            setDrawable(drawable);
            return;
        }
        setDrawable(null);
        */
    }

    private void setDrawable(@Nullable Drawable drawable) {
        mDrawable = drawable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(Canvas canvas) {
        if (mDrawable != null) mDrawable.draw(canvas);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAlpha(int alpha) {
        if (mDrawable != null) mDrawable.setAlpha(alpha);
        mAlpha = alpha;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColorFilter(ColorFilter cf) {
        throw new RuntimeException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
