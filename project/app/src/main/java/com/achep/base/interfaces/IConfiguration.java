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
package com.achep.base.interfaces;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.Set;

/**
 * @author Artem Chepurnoy
 */
public interface IConfiguration {

    @NonNull
    IBilling getBilling();

    /**
     * @author Artem Chepurnoy
     */
    interface IBilling {

        @NonNull
        List<String> getProducts();

        /**
         * @return {@code true} if we may to show an alternative to Google Play
         * billing services, such as Bitcoin or PayPal, {@code false} otherwise.
         */
        boolean hasAlternativePaymentMethods();
    }

    @NonNull
    IHelp getHelp();

    /**
     * @author Artem Chepurnoy
     */
    interface IHelp {

        /**
         * @return a help text, to be displayed in
         * {@link com.achep.base.ui.fragments.dialogs.HelpDialog}.
         */
        @NonNull
        CharSequence getText(@NonNull Context context);

        /**
         * Called when a user has finished/probably finished reading the
         * help text.
         */
        void onUserReadHelp();
    }

    @NonNull
    IPermissions getPermissions();

    /**
     * @author Artem Chepurnoy
     */
    interface IPermissions {

        void onBuildPermissions(@NonNull Set<String> list);
    }
}
