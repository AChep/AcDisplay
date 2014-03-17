/*
 * Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
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

import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.achep.activedisplay.R;
import com.achep.activedisplay.notifications.NotificationData;
import com.achep.activedisplay.notifications.NotificationUtils;
import com.achep.activedisplay.utils.LogUtils;

import java.util.ArrayList;

/**
 * Jelly bean 4.3 backport.
 */
final class ViewParser implements Parser.NotificationParser {

    private static final String TAG = "ViewParser";

    @Override
    public NotificationData parse(Context contextApp, StatusBarNotification notification, NotificationData nd) {
        Log.i(TAG, "Parsing notification using view parser.");

        nd.number = notification.getNotification().number;

        // Replace app's context with notification's context
        // to be able to get its resources.
        Context contextNotify = NotificationUtils.createContext(contextApp, notification);
        final Notification n = notification.getNotification();
        final RemoteViews rvs = n.bigContentView == null ? n.contentView : n.bigContentView;

        ViewGroup view;
        try {
            LayoutInflater inflater = (LayoutInflater) contextNotify.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (ViewGroup) inflater.inflate(rvs.getLayoutId(), null);
            if (view == null) {
                LogUtils.track();
                return nd;
            }
            rvs.reapply(contextNotify, view);
        } catch (Exception e) {
            LogUtils.track();
            e.printStackTrace();
            return nd;
        }

        ArrayList<ViewParentLink<TextView>> textViews =
                new RecursiveFinder<>(TextView.class)
                        .expand(view, true);
        ViewParentLink<TextView> title = findTitleTextView(textViews);

        nd.titleText = title.view.getText();
        nd.messageText = findMessageText(contextApp, textViews, n, title);
        return nd;
    }

    private ViewParentLink<TextView> findTitleTextView(
            ArrayList<ViewParentLink<TextView>> textViewsList) {
        int item = 0;
        float maxTextSize = textViewsList.get(item).view.getTextSize();

        final int size = textViewsList.size();
        for (int i = 1; i < size; i++) {
            float textSize = textViewsList.get(i).view.getTextSize();
            if (textSize > maxTextSize) {
                maxTextSize = textSize;
                item = i;
            }
        }

        return textViewsList.get(item);
    }

    private String findMessageText(Context context,
                                   ArrayList<ViewParentLink<TextView>> textViewsList,
                                   Notification notification,
                                   ViewParentLink<TextView> title) {
        float subtextSize = context.getResources().getDimension(R.dimen.notification_subtext_size);
        int offset = 0;

        // Remove title view
        textViewsList.remove(title);

        // Remove a lot of unneeded action texts
        if (notification.actions != null)
            for (Notification.Action action : notification.actions) {
                final int size = textViewsList.size();
                for (int i = 0; i < size; i++) {
                    CharSequence text = textViewsList.get(i).view.getText();
                    assert text != null;
                    if (text.equals(action.title)) {
                        textViewsList.remove(i);
                        break;
                    }
                }
            }

        // Remove subtexts such as time or progress text.
        int size = textViewsList.size();
        for (int i = 0; i < size; i++) {
            final int k = i + offset;
            final TextView view = textViewsList.get(k).view;
            if (view.getTextSize() == subtextSize
                    || view.toString().matches("^(\\s*|)$")) {
                textViewsList.remove(k);
                offset--;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (ViewParentLink<TextView> tv : textViewsList) {
            sb.append(tv.view.getText());
            sb.append('\n');
        }
        if (sb.length() > 0) sb.delete(sb.length() - 1, sb.length());

        return Parser.removeSpaces(sb.toString());
    }

    private static class RecursiveFinder<T extends View> {

        private final ArrayList<ViewParentLink<T>> list;
        private final Class<T> clazz;

        public RecursiveFinder(Class<T> clazz) {
            this.list = new ArrayList<>();
            this.clazz = clazz;
        }

        private ArrayList<ViewParentLink<T>> expand(ViewGroup viewGroup, boolean visibleOnly) {
            int offset = 0;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = viewGroup.getChildAt(i + offset);

                if (child == null || (visibleOnly && child.getVisibility() != View.VISIBLE)) {
                    continue;
                }

                if (clazz.isAssignableFrom(child.getClass())) {
                    //noinspection unchecked
                    list.add(new ViewParentLink<>((T) child, viewGroup));
                } else if (child instanceof ViewGroup) {
                    expand((ViewGroup) child, visibleOnly);
                }
            }
            return list;
        }
    }

    private static class ViewParentLink<T extends View> {

        private ViewGroup parent;
        private T view;

        public ViewParentLink(T view, ViewGroup parent) {
            this.parent = parent;
            this.view = view;
        }

    }

}
