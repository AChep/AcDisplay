package com.drivemode.android.typeface;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

/**
 * This is a typeface instance cache.
 * The cache is to avoid memory leak problem when a typeface is loaded.
 * See the link for more details about the memory leak issue.
 * (https://code.google.com/p/android/issues/detail?id=9904)
 *
 * @author hnakagawa
 */
/* package */ final class TypefaceCache {
    private static TypefaceCache sInstance;

    private final Hashtable<String, Typeface> mCache = new Hashtable<>();

    private final Application mApplication;

    private TypefaceCache(Application application) {
        mApplication = application;
    }

    /**
     * If the cache has an instance for the typeface name, this will return the instance immediately.
     * Otherwise this method will create typeface instance and put it into the cache and return the instance.
     *
     * @param name the typeface name.
     * @return {@link android.graphics.Typeface} instance.
     */
    public synchronized Typeface get(String name) {
        Typeface typeface = mCache.get(name);
        if (typeface == null) {
            try {
                typeface = Typeface.createFromAsset(mApplication.getAssets(), name);
            } catch (Exception exp) {
                return null;
            }
            mCache.put(name, typeface);
        }
        return typeface;
    }

    /**
     * Retrieve this cache.
     *
     * @param context the context.
     * @return the cache instance.
     */
    public static synchronized TypefaceCache getInstance(Context context) {
        if (sInstance == null)
            sInstance = new TypefaceCache((Application) context.getApplicationContext());
        return sInstance;
    }
}
