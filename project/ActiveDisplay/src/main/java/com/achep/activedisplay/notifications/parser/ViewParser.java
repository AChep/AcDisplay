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
package com.achep.activedisplay.notifications.parser;

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

import com.achep.activedisplay.Device;
import com.achep.activedisplay.R;
import com.achep.activedisplay.notifications.NotificationData;
import com.achep.activedisplay.notifications.NotificationUtils;

import java.util.ArrayList;

/**
 * Jelly bean 4.3 backport.
 */
public final class ViewParser implements IExtractor {

    private static final String TAG = "ViewParser";

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public NotificationData loadTexts(Context contextApp, StatusBarNotification notification, NotificationData data) {
        Log.i(TAG, "Parsing notification using view parser.");

        data.number = notification.getNotification().number;

        // Replace app's context with notification's context
        // to be able to get its resources.
        Context contextNotify = NotificationUtils.createContext(contextApp, notification);
        final Notification n = notification.getNotification();
        final RemoteViews rvs = n.bigContentView == null ? n.contentView : n.bigContentView;

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

        ArrayList<TextView> textViews = new RecursiveFinder<>(TextView.class).expand(view, true);

        // Get rid of notification' actions
        if (Device.hasKitKatApi() && n.actions != null) {
            for (Notification.Action action : n.actions) {
                for (int i = textViews.size() - 1; i >= 0; i--) {
                    CharSequence text = textViews.get(i).getText();
                    assert text != null;
                    if (text.equals(action.title)) {
                        textViews.remove(i);
                        break;
                    }
                }
            }
        }

        // Clickable views are probably not needed too.
        for (int i = textViews.size() - 1; i >= 0; i--) {
            if (textViews.get(i).isClickable()) {
                textViews.remove(i);
                break;
            }
        }

        TextView title = findTitleTextView(textViews);
        textViews.remove(title); // no need of title

        data.titleText = title.getText();
        data.messageText = findMessageText(contextApp, textViews);
        return data;
    }

    private TextView findTitleTextView(ArrayList<TextView> textViewsList) {
        // The idea is that title text is biggest from all
        // views here.
        TextView biggest = null;
        for (TextView textView : textViewsList) {
            if (biggest == null || textView.getTextSize() > biggest.getTextSize()) {
                biggest = textView;
            }
        }
        return biggest;
    }

    private String findMessageText(Context context, ArrayList<TextView> textViewsList) {
        // Remove subtexts such as time or progress text.
        float subtextSize = context.getResources().getDimension(R.dimen.notification_subtext_size);
        for (int i = textViewsList.size() - 1; i >= 0; i--) {
            final TextView view = textViewsList.get(i);
            final String text = view.getText().toString();
            if (view.getTextSize() == subtextSize
                    || text.matches("^(\\s*|)$")
                    || text.matches("^\\d{1,2}:\\d{1,2}(\\s?\\w{2}|)$")) {
                textViewsList.remove(i);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (TextView tv : textViewsList) {
            sb.append(tv.getText());
            sb.append('\n');
        }
        if (sb.length() > 0) sb.delete(sb.length() - 1, sb.length());

        return Utils.removeSpaces(sb.toString());
    }

    private static class RecursiveFinder<T extends View> {

        private final ArrayList<T> list;
        private final Class<T> clazz;

        public RecursiveFinder(Class<T> clazz) {
            this.list = new ArrayList<>();
            this.clazz = clazz;
        }

        public ArrayList<T> expand(ViewGroup viewGroup, boolean visibleOnly) {
            int offset = 0;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i + offset);

                if (child == null
                        || visibleOnly && child.getVisibility() != View.VISIBLE) {
                    continue;
                }

                if (clazz.isAssignableFrom(child.getClass())) {
                    //noinspection unchecked
                    list.add((T) child);
                } else if (child instanceof ViewGroup) {
                    expand((ViewGroup) child, visibleOnly);
                }
            }
            return list;
        }
    }

}
