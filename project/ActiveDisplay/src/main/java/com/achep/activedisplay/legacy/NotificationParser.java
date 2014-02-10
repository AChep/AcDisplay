/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.legacy;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
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

import java.util.ArrayList;

/**
 * Created by Artem on 13.01.14.
 */
public class NotificationParser implements INotificationParser {

    private static final String TAG = "NotificationParser";

    public NotificationData parce(Context myContext, StatusBarNotification notification) {
        NotificationData nd = new NotificationData();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            try {
                Bundle extras = notification.getNotification().extras;
                nd.titleText = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
                nd.infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
                nd.subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                nd.summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);

                CharSequence[] textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                if (textLines != null) {
                    StringBuilder sb = new StringBuilder();
                    for (CharSequence line : textLines) {
                        sb.append(line);
                        sb.append('\n');
                    }
                    nd.messageText = sb.toString();
                } else {
                    nd.messageText = extras.getCharSequence(Notification.EXTRA_TEXT);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.wtf(TAG, "KitKat notification parsing failed.");
                return null;
            }

            if (nd.titleText != null && nd.messageText != null) {
                return nd;
            }
        }


        // Replace app's context with notification's context
        // to be able to get its resources.
        Context context = NotificationUtils.createContext(myContext, notification);
        final Notification n = notification.getNotification();
        final RemoteViews rvs = n.bigContentView == null ? n.contentView : n.bigContentView;

        ViewGroup view;
        try {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (ViewGroup) inflater.inflate(rvs.getLayoutId(), null);
            if (view == null) {
                Log.w(TAG, "Notification package (layout is @null): " + notification.getPackageName());
                return null;
            }
            rvs.reapply(context, view);
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(TAG, "Notification package (exception): " + notification.getPackageName());
            return null;
        }

        ArrayList<ViewParentLink<TextView>> textViews =
                new RecursiveFinder<>(TextView.class)
                        .expand(view, true);
        ViewParentLink<TextView> title = findTitleTextView(textViews);

        nd.titleText = title.view.getText();
        nd.messageText = findMessageText(myContext, textViews, n, title);

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
                    if (textViewsList.get(i).view.getText().equals(action.title)) {
                        Log.d(TAG, "Removed \"" + action.title + "\" action from the list of texts.");

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
                Log.d(TAG, "Removed item \"" + view.getText() + "\" from the list of texts.");

                textViewsList.remove(k);
                offset--;
            }
        }

        /*final SimilarityStrategy strategy = new JaroWinklerStrategy();
        final StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
        final String titleText = title.view.getText().toString();
        final String tickerText = notification.tickerText != null
                ? notification.tickerText.toString() : null;

        int maxScoreItem = 0;
        int maxScore = Integer.MIN_VALUE;
        int maxLength = Integer.MIN_VALUE;

        size = textViewsList.size();
        double[] similaritiesToTitle = new double[size];
        double[] similaritiesToTicker = tickerText != null ? new double[size] : null;

        // pre calculations
        for (int i = 0; i < size; i++) {
            final TextView textView = textViewsList.get(i).view;

            final String text = textView.getText().toString();
            maxLength = Math.max(text.length(), maxLength);
            similaritiesToTitle[i] = service.score(text, titleText);
            if (tickerText != null) similaritiesToTicker[i] = service.score(text, tickerText);
        }

        // score calculations
        for (int i = 0; i < size; i++) {
            final TextView textView = textViews.get(i);
            if (textView == titleTextView)
                continue;
            final String text = textView.getText().toString();

            int score = 0;
            score -= 30 * similaritiesToTitle[i];
            score += 30 * similaritiesToTicker[i];
            score += 40 * text.length() / maxLength;
            score -= 40 * (text.matches("[a-zA-Z0-9[!#$%&'()*+,/\\-_\\.\\\"]]+@[a-zA-Z0-9[!#$%&'()*+,/\\-_\\\"]]+\\.[a-zA-Z0-9[!#$%&'()*+,/\\-_\\\"\\.]]+") ? 1 : 0);
            score -= 30 * (text.matches("^(\\d|)\\d:\\d\\d$") ? 1 : 0);
            score -= 30 * (textView.isClickable() ? 1 : 0);
            score -= 20 * (text.matches("^\\d+$") ? 1 : 0);
            if (textView.getTextSize() < titleTextView.getTextSize()) score += 30;

            if (score > maxScore) {
                maxScore = score;
                maxScoreItem = i;
            }

            if (Project.DEBUG) Log.d(TAG, "text=" + text + " score=" + score);
        }*/

        StringBuilder sb = new StringBuilder();
        for (ViewParentLink<TextView> tv : textViewsList) {
            sb.append(tv.view.getText().toString().replaceAll("\\s+$", ""));
            sb.append('\n');
        }
        sb.delete(sb.length() - 1, sb.length());

        return sb.toString();
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
