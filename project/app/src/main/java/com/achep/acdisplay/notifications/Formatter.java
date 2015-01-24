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
package com.achep.acdisplay.notifications;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.R;
import com.achep.base.utils.CsUtils;
import com.achep.base.utils.NullUtils;
import com.achep.base.utils.Operator;

/**
 * @author Artem Chepurnoy
 */
public class Formatter {

    private static final String TAG = "Styler";

    private int mPrivacyMode;

    Formatter() { /* empty */ }

    /**
     * @see #hasHiddenContent()
     */
    void setPrivacyMode(int privacy) {
        mPrivacyMode = privacy;
    }

    /**
     * @return {@code true} if {@link Formatter} is in the
     * secure mode and may return not real notification's data.
     * @see #setPrivacyMode(int)
     */
    private boolean hasHiddenContent() {
        return Operator.bitAnd(mPrivacyMode, Config.PRIVACY_HIDE_CONTENT_MASK);
    }

    private boolean hasHiddenActions() {
        return Operator.bitAnd(mPrivacyMode, Config.PRIVACY_HIDE_ACTIONS_MASK);
    }

    @NonNull
    public Data get(@NonNull Context context, @NonNull OpenNotification notification) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean secure = km.isKeyguardSecure() && km.isKeyguardLocked();
        boolean hiddenContent = secure && hasHiddenContent();
        boolean hiddenActions = secure && hasHiddenActions();

        // Get the title
        CharSequence title = NullUtils.whileNotNull(
                notification.titleBigText,
                notification.titleText);

        // Get the subtitle
        CharSequence subtitle;
        if (hiddenContent) {
            PackageManager pm = context.getPackageManager();
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(notification.getPackageName(), 0);
                subtitle = pm.getApplicationLabel(appInfo);
            } catch (PackageManager.NameNotFoundException e) {
                subtitle = null;
            }
        } else {
            subtitle = CsUtils.join(" ", notification.subText, notification.infoText);
        }

        // Get message text
        CharSequence[] messages;
        if (hiddenContent) {
            Resources res = context.getResources();
            CharSequence message = res.getString(R.string.privacy_mode_hidden_content);
            SpannableString spannableMessage = new SpannableString(message);
            spannableMessage.setSpan(new StyleSpan(Typeface.ITALIC), 0, spannableMessage.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            messages = new CharSequence[]{spannableMessage};
        } else {
            CharSequence message = NullUtils.whileNotNull(
                    notification.messageBigText,
                    notification.messageText);
            messages = (CharSequence[]) NullUtils.whileNotNull(
                    notification.messageTextLines,
                    new CharSequence[]{message});
        }

        // Get actions
        Action[] actions;
        if (hiddenActions) {
            actions = null;
        } else {
            actions = notification.getActions();
        }

        return new Data(title, subtitle, messages, actions);
    }

    public static class Data {

        @Nullable
        public CharSequence title;
        @Nullable
        public CharSequence subtitle;
        @Nullable
        public CharSequence[] messages;
        @Nullable
        public Action[] actions;

        private Data(@Nullable CharSequence title,
                     @Nullable CharSequence subtitle,
                     @Nullable CharSequence[] messages,
                     @Nullable Action[] actions) {
            this.title = title;
            this.subtitle = subtitle;
            this.messages = messages;
            this.actions = actions;
        }

    }

}
