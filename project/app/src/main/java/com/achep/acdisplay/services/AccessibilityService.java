package com.achep.acdisplay.services;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;

import com.achep.acdisplay.Device;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.OpenNotification;

/**
 * Created by Artem Chepurnoy on 06.09.2014.
 */
public class AccessibilityService extends android.accessibilityservice.AccessibilityService {

    public static boolean isRunning;

    @Override
    public void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        isRunning = true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Parcelable parcelable = event.getParcelableData();
                if (parcelable instanceof Notification) {
                    if (Device.hasJellyBeanMR2Api()) {
                        // No need to use the accessibility service
                        // instead of NotificationListener.
                        return;
                    }

                    Notification notification = (Notification) parcelable;
                    OpenNotification openNotification = OpenNotification.newInstance(notification);
                    NotificationPresenter.getInstance().postNotification(this, openNotification, 0);
                }
                break;
        }
    }

    @Override
    public void onInterrupt() {
        isRunning = false;
    }
}
