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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.Action;
import com.achep.acdisplay.notifications.NotificationData;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by Artem on 04.03.14.
 */
public final class Extractor {

    private static final String TAG = "Extractor";

    @SuppressLint("NewApi")
    private Bundle getExtras(Notification notification) {
        if (Device.hasKitKatApi()) {
            return notification.extras;
        }

        // Access extras using reflections.
        try {
            Field field = Notification.class.getDeclaredField("extras");
            field.setAccessible(true);
            return (Bundle) field.get(notification);
        } catch (Exception e) {
            Log.w(TAG, "Failed to access extras on Jelly Bean.");
            return null;
        }
    }

    @SuppressLint("InlinedApi")
    public NotificationData loadTexts(Context context,
                                      OpenNotification openNotification,
                                      NotificationData data) {
        // Loop is here only to provide useful break; function.
        //noinspection LoopStatementThatDoesntLoop
        while (true) {
            final Notification n = openNotification.getNotification();
            final Bundle extras = getExtras(n);

            // If extras are available - try to load data from it.
            if (extras != null) {
                data.titleText = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
                if (data.titleText == null) {
                    data.titleText = extras.getCharSequence(Notification.EXTRA_TITLE);
                }

                data.infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
                data.subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                data.summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
                data.messageText = Utils.removeSpaces(extras.getCharSequence(Notification.EXTRA_TEXT));

                CharSequence[] messageTextLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
                if (messageTextLines != null) {
                    ArrayList<CharSequence> messageTextList = new ArrayList<>();
                    for (CharSequence msg : messageTextLines) {
                        msg = Utils.removeSpaces(msg);
                        if (!TextUtils.isEmpty(msg)) {
                            messageTextList.add(msg);
                        }
                    }
                    messageTextLines = messageTextList.toArray(new CharSequence[messageTextList.size()]);
                    data.messageTextLines = messageTextLines;
                }

              /*  if (!TextUtils.isEmpty(data.titleText) || !TextUtils.isEmpty(data.getLargeMessage())) {
                    break;
                }*/
            }

            // Parse views to get title and message text.

            final Context contextNotify = NotificationUtils.createContext(context, openNotification);
            final RemoteViews rvs = n.bigContentView == null ? n.contentView : n.bigContentView;

            if (rvs == null) {
                break;
            }

            LayoutInflater inflater = (LayoutInflater) contextNotify.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup view = (ViewGroup) inflater.inflate(rvs.getLayoutId(), null);

            if (view == null) {
                break;
            }

            try {
                rvs.reapply(contextNotify, view);
            } catch (Exception e) {
                break;
            }

            ArrayList<TextView> textViews = new RecursiveFinder<>(TextView.class).expand(view);
            removeClickableViews(textViews);
            removeSubtextViews(context, textViews);
            removeActionViews(data.actions, textViews);

            // There're no views present.
            if (textViews.size() == 0)
                break;

            TextView title = findTitleTextView(textViews);
            textViews.remove(title); // no need of title view anymore
            data.titleText = title.getText();

            // There're no views present.
            if (textViews.size() == 0)
                break;

            int length = textViews.size();
            CharSequence[] messages = new CharSequence[length];
            for (int i = 0; i < length; i++) {
                messages[i] = textViews.get(i).getText();
            }

            data.messageText = Utils.mergeLargeMessage(messages);
            break;
        }

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

    private void removeActionViews(Action[] actions, ArrayList<TextView> textViews) {
        if (actions == null) {
            return;
        }

        for (Action action : actions) {
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
