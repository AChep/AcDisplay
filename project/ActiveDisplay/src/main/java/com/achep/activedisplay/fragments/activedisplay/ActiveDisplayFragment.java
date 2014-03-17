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
package com.achep.activedisplay.fragments.activedisplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextClock;
import android.widget.TextView;

import com.achep.activedisplay.Keys;
import com.achep.activedisplay.Operator;
import com.achep.activedisplay.R;
import com.achep.activedisplay.Timeout;
import com.achep.activedisplay.activities.KeyguardActivity;
import com.achep.activedisplay.blacklist.Blacklist;
import com.achep.activedisplay.blacklist.activities.BlacklistActivity;
import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.ViewUtils;
import com.achep.activedisplay.widgets.NotificationRadioButton;

import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

/**
 * This is main fragment of ActiveDisplay app.
 */
public class ActiveDisplayFragment extends MyFragment implements
        CompoundButton.OnCheckedChangeListener,
        Timeout.OnTimeoutEventListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "ActiveDisplayFragment";

    private static final int REFRESH_UI_NOTIFICATION = 1;
    private static final int REFRESH_UI_NOTIFICATION_LIST = 2;
    private static final int REFRESH_UI_TIMEOUT = 4;

    private TextClock mDateView;
    private HoloCircularProgressBar mHandleTimeoutProgressBar;
    private ImageView mHandleIconImageView;
    private TextView mNotificationNumber;
    private RadioGroup mRadioGroup;

    private View mOverflowView;
    private PopupMenu mPopupMenu;

    private OnEventListener mOnEventListener;

    private NotificationPresenter mPresenter;
    private NotificationListener mNotificationListener = new NotificationListener();
    private Blacklist mBlacklist;
    private boolean mBroadcasting;

    private Timeout mTimeout;

    public interface OnEventListener {

        public boolean onTouchHandleEvent(View view, MotionEvent event);

    }

    public void setActiveDisplayActionsListener(OnEventListener listener) {
        mOnEventListener = listener;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        assert activity != null;

        mBlacklist = Blacklist.getInstance(activity);
        mPresenter = NotificationPresenter.getInstance(getActivity());
        mPresenter.addOnNotificationListChangedListener(mNotificationListener);
        updateNotification();
        updateNotificationList();

        setTimeoutPresenter(mTimeout); // may be null

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.registerOnSharedPreferenceChangeListener(this);
        updateDateVisibility(prefs);
    }

    @Override
    public void onDestroyView() {
        //mBlacklist.removeOnSharedListChangedListener(mBlacklistListener);
        mPresenter.removeOnNotificationListChangedListener(mNotificationListener);
        if (mTimeout != null) {
            mTimeout.removeListener(this);
            mTimeout = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case Keys.Settings.SHOW_DATE:
                updateDateVisibility(prefs);
                break;
        }
    }

    private void updateDateVisibility(SharedPreferences prefs) {
        ViewUtils.setVisible(mDateView, prefs.getBoolean(Keys.Settings.SHOW_DATE, true));
    }

    @Override
    protected void handleTodoList(int v) {
        if (Operator.bitandCompare(v, REFRESH_UI_NOTIFICATION))
            updateNotification();
        if (Operator.bitandCompare(v, REFRESH_UI_NOTIFICATION_LIST))
            updateNotificationList();
        if (Operator.bitandCompare(v, REFRESH_UI_TIMEOUT))
            refreshTimeout();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_ad, container, false);

        assert root != null;
        final View handle = root.findViewById(R.id.handle);
        handle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                boolean handled = true;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        // Keep current selected notification to display
                        // for all the time of this touch gesture.
                        mPresenter.lockSelectedNotification();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        mPresenter.unlockSelectedNotification();
                        break;
                    default:
                        handled = false;
                        break;
                }
                return mOnEventListener != null && mOnEventListener.onTouchHandleEvent(view, event) || handled;
            }
        });

        // handle
        mHandleTimeoutProgressBar = (HoloCircularProgressBar) handle.findViewById(R.id.timeout);
        mHandleIconImageView = (ImageView) handle.findViewById(R.id.icon);
        mNotificationNumber = (TextView) handle.findViewById(R.id.count);

        // multi notifications
        mRadioGroup = (RadioGroup) root.findViewById(R.id.radios);

        // blacklist
        mOverflowView = root.findViewById(R.id.overflow);
        mOverflowView.setOnClickListener(new View.OnClickListener() {

            private final OverflowClickListener onMenuItemClickListener = new OverflowClickListener();
            private final PopupMenu.OnDismissListener onDismissListener = new PopupMenu.OnDismissListener() {
                @Override
                public void onDismiss(PopupMenu popupMenu) {
                    mPopupMenu = null;
                }
            };

            @SuppressWarnings("ConstantConditions")
            @Override
            public void onClick(View view) {
                OpenStatusBarNotification notification = mPresenter.getSelectedNotification();
                if (notification == null) {
                    return;
                }

                Context context = view.getContext();
                mPopupMenu = new PopupMenu(context, view);
                mPopupMenu.inflate(R.menu.ad_overflow);

                mPopupMenu.setOnDismissListener(onDismissListener);
                mPopupMenu.setOnMenuItemClickListener(onMenuItemClickListener);
                mPopupMenu.show();
            }
        });

        mDateView = (TextClock) root.findViewById(R.id.date);

        return root;
    }

    private void updateNotification() {
        if (tryPutTodo(REFRESH_UI_NOTIFICATION)) {
            return;
        }

        final OpenStatusBarNotification notification = mPresenter.getSelectedNotification();
        final boolean emptyNotification = notification == null;

        // Update overflow & notification icons
        ViewUtils.setVisible(mOverflowView, !emptyNotification, View.INVISIBLE);
        mHandleIconImageView.setImageDrawable(emptyNotification
                ? getResources().getDrawable(R.drawable.stat_unlock)
                : notification.getSmallIcon(getActivity()));

        // Update notification number
        final int number = emptyNotification ? 0 : notification.getNotificationData().number;
        ViewUtils.setVisible(mNotificationNumber, number > 0);
        mNotificationNumber.setText(Integer.toString(number));
    }

    private void updateNotificationList() {
        if (tryPutTodo(REFRESH_UI_NOTIFICATION_LIST)) return;

        boolean visible = Helper.updateNotificationList(mPresenter, mRadioGroup,
                R.layout.radio_notification_icon, getActivity().getLayoutInflater());
        if (!visible) return;

        // Check current notification
        final int size = mPresenter.getList().size();
        final OpenStatusBarNotification notification = mPresenter.getSelectedNotification();
        for (int i = 0; i < size; i++) {
            NotificationRadioButton nrb = (NotificationRadioButton) mRadioGroup.getChildAt(i);

            assert nrb != null;
            nrb.setOnCheckedChangeListener(this);
            if (notification == nrb.getNotification()) {
                mBroadcasting = true;
                nrb.setChecked(true);
                mBroadcasting = false;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (!checked || mBroadcasting) {
            return;
        }

        NotificationRadioButton nrb = (NotificationRadioButton) compoundButton;
        mPresenter.setSelectedNotification(nrb.getNotification());
    }

    // //////////////////////////////////////////
    // ///////////// -- TIMEOUT -- //////////////
    // //////////////////////////////////////////

    public void setTimeoutPresenter(Timeout tp) {
        if (mTimeout != null) {
            mTimeout.removeListener(this);
        }

        mTimeout = tp;
        if (mTimeout != null) {
            mTimeout.addListener(this);
        }

        refreshTimeout();
    }

    @Override
    public void onTimeoutEvent(int event) {
        refreshTimeout();
    }

    private void refreshTimeout() {
        if (tryPutTodo(REFRESH_UI_TIMEOUT)) return;

        mHandleTimeoutProgressBar.cancelAnimateProgress();
        if (mTimeout == null) {
            mHandleTimeoutProgressBar.setProgress(1);
        } else if (mTimeout.getRemainingTime() > 0) {
            mHandleTimeoutProgressBar.animateProgressFromOne(mTimeout.getRemainingTime());
        } else {
            mHandleTimeoutProgressBar.setProgress(0);
        }
    }

    // //////////////////////////////////////////
    // ///////////// -- CLASSES -- //////////////
    // //////////////////////////////////////////

    @SuppressWarnings("ConstantConditions")
    private class NotificationListener extends NotificationPresenter.SimpleOnNotificationListChangedListener {

        @Override
        public void onNotificationEvent(NotificationPresenter nm,
                                        OpenStatusBarNotification notification,
                                        final int event) {
            super.onNotificationEvent(nm, notification, event);
            switch (event) {
                case SELECTED:
                    updateNotification();

                    if (mPopupMenu != null) {
                        mPopupMenu.dismiss();
                    }
                    break;
                default:
                    updateNotificationList();
                    break;
            }
        }
    }

    private class OverflowClickListener implements PopupMenu.OnMenuItemClickListener {

        private Runnable mLaunchBlacklist = new Runnable() {
            @Override
            public void run() {
                // TODO: Launch notification's app settings
                startActivity(new Intent(getActivity(), BlacklistActivity.class));
            }
        };

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_launch_blacklist:
                    Activity activity = getActivity();
                    if (activity instanceof KeyguardActivity) {
                        KeyguardActivity keyguard = (KeyguardActivity) activity;
                        keyguard.unlock(mLaunchBlacklist);
                    } else {
                        mLaunchBlacklist.run();
                    }
                    break;
                default:
                    return false;
            }
            return true;
        }

    }
}
