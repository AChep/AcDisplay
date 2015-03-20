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
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.achep.acdisplay.R;
import com.achep.acdisplay.notifications.Action;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenNotification;
import com.achep.base.Device;
import com.achep.base.tests.Check;

import java.util.HashMap;

/**
 * @author Artem Chepurnoy
 * @since 3.1
 */
public class NotificationActions extends LinearLayout {

    public interface Callback {

        void onRiiStateChanged(@NonNull NotificationActions na, boolean shown);

        /**
         * Called on action's button click.
         */
        void onActionClick(@NonNull NotificationActions na,
                           @NonNull View view, @NonNull Action action);

        /**
         * Called on action's button click.
         *
         * @param remoteInput the chosen {@link android.support.v4.app.RemoteInput} to reply to
         * @param text        the text of the quick reply
         */
        void onActionClick(@NonNull NotificationActions na,
                           @NonNull View view, @NonNull Action action,
                           @NonNull RemoteInput remoteInput,
                           @NonNull CharSequence text);
    }

    /**
     * Disables the {@link #mView current action view} if the text is
     * {@link android.text.TextUtils#isEmpty(CharSequence) empty}.
     */
    protected final Textable.OnTextChangedListener mOnTextChangedListener =
            new Textable.OnTextChangedListener() {
                @Override
                public void onTextChanged(@Nullable CharSequence text) {
                    assert mView != null;
                    mView.setEnabled(!TextUtils.isEmpty(text));
                }
            };

    private final HashMap<Action, RemoteInput> mRemoteInputsMap = new HashMap<>();
    private final HashMap<View, Action> mActionsMap = new HashMap<>();
    private final OnClickListener mActionsOnClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Action action = mActionsMap.get(v);
            assert action != null;
            onActionClick(v, action);
        }
    };

    private final OnLongClickListener mActionsOnLongClick = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            sendAction(v, mActionsMap.get(v));
            hideRii();
            return true;
        }
    };

    /**
     * You know what is it for.
     */
    @Nullable
    private Callback mCallback;

    @Nullable
    private RemoteInput mRemoteInput;
    @Nullable
    private Textable mTextable;
    @Nullable
    private View mView;

    private LinearLayout.LayoutParams mLayoutParams;
    private Typeface mTypeface;

    public NotificationActions(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;
    }

    protected void onActionClick(@NonNull View view, @NonNull Action action) {
        if (isRiiShowing()) {
            if (mView != view) {
                // Ignore this click. This may happen because of
                // the animation delays.
                return;
            }
            // Send the callback with performed remote input.

            assert mRemoteInput != null;
            assert mTextable != null;
            CharSequence text = mTextable.getText();
            Check.getInstance().isFalse(TextUtils.isEmpty(text));

            assert text != null;
            sendActionWithRemoteInput(view, action, mRemoteInput, text);
            hideRii();
        } else if ((mRemoteInput = mRemoteInputsMap.get(action)) != null) {
            // Initialize and show the remote input graphic
            // user interface.

            mView = view;
            mTextable = onCreateTextable(mRemoteInput);
            mOnTextChangedListener.onTextChanged(mTextable.getText());

            if (Device.hasKitKatApi() && isLaidOut()) {
                TransitionManager.beginDelayedTransition(this);
            }

            mLayoutParams = (LayoutParams) mView.getLayoutParams();
            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mView.setLayoutParams(lp);
            // Hide all other actions
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View v = getChildAt(i);
                if (v != mView) v.setVisibility(GONE);
            }
            // Add the textable view
            addView(mTextable.getView(), 0);
            mTextable.getView().requestFocus();

            if (mCallback != null) mCallback.onRiiStateChanged(this, true);
        } else {
            sendAction(view, action);
        }
    }

    private void sendAction(@NonNull View view, @NonNull Action action) {
        if (mCallback != null) mCallback.onActionClick(this, view, action);
    }

    private void sendActionWithRemoteInput(@NonNull View view, @NonNull Action action,
                                           @NonNull RemoteInput remoteInput,
                                           @NonNull CharSequence text) {
        if (mCallback != null) mCallback.onActionClick(this, view, action, remoteInput, text);
    }

    /**
     * Returns the appropriate {@link NotificationActions.Textable} for this
     * {@link android.support.v4.app.RemoteInput remote input}.
     *
     * @see android.support.v4.app.RemoteInput#getAllowFreeFormInput()
     */
    @NonNull
    protected Textable onCreateTextable(@NonNull RemoteInput remoteInput) {
        return remoteInput.getAllowFreeFormInput()
                ? new TextableFreeForm(this, remoteInput, mOnTextChangedListener)
                : new TextableRestrictedForm(this, remoteInput, mOnTextChangedListener);
    }

    public void hideRii() {
        Check.getInstance().isInMainThread();
        if (!isRiiShowing()) return;
        assert mTextable != null;
        assert mView != null;

        removeView(mTextable.getView());
        mView.setLayoutParams(mLayoutParams);
        // Pop-up all other actions back.
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View v = getChildAt(i);
            if (v != mView) v.setVisibility(VISIBLE);
        }

        mView = null;
        mTextable = null;
        mRemoteInput = null;
        mLayoutParams = null;

        if (mCallback != null) mCallback.onRiiStateChanged(this, false);
    }

    public boolean isRiiShowing() {
        return mRemoteInput != null;
    }

    /**
     * Sets new actions.
     *
     * @param notification the host notification
     * @param actions      the actions to set
     */
    public void setActions(@Nullable OpenNotification notification, @Nullable Action[] actions) {
        Check.getInstance().isInMainThread();

        mRemoteInputsMap.clear();
        mActionsMap.clear();
        hideRii();

        if (actions == null) {
            // Free actions' container.
            removeAllViews();
            return;
        } else {
            assert notification != null;
        }

        int count = actions.length;
        View[] views = new View[count];

        // Find available views.
        int childCount = getChildCount();
        int a = Math.min(childCount, count);
        for (int i = 0; i < a; i++) {
            views[i] = getChildAt(i);
        }

        // Remove redundant views.
        for (int i = childCount - 1; i >= count; i--) {
            removeViewAt(i);
        }

        LayoutInflater inflater = null;
        for (int i = 0; i < count; i++) {
            final Action action = actions[i];
            View root = views[i];

            if (root == null) {
                // Initialize layout inflater only when we really need it.
                if (inflater == null) {
                    inflater = (LayoutInflater) getContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    assert inflater != null;
                }

                root = inflater.inflate(getActionLayoutResource(), this, false);
                root = onCreateActionView(root);
                // We need to keep all IDs unique to make
                // TransitionManager.beginDelayedTransition(viewGroup, null)
                // work correctly!
                root.setId(getChildCount() + 1);
                addView(root);
            }

            mActionsMap.put(root, action);

            int style = Typeface.NORMAL;
            root.setOnLongClickListener(null);
            if (action.intent != null) {
                root.setEnabled(true);
                root.setOnClickListener(mActionsOnClick);

                RemoteInput remoteInput = getRemoteInput(action);
                if (remoteInput != null) {
                    mRemoteInputsMap.put(action, remoteInput);
                    root.setOnLongClickListener(mActionsOnLongClick);

                    // Highlight the action
                    style = Typeface.ITALIC;
                }
            } else {
                root.setEnabled(false);
                root.setOnClickListener(null);
            }

            // Get message view and apply the content.
            TextView textView = root instanceof TextView
                    ? (TextView) root
                    : (TextView) root.findViewById(android.R.id.title);
            textView.setText(action.title);
            if (mTypeface == null) mTypeface = textView.getTypeface();
            textView.setTypeface(mTypeface, style);

            Drawable icon = NotificationUtils.getDrawable(getContext(), notification, action.icon);
            if (icon != null) icon = onCreateActionIcon(icon);

            if (Device.hasJellyBeanMR1Api()) {
                textView.setCompoundDrawablesRelative(icon, null, null, null);
            } else {
                textView.setCompoundDrawables(icon, null, null, null);
            }
        }
    }

    @NonNull
    protected View onCreateActionView(@NonNull View view) {
        return view;
    }

    @Nullable
    protected Drawable onCreateActionIcon(@NonNull Drawable icon) {
        int size = getResources().getDimensionPixelSize(R.dimen.notification_action_icon_size);
        icon = icon.mutate();
        icon.setBounds(0, 0, size, size);

        // The matrix is stored in a single array, and its treated as follows:
        // [ a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t ]
        // When applied to a color [r, g, b, a], the resulting color is computed as (after clamping)
        //   R' = a*R + b*G + c*B + d*A + e;
        //   G' = f*R + g*G + h*B + i*A + j;
        //   B' = k*R + l*G + m*B + n*A + o;
        //   A' = p*R + q*G + r*B + s*A + t;
        ColorFilter colorFilter = new ColorMatrixColorFilter(new float[]{
                0, 0, 0, 0, 255, // Red
                0, 0, 0, 0, 255, // Green
                0, 0, 0, 0, 255, // Blue
                0, 0, 0, 1, 0 //    Alpha
        });
        icon.setColorFilter(colorFilter); // force white color
        return icon;
    }

    @Nullable
    // FIXME: Which RemoteInput should I use?
    protected RemoteInput getRemoteInput(@NonNull Action action) {
        return null;
        /*
        if (action.remoteInputs == null || action.remoteInputs.length == 0) return null;
        for (RemoteInput ri : action.remoteInputs) {
            if (ri.getAllowFreeFormInput()) {
                return ri;
            }
        }
        return null;
        */
    }

    @LayoutRes
    protected int getActionLayoutResource() {
        return R.layout.notification_action;
    }

    //-- TEXTABLE -------------------------------------------------------------

    /**
     * Base class for the {@link android.support.v4.app.RemoteInput} view fields. For example:
     * the UI should provide the dropdown only if the
     * {@link android.support.v4.app.RemoteInput#getAllowFreeFormInput()} if {@code false},
     * free text form otherwise.
     *
     * @author Artem Chepurnoy
     * @see android.support.v4.app.RemoteInput
     * @since 3.1
     */
    private static abstract class Textable {

        public interface OnTextChangedListener {

            /**
             * Called on {@link #getText()} text has changed.
             */
            void onTextChanged(@Nullable CharSequence text);

        }

        @NonNull
        protected final Context mContext;
        @NonNull
        protected final RemoteInput mRemoteInput;
        @NonNull
        protected final NotificationActions mContainer;
        @NonNull
        protected final OnTextChangedListener mListener;

        public Textable(@NonNull NotificationActions container,
                        @NonNull RemoteInput remoteInput,
                        @NonNull OnTextChangedListener listener) {
            mContainer = container;
            mRemoteInput = remoteInput;
            mListener = listener;
            mContext = container.getContext();
        }

        /**
         * @return the view of this {@code Textable}.
         */
        @NonNull
        public abstract View getView();

        /**
         * @return the text the {@code Textable} is displaying.
         */
        @Nullable
        public abstract CharSequence getText();

        /**
         * Inflates a new view hierarchy from the specified xml resource. The view's root
         * is the {@link #mContainer}.
         *
         * @return the root View of the inflated hierarchy.
         */
        @NonNull
        protected final View inflate(@LayoutRes int layoutRes) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(layoutRes, mContainer, false);
        }

    }

    /**
     * @author Artem Chepurnoy
     * @since 3.1
     */
    protected static class TextableFreeForm extends Textable {

        private EditText mEditText;

        private final TextWatcher mTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* unused */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mListener.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) { /* unused */ }
        };

        public TextableFreeForm(@NonNull NotificationActions container,
                                @NonNull RemoteInput remoteInput,
                                @NonNull OnTextChangedListener listener) {
            super(container, remoteInput, listener);
            mEditText = onCreateEditText();
            mEditText.setHint(remoteInput.getLabel());
            mEditText.addTextChangedListener(mTextWatcher);
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public View getView() {
            return mEditText;
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public CharSequence getText() {
            return mEditText.getText();
        }

        @NonNull
        protected EditText onCreateEditText() {
            return (EditText) inflate(R.layout.notification_reply_free_form);
        }

    }

    /**
     * @author Artem Chepurnoy
     * @since 3.1
     */
    protected static class TextableRestrictedForm extends Textable {

        private final Spinner mSpinner;

        public TextableRestrictedForm(@NonNull NotificationActions container,
                                      @NonNull RemoteInput remoteInput,
                                      @NonNull OnTextChangedListener listener) {
            super(container, remoteInput, listener);
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                    mContext, android.R.layout.simple_spinner_dropdown_item,
                    remoteInput.getChoices());
            mSpinner = onCreateSpinner();
            mSpinner.setAdapter(adapter);
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public View getView() {
            return mSpinner;
        }

        /**
         * {@inheritDoc}
         */
        @Nullable
        @Override
        public CharSequence getText() {
            int pos = mSpinner.getSelectedItemPosition();
            return mRemoteInput.getChoices()[pos];
        }

        @NonNull
        protected Spinner onCreateSpinner() {
            return (Spinner) inflate(R.layout.notification_reply_restricted_form);
        }

    }

}
