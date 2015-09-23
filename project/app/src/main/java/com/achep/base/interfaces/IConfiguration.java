package com.achep.base.interfaces;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;

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
}
