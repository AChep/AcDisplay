/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.achep.acdisplay.notifications.parser;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.NotificationData;
import com.achep.acdisplay.notifications.NotificationUtils;

import java.util.ArrayList;

/**
 * Jelly bean 4.3 backport.
 */
public final class ViewExtractor implements Extractor {

    private static final String TAG = "ViewExtractor";

    @Override
    public NotificationData loadTexts(Context contextApp, StatusBarNotification notification, NotificationData data) {
        Log.i(TAG, "Parsing notification using view parser.");

        data.number = notification.getNotification().number;

        // Replace app's context with notification's context
        // to be able to get its resources.
        Context contextNotify = NotificationUtils.createContext(contextApp, notification);
        final Notification n = notification.getNotification();
        final RemoteViews rvs = n.bigContentView == null ? n.contentView : n.bigContentView;

        assert rvs != null;
        // TODO: Compare both bigContentView and contentView to get actions and so.

        LayoutInflater inflater = (LayoutInflater) contextNotify.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup view = (ViewGroup) inflater.inflate(rvs.getLayoutId(), null);

        if (view == null) {
            Log.e(TAG, "Failed to inflate notification\'s layout.");
            return data;
        }
        try {
            rvs.reapply(contextNotify, view);
        } catch (Exception e) {
            Log.e(TAG, "Failed to reapply content from remote view.");
            e.printStackTrace();
            return data;
        }

        ArrayList<TextView> textViews = new RecursiveFinder<>(TextView.class).expand(view);
        removeClickableViews(textViews);
        removeSubtextViews(contextApp, textViews);
        if (Device.hasKitKatApi()) {
            removeActionViews(n, textViews);
        }

        TextView title = findTitleTextView(textViews);
        textViews.remove(title); // no need of title view anymore

        int length = textViews.size();
        CharSequence[] messages = new CharSequence[length];
        for (int i = 0; i < length; i++) {
            messages[i] = textViews.get(i).getText();
        }

        data.titleText = title.getText();
        data.messageText = Utils.mergeLargeMessage(messages);
        return data;
    }

    private TextView findTitleTextView(ArrayList<TextView> textViews) {
        // The idea is that title text is biggest from all
        // views here.
        TextView biggest = null;
        for (TextView textView : textViews) {
            if (biggest == null || textView.getTextSize() > biggest.getTextSize()) {
                biggest = textView;
            }
        }
        return biggest;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void removeActionViews(Notification n, ArrayList<TextView> textViews) {
        if (n.actions == null) {
            return;
        }

        for (Notification.Action action : n.actions) {
            for (int i = textViews.size() - 1; i >= 0; i--) {
                CharSequence text = textViews.get(i).getText();
                if (text != null && text.equals(action.title)) {
                    textViews.remove(i);
                    break;
                }
            }
        }
    }

    private void removeClickableViews(ArrayList<TextView> textViews) {
        for (int i = textViews.size() - 1; i >= 0; i--) {
            TextView child = textViews.get(i);
            if (child.isClickable() || child.getVisibility() != View.VISIBLE) {
                textViews.remove(i);
                break;
            }
        }
    }

    private void removeSubtextViews(Context context, ArrayList<TextView> textViews) {
        float subtextSize = context.getResources().getDimension(R.dimen.notification_subtext_size);
        for (int i = textViews.size() - 1; i >= 0; i--) {
            final TextView child = textViews.get(i);
            final String text = child.getText().toString();
            if (child.getTextSize() == subtextSize
                    // empty textviews
                    || text.matches("^(\\s*|)$")
                    // clock textviews
                    || text.matches("^\\d{1,2}:\\d{1,2}(\\s?\\w{2}|)$")) {
                textViews.remove(i);
            }
        }
    }

    private static class RecursiveFinder<T extends View> {

        private final ArrayList<T> list;
        private final Class<T> clazz;

        public RecursiveFinder(Class<T> clazz) {
            this.list = new ArrayList<>();
            this.clazz = clazz;
        }

        public ArrayList<T> expand(ViewGroup viewGroup) {
            int offset = 0;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i + offset);

                if (child == null) {
                    continue;
                }

                if (clazz.isAssignableFrom(child.getClass())) {
                    //noinspection unchecked
                    list.add((T) child);
                } else if (child instanceof ViewGroup) {
                    expand((ViewGroup) child);
                }
            }
            return list;
        }
    }

}
