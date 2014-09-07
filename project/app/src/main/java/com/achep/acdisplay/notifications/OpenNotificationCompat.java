package com.achep.acdisplay.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.RemoteViews;

import org.apache.commons.lang.builder.EqualsBuilder;

/**
* Created by Artem Chepurnoy on 05.09.2014.
*/
// TODO: Find the way to get notification's ID and TAG.
public class OpenNotificationCompat extends OpenNotification {

    private String mPackageName;

    protected OpenNotificationCompat(Notification n) {
        super(null, n);
    }

    @Override
    public void loadData(Context context) {
        RemoteViews rvs = getNotification().contentView;
        if (rvs == null) rvs = getNotification().bigContentView;
        if (rvs == null) rvs = getNotification().tickerView;
        mPackageName = rvs != null ? rvs.getPackage() : "!2#$%^&*()";

        super.loadData(context);
    }

    //-- COMPARING INSTANCES --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getNotification().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return getNotification().equals(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasIdenticalIds(@Nullable OpenNotification n) {
        if (n == null) {
            return false;
        }

        EqualsBuilder builder = new EqualsBuilder();

        RemoteViews cv = getNotification().contentView;
        RemoteViews cv2 = n.getNotification().contentView;
        if (cv != null && cv2 != null) {
            builder.append(cv.getLayoutId(), cv2.getLayoutId());
        }

        return builder
                .append(getNotification().ledARGB, n.getNotification().ledARGB)
                .append(getPackageName(), n.getPackageName())
                .append(getNotificationData().titleText, n.getNotificationData().titleText)
                .isEquals();

    }

    //-- OTHER ----------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getPackageName() {
        return mPackageName;
    }
}
