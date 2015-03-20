package com.drivemode.android.typeface;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Helper class for setting typeface to the text views.
 *
 * @author KeithYokoma
 */
@SuppressWarnings("unused") // public APIs
public final class TypefaceHelper {
    public static final String TAG = TypefaceHelper.class.getSimpleName();
    private static TypefaceHelper sHelper;
    private final TypefaceCache mCache;

    private TypefaceHelper(Application application) {
        mCache = TypefaceCache.getInstance(application);
    }

    /**
     * Initialize the instance.
     *
     * @param application the application context.
     */
    public static synchronized void initialize(Application application) {
        if (sHelper != null) {
            Log.v(TAG, "already initialized");
        }
        sHelper = new TypefaceHelper(application);
    }

    /**
     * Terminate the instance.
     */
    public static synchronized void destroy() {
        if (sHelper == null) {
            Log.v(TAG, "not initialized yet");
            return;
        }
        sHelper = null;
    }

    /**
     * Retrieve the helper instance.
     *
     * @return the helper instance.
     */
    public static synchronized TypefaceHelper getInstance() {
        if (sHelper == null) {
            throw new IllegalArgumentException("Instance is not initialized yet. Call initialize() first.");
        }
        return sHelper;
    }

    /**
     * Set the typeface to the target view.
     *
     * @param view         to set typeface.
     * @param typefaceName typeface name.
     * @param <V>          text view parameter.
     */
    public <V extends TextView> void setTypeface(V view, String typefaceName) {
        Typeface typeface = mCache.get(typefaceName);
        if (typeface != null) {
            view.setTypeface(typeface);
        }
    }

    /**
     * Set the typeface to the target view.
     *
     * @param view         to set typeface.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     * @param <V>          text view parameter.
     */
    public <V extends TextView> void setTypeface(V view, String typefaceName, int style) {
        Typeface typeface = mCache.get(typefaceName);
        if (typeface != null) {
            view.setTypeface(typeface, style);
        }
    }

    /**
     * Set the typeface to the all text views belong to the view group.
     * Note that this method recursively trace the child view groups and set typeface for the text views.
     *
     * @param viewGroup    the view group that contains text views.
     * @param typefaceName typeface name.
     * @param <V>          view group parameter.
     */
    public <V extends ViewGroup> void setTypeface(V viewGroup, String typefaceName) {
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                setTypeface((ViewGroup) child, typefaceName);
                continue;
            }
            if (!(child instanceof TextView)) {
                continue;
            }
            setTypeface((TextView) child, typefaceName);
        }
    }

    /**
     * Set the typeface to the all text views belong to the view group.
     * Note that this method recursively trace the child view groups and set typeface for the text views.
     *
     * @param viewGroup    the view group that contains text views.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     * @param <V>          view group parameter.
     */
    public <V extends ViewGroup> void setTypeface(V viewGroup, String typefaceName, int style) {
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                setTypeface((ViewGroup) child, typefaceName, style);
                continue;
            }
            if (!(child instanceof TextView)) {
                continue;
            }
            setTypeface((TextView) child, typefaceName, style);
        }
    }

    /**
     * Set the typeface to the target paint.
     *
     * @param paint        the set typeface.
     * @param typefaceName typeface name.
     */
    public void setTypeface(Paint paint, String typefaceName) {
        Typeface typeface = mCache.get(typefaceName);
        if (typeface != null) {
            paint.setTypeface(typeface);
        }
    }

    /**
     * Set the typeface to the all text views belong to the view group.
     *
     * @param context      the context.
     * @param layoutRes    the layout resource id.
     * @param typefaceName typeface name.
     * @return the view.
     */
    public View setTypeface(Context context, @LayoutRes int layoutRes, String typefaceName) {
        return setTypeface(context, layoutRes, null, typefaceName);
    }

    /**
     * Set the typeface to the all text views belong to the view group.
     *
     * @param context      the context.
     * @param layoutRes    the layout resource id.
     * @param parent       the parent view group to attach the layout.
     * @param typefaceName typeface name.
     * @return the view.
     */
    public View setTypeface(Context context, @LayoutRes int layoutRes, ViewGroup parent, String typefaceName) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(layoutRes, parent);
        setTypeface(view, typefaceName);
        return view;
    }

    /**
     * Set the typeface to the all text views belong to the view group.
     *
     * @param context      the context.
     * @param layoutRes    the layout resource id.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     * @return the view.
     */
    public View setTypeface(Context context, @LayoutRes int layoutRes, String typefaceName, int style) {
        return setTypeface(context, layoutRes, null, typefaceName, 0);
    }

    /**
     * Set the typeface to the all text views belong to the view group.
     *
     * @param context      the context.
     * @param layoutRes    the layout resource id.
     * @param parent       the parent view group to attach the layout.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     * @return the view.
     */
    public View setTypeface(Context context, @LayoutRes int layoutRes, ViewGroup parent, String typefaceName, int style) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(layoutRes, parent);
        setTypeface(view, typefaceName, style);
        return view;
    }

    /**
     * Set the typeface to the all text views belong to the activity.
     * Note that we use decor view of the activity so that the typeface will also be applied to action bar.
     *
     * @param activity     the activity.
     * @param typefaceName typeface name.
     */
    public void setTypeface(Activity activity, String typefaceName) {
        setTypeface(activity, typefaceName, 0);
    }

    /**
     * Set the typeface to the all text views belong to the activity.
     * Note that we use decor view of the activity so that the typeface will also be applied to action bar.
     *
     * @param activity     the activity.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     */
    public void setTypeface(Activity activity, String typefaceName, int style) {
        setTypeface((ViewGroup) activity.getWindow().getDecorView(), typefaceName, style);
    }

    /**
     * Set the typeface to the all text views belong to the fragment.
     * Make sure to call this method after fragment view creation.
     * If you use fragments in the support package,
     * call {@link com.drivemode.android.typeface.TypefaceHelper#supportSetTypeface(android.support.v4.app.Fragment, String)} instead.
     *
     * @param fragment     the fragment.
     * @param typefaceName typeface name.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <F extends android.app.Fragment> void setTypeface(F fragment, String typefaceName) {
        setTypeface(fragment, typefaceName, 0);
    }

    /**
     * Set the typeface to the all text views belong to the fragment.
     * Make sure to call this method after fragment view creation.
     * If you use fragments in the support package,
     * call {@link com.drivemode.android.typeface.TypefaceHelper#supportSetTypeface(android.support.v4.app.Fragment, String, int)} instead.
     *
     * @param fragment     the fragment.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public <F extends android.app.Fragment> void setTypeface(F fragment, String typefaceName, int style) {
        View root = fragment.getView();
        if (root instanceof TextView) {
            setTypeface((TextView) root, typefaceName, style);
        } else if (root instanceof ViewGroup) {
            setTypeface((ViewGroup) root, typefaceName, style);
        }
    }

    /**
     * Set the typeface to the all text views belong to the fragment.
     * Make sure to call this method after fragment view creation.
     * And this is a support package fragments only.
     *
     * @param fragment     the fragment.
     * @param typefaceName typeface name.
     */
    public <F extends android.support.v4.app.Fragment> void supportSetTypeface(F fragment, String typefaceName) {
        supportSetTypeface(fragment, typefaceName, 0);
    }

    /**
     * Set the typeface to the all text views belong to the fragment.
     * Make sure to call this method after fragment view creation.
     * And this is a support package fragments only.
     *
     * @param fragment     the fragment.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     */
    public <F extends android.support.v4.app.Fragment> void supportSetTypeface(F fragment, String typefaceName, int style) {
        View root = fragment.getView();
        if (root instanceof TextView) {
            setTypeface((TextView) root, typefaceName, style);
        } else if (root instanceof ViewGroup) {
            setTypeface((ViewGroup) root, typefaceName, style);
        }
    }

    /**
     * Set the typeface for the dialog view.
     *
     * @param dialog       the dialog.
     * @param typefaceName typeface name.
     */
    public <D extends Dialog> void setTypeface(D dialog, String typefaceName) {
        setTypeface(dialog, typefaceName, 0);
    }

    /**
     * Set the typeface for the dialog view.
     *
     * @param dialog       the dialog.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     */
    public <D extends Dialog> void setTypeface(D dialog, String typefaceName, int style) {
        DialogUtils.setTypeface(this, dialog, typefaceName, style);
    }

    /**
     * Set the typeface for the toast view.
     *
     * @param toast        toast.
     * @param typefaceName typeface name.
     * @return toast that the typeface is injected.
     */
    public Toast setTypeface(Toast toast, String typefaceName) {
        return setTypeface(toast, typefaceName, 0);
    }

    /**
     * Set the typeface for the toast view.
     *
     * @param toast        toast.
     * @param typefaceName typeface name.
     * @param style        the typeface style.
     * @return toast that the typeface is injected.
     */
    public Toast setTypeface(Toast toast, String typefaceName, int style) {
        setTypeface((ViewGroup) toast.getView(), typefaceName, style);
        return toast;
    }
}
