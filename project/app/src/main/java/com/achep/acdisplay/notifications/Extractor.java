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
package com.achep.acdisplay.notifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.base.Device;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by Artem on 04.03.14.
 */
final class Extractor {

    private static final String TAG = "Extractor";

    /**
     * Removes all kinds of multiple spaces from given string.
     */
    @Nullable
    private static String removeSpaces(@Nullable CharSequence cs) {
        if (cs == null) return null;
        String string = cs instanceof String
                ? (String) cs : cs.toString();
        return string
                .replaceAll("(\\s+$|^\\s+)", "")
                .replaceAll("\n+", "\n");
    }

    /**
     * Removes both {@link ForegroundColorSpan} and {@link BackgroundColorSpan} from given string.
     */
    @Nullable
    private static CharSequence removeColorSpans(@Nullable CharSequence cs) {
        if (cs == null) return null;
        if (cs instanceof Spanned) {
            cs = new SpannableStringBuilder(cs);
        }
        if (cs instanceof Spannable) {
            CharacterStyle[] styles;
            Spannable spanned = (Spannable) cs;
            styles = spanned.getSpans(0, spanned.length(), TextAppearanceSpan.class);
            for (CharacterStyle style : styles) spanned.removeSpan(style);
            styles = spanned.getSpans(0, spanned.length(), ForegroundColorSpan.class);
            for (CharacterStyle style : styles) spanned.removeSpan(style);
            styles = spanned.getSpans(0, spanned.length(), BackgroundColorSpan.class);
            for (CharacterStyle style : styles) spanned.removeSpan(style);
        }
        return cs;
    }

    @SuppressLint("InlinedApi")
    public void loadTexts(@NonNull Context context, @NonNull OpenNotification n) {
        final Notification notification = n.getNotification();
        final Bundle extras = getExtras(notification);

        if (extras != null) loadFromExtras(n, extras);
        if (TextUtils.isEmpty(n.titleText)
                && TextUtils.isEmpty(n.titleBigText)
                && TextUtils.isEmpty(n.messageText)
                && n.messageTextLines == null) {
            loadFromView(context, n);
        }
    }

    /**
     * Gets a bundle with additional data from notification.
     */
    @Nullable
    @SuppressLint("NewApi")
    private Bundle getExtras(@NonNull Notification notification) {
        if (Device.hasKitKatApi()) {
            return notification.extras;
        }

        // Access extras using reflections.
        try {
            Field field = notification.getClass().getDeclaredField("extras");
            field.setAccessible(true);
            return (Bundle) field.get(notification);
        } catch (Exception e) {
            Log.w(TAG, "Failed to access extras on Jelly Bean.");
            return null;
        }
    }

    @Nullable
    private CharSequence[] doIt(@Nullable CharSequence[] lines) {
        if (lines != null) {
            // Filter empty lines.
            ArrayList<CharSequence> list = new ArrayList<>();
            for (CharSequence msg : lines) {
                msg = removeSpaces(msg);
                if (!TextUtils.isEmpty(msg)) {
                    list.add(removeColorSpans(msg));
                }
            }

            // Create new array.
            if (list.size() > 0) {
                return list.toArray(new CharSequence[list.size()]);
            }
        }
        return null;
    }

    //-- LOADING FROM EXTRAS --------------------------------------------------

    /**
     * Loads all possible texts from given {@link Notification#extras extras}.
     *
     * @param extras extras to load from
     */
    @SuppressLint("InlinedApi")
    private void loadFromExtras(@NonNull OpenNotification n, @NonNull Bundle extras) {
        n.titleBigText = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        n.titleText = extras.getCharSequence(Notification.EXTRA_TITLE);
        n.infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
        n.subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        n.summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
        n.messageBigText = removeColorSpans(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        n.messageText = removeColorSpans(extras.getCharSequence(Notification.EXTRA_TEXT));

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        n.messageTextLines = doIt(lines);
    }

    //-- LOADING FROM VIEWS ---------------------------------------------------

    private void loadFromView(@NonNull Context context, @NonNull OpenNotification n) {
        ViewGroup view;
        {
            final Context contextNotify = NotificationUtils.createContext(context, n);
            if (contextNotify == null) return;

            final Notification notification = n.getNotification();
            final RemoteViews rvs = notification.bigContentView == null
                    ? notification.contentView
                    : notification.bigContentView;

            // Try to load the view from remote views.
            LayoutInflater inflater = (LayoutInflater) contextNotify.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            try {
                view = (ViewGroup) inflater.inflate(rvs.getLayoutId(), null);
                rvs.reapply(contextNotify, view);
            } catch (Exception e) {
                return;
            }
        }

        ArrayList<TextView> textViews = new RecursiveFinder<>(TextView.class).expand(view);
        removeClickableViews(textViews);
        removeSubtextViews(context, textViews);
        removeActionViews(n.getActions(), textViews);

        // No views
        if (textViews.size() == 0)
            return;

        TextView title = findTitleTextView(textViews);
        textViews.remove(title); // no need of title view anymore
        n.titleText = title.getText();

        // No views
        if (textViews.size() == 0)
            return;

        // Pull all other texts and merge them.
        int length = textViews.size();
        CharSequence[] messages = new CharSequence[length];
        for (int i = 0; i < length; i++) messages[i] = textViews.get(i).getText();
        n.messageTextLines = doIt(messages);
    }

    private void removeActionViews(@Nullable Action[] actions,
                                   @NonNull ArrayList<TextView> textViews) {
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

    private void removeClickableViews(@NonNull ArrayList<TextView> textViews) {
        for (int i = textViews.size() - 1; i >= 0; i--) {
            TextView child = textViews.get(i);
            if (child.isClickable() || child.getVisibility() != View.VISIBLE) {
                textViews.remove(i);
                break;
            }
        }
    }

    private void removeSubtextViews(@NonNull Context context,
                                    @NonNull ArrayList<TextView> textViews) {
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

    @NonNull
    private TextView findTitleTextView(@NonNull ArrayList<TextView> textViews) {
        // The idea is that title text is the
        // largest one.
        TextView largest = null;
        for (TextView textView : textViews) {
            if (largest == null || textView.getTextSize() > largest.getTextSize()) {
                largest = textView;
            }
        }
        assert largest != null; // cause the count of views is always >= 1
        return largest;
    }

    private static class RecursiveFinder<T extends View> {

        private final ArrayList<T> list;
        private final Class<T> clazz;

        public RecursiveFinder(@NonNull Class<T> clazz) {
            this.list = new ArrayList<>();
            this.clazz = clazz;
        }

        public ArrayList<T> expand(@NonNull ViewGroup viewGroup) {
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
