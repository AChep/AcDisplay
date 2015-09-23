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
package com.achep.base;

import android.support.annotation.NonNull;

import com.achep.acdisplay.BuildConfig;

/**
 * Contains a number of build constants mostly redirected from
 * the {@link BuildConfig build config}.
 *
 * @author Artem Chepurnoy
 */
/* The first class of the AcDisplay, dated by 30.12.2013 */
@SuppressWarnings("PointlessBooleanExpression")
public final class Build {

    /**
     * Defines if the current build <b>debug</b> or not.
     */
    public static final boolean DEBUG = true &&
            BuildConfig.MY_DEBUG;

    /**
     * {@code true}, to force-enable the power-saving mode, {@code false}
     * to check the state of the system.
     */
    public static final boolean DEBUG_POWER_SAVING = false && DEBUG;

    /**
     * The timestamp of build in {@code EEE MMMM dd HH:mm:ss zzz yyyy} format.
     */
    @NonNull
    public static final String TIME_STAMP =
            BuildConfig.MY_TIME_STAMP;

    /**
     * Encrypted public key of my dev account in Google Play. The key is encrypted on
     * build and should not be hardcoded.
     *
     * @see #GOOGLE_PLAY_PUBLIC_KEY_SALT
     */
    @NonNull
    public static final String GOOGLE_PLAY_PUBLIC_KEY_ENCRYPTED =
            BuildConfig.MY_GOOGLE_PLAY_PUBLIC_KEY;

    /**
     * Salt for {@link #GOOGLE_PLAY_PUBLIC_KEY_ENCRYPTED}. The salt is generated on
     * build and should not be hardcoded.
     *
     * @see #GOOGLE_PLAY_PUBLIC_KEY_ENCRYPTED
     */
    @NonNull
    public static final String GOOGLE_PLAY_PUBLIC_KEY_SALT =
            BuildConfig.MY_GOOGLE_PLAY_PUBLIC_KEY_SALT;

    /**
     * Encrypted key that is used to store logs on storage.
     *
     * @see #LOG_KEY_SALT
     */
    @NonNull
    public static final String LOG_KEY_ENCRYPTED =
            BuildConfig.MY_LOG_KEY;

    /**
     * Salt for {@link #LOG_KEY_ENCRYPTED}. The salt is generated on
     * build and should not be hardcoded.
     *
     * @see #LOG_KEY_ENCRYPTED
     */
    @NonNull
    public static final String LOG_KEY_SALT =
            BuildConfig.MY_LOG_KEY_SALT;

    /**
     * {@code true} to encrypt logs before saving, {@code false}
     * to write a plain text.
     */
    public static final boolean ENCRYPT_LOGS = false && DEBUG;

    /**
     * The official e-mail for tons of complains, billions of
     * "How to uninstall?" screams and one or two useful emails.
     */
    @NonNull
    public static final String SUPPORT_EMAIL =
            "support@artemchep.com";

}
