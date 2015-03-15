/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.acdisplay.ui.widgets.notification;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.base.tests.Check;

import java.util.Arrays;

/**
 * @author Artem Chepurnoy
 * @since 3.1
 */
public class NotificationMessages extends LinearLayout {

    private CharSequence[] mMessages;
    private boolean mHighlightMessages;
    private int mMaxLines;

    public NotificationMessages(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationMessages(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NotificationMessages);
        setMaxLines(a.getInt(R.styleable.NotificationMessages_maxLines, 6));
        setHighlightMessagesEnabled(a.getBoolean(R.styleable.NotificationMessages_highlightMessages, true));
        a.recycle();
    }

    public void setMaxLines(int maxLines) {
        mMaxLines = maxLines;
        updateMessages();
    }

    public void setHighlightMessagesEnabled(boolean enabled) {
        mHighlightMessages = enabled;
        updateMessages();
    }

    protected void updateMessages() {
        if (mMessages != null) setMessages(mMessages);
    }

    /**
     * @param messages an array of non-empty messages.
     */
    public void setMessages(final @Nullable CharSequence[] messages) {
        mMessages = messages;

        if (messages == null) {
            // Free messages' container.
            removeAllViews();
            return;
        }

        int freeLines = mMaxLines;
        final int length = messages.length;
        final int viewCount = Math.min(length, freeLines);
        final int[] viewMaxLines = new int[length];
        if (freeLines > length) { // We can reserve more than one line per message

            // Initial setup.
            Arrays.fill(viewMaxLines, 1);
            freeLines -= length;

            // Build list of lengths, so we don't have
            // to recalculate it every time.
            int[] msgLengths = new int[length];
            for (int i = 0; i < length; i++) {
                assert messages[i] != null;
                msgLengths[i] = messages[i].length();
            }

            while (freeLines > 0) {
                int pos = 0;
                float a = 0;
                for (int i = 0; i < length; i++) {
                    final float k = (float) msgLengths[i] / viewMaxLines[i];
                    if (k > a) {
                        a = k;
                        pos = i;
                    }
                }
                viewMaxLines[pos]++;
                freeLines--;
            }
        } else {
            // Show first messages.
            for (int i = 0; freeLines > 0; freeLines--, i++) {
                viewMaxLines[i] = 1;
            }
        }

        View[] views = new View[viewCount];

        // Find available views.
        int childCount = getChildCount();
        for (int i = Math.min(childCount, viewCount) - 1; i >= 0; i--) {
            views[i] = getChildAt(i);
        }

        // Remove redundant views.
        for (int i = childCount - 1; i >= viewCount; i--) {
            removeViewAt(i);
        }

        boolean highlightFirstLetter = mHighlightMessages && viewCount > 1;

        LayoutInflater inflater = null;
        for (int i = 0; i < viewCount; i++) {
            View root = views[i];

            if (root == null) {
                // Initialize layout inflater only when we really need it.
                if (inflater == null) {
                    inflater = (LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                }

                root = inflater.inflate(
                        getMessageLayoutResource(),
                        this, false);
                // FIXME: ?
                // We need to keep all IDs unique to make
                // TransitionManager#beginDelayedTransition(ViewGroup)
                // work correctly!
                root.setId(getChildCount() + 1);
                addView(root);
            }

            Check.getInstance().isTrue(messages[i].length() != 0);

            final CharSequence text;
            final char char_ = messages[i].charAt(0);
            if (highlightFirstLetter && (Character.isLetter(char_) || Character.isDigit(char_))) {
                SpannableString spannable = new SpannableString(messages[i]);
                spannable.setSpan(new UnderlineSpan(), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text = spannable;
            } else {
                text = messages[i];
            }

            // Get message view and apply the content.
            TextView textView = root instanceof TextView
                    ? (TextView) root
                    : (TextView) root.findViewById(android.R.id.message);
            textView.setMaxLines(viewMaxLines[i]);
            textView.setText(text);
        }
    }

    @LayoutRes
    protected int getMessageLayoutResource() {
        return R.layout.notification_message;
    }

}
