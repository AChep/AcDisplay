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
package com.achep.activedisplay.fragments.activedisplay;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;

import com.achep.activedisplay.Operator;
import com.achep.activedisplay.Project;
import com.achep.activedisplay.R;
import com.achep.activedisplay.Timeout;
import com.achep.activedisplay.blacklist.SharedList;
import com.achep.activedisplay.fragments.MyFragment;
import com.achep.activedisplay.notifications.Blacklist;
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
        Timeout.OnTimeoutEventListener {

    private static final String TAG = "ActiveDisplayFragment";

    private static final int REFRESH_UI_NOTIFICATION = 1;
    private static final int REFRESH_UI_NOTIFICATION_LIST = 2;
    private static final int REFRESH_UI_NOTIFICATION_STATE_INDICATOR = 4;
    private static final int REFRESH_UI_TIMEOUT = 8;

    private HoloCircularProgressBar mHandleTimeoutProgressBar;
    private ImageView mHandleIconImageView;
    private RadioGroup mRadioGroup;
    private View mBlacklistedIndicator;

    private View mOverflowView;
    private PopupMenu mPopupMenu;

    private OnEventListener mOnEventListener;

    private NotificationPresenter mPresenter;
    private NotificationListener mNotificationListener = new NotificationListener();
    private Blacklist mBlacklist;
    private BlacklistListener mBlacklistListener = new BlacklistListener();
    private boolean mBroadcasting;

    private Timeout mTimeout;

    public interface OnEventListener {

        public boolean onTouchHandleEvent(View view, MotionEvent event);

    }

    public void setActiveDisplayActionsListener(OnEventListener listener) {
        mOnEventListener = listener;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mBlacklist = Blacklist.getInstance(getActivity());
        mPresenter = NotificationPresenter.getInstance();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mBlacklist.addOnSharedListChangedListener(mBlacklistListener);
        synchronized (mPresenter.monitor) {
            mPresenter.addOnNotificationListChangedListener(mNotificationListener);
            updateNotification();
            updateNotificationList();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBlacklist.removeOnSharedListChangedListener(mBlacklistListener);

        synchronized (mPresenter.monitor) {
            mPresenter.removeOnNotificationListChangedListener(mNotificationListener);
            mPresenter = null;
        }
        if (mTimeout != null) mTimeout.removeListener(this);
    }

    @Override
    protected void handleTodoList(int v) {
        if (Operator.bitandCompare(v, REFRESH_UI_NOTIFICATION))
            updateNotification();
        if (Operator.bitandCompare(v, REFRESH_UI_NOTIFICATION_LIST))
            updateNotificationList();
        if (Operator.bitandCompare(v, REFRESH_UI_NOTIFICATION_STATE_INDICATOR))
            updateNotificationStateIndicator();
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
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        synchronized (mPresenter.monitor) {

                            // Keep current selected notification to display
                            // for all the time of this touch gesture.
                            mPresenter.lockSelectedNotification();
                        }
                    case MotionEvent.ACTION_UP:
                        synchronized (mPresenter.monitor) {
                            mPresenter.unlockSelectedNotification();
                        }
                        break;
                }
                return mOnEventListener != null && mOnEventListener.onTouchHandleEvent(view, event);
            }
        });

        // handle
        mHandleTimeoutProgressBar = (HoloCircularProgressBar) handle.findViewById(R.id.timeout);
        mHandleIconImageView = (ImageView) handle.findViewById(R.id.icon);

        // multi notifications
        mRadioGroup = (RadioGroup) root.findViewById(R.id.radios);

        // blacklist
        mBlacklistedIndicator = root.findViewById(R.id.blacklisted);
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
                synchronized (mPresenter.monitor) {
                    OpenStatusBarNotification notification = mPresenter.getSelectedNotification();
                    if (notification == null) {
                        return;
                    }

                    Context context = view.getContext();
                    mPopupMenu = new PopupMenu(context, view);
                    mPopupMenu.inflate(R.menu.ad_overflow);

                    // Manage available options.
                    // TODO: Move it to OpenNotification class (?)
                    Menu m = mPopupMenu.getMenu();
                    boolean isBlacklisted = notification.isBlacklisted(context);
                    m.findItem(R.id.action_add_to_blacklist).setVisible(!isBlacklisted);
                    m.findItem(R.id.action_remove_from_blacklist).setVisible(isBlacklisted);

                    mPopupMenu.setOnDismissListener(onDismissListener);
                    mPopupMenu.setOnMenuItemClickListener(onMenuItemClickListener);
                    mPopupMenu.show();
                }
            }
        });
        return root;
    }

    private void updateNotification() {
        if (tryPutTodo(REFRESH_UI_NOTIFICATION)) return;

        updateNotificationStateIndicator();

        synchronized (mPresenter.monitor) {
            OpenStatusBarNotification notification = mPresenter.getSelectedNotification();
            boolean emptyNotification = notification == null;

            ViewUtils.setVisible(mOverflowView, !emptyNotification && Project.DEBUG, View.INVISIBLE);
            mHandleIconImageView.setImageDrawable(emptyNotification
                    ? getResources().getDrawable(R.drawable.stat_unlock)
                    : notification.getSmallIcon(getActivity()));
        }
    }

    private void updateNotificationStateIndicator() {
        if (tryPutTodo(REFRESH_UI_NOTIFICATION_STATE_INDICATOR)) return;

        synchronized (mPresenter.monitor) {
            OpenStatusBarNotification notification = mPresenter.getSelectedNotification();
            ViewUtils.setVisible(mBlacklistedIndicator,
                    notification != null && mBlacklist.contains(notification) && Project.DEBUG,
                    View.GONE);
        }
    }

    private void updateNotificationList() {
        if (tryPutTodo(REFRESH_UI_NOTIFICATION_LIST)) return;

        synchronized (mPresenter.monitor) {
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
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (!checked || mBroadcasting) {
            return;
        }

        NotificationRadioButton nrb = (NotificationRadioButton) compoundButton;
        synchronized (mPresenter.monitor) {
            mPresenter.setSelectedNotification(nrb.getNotification());
        }
    }

    // //////////////////////////////////////////
    // ///////////// -- TIMEOUT -- //////////////
    // //////////////////////////////////////////

    public void setTimeoutPresenter(Timeout tp) {
        if (mTimeout != null) {
            mTimeout.removeListener(this);
        }

        mTimeout = tp;
        mTimeout.addListener(this);

        refreshTimeout();
    }

    @Override
    public void onTimeoutEvent(int event) {
        refreshTimeout();
    }

    private void refreshTimeout() {
        if (tryPutTodo(REFRESH_UI_TIMEOUT)) return;

        mHandleTimeoutProgressBar.cancelAnimateProgress();
        if (mTimeout.getRemainingTime() > 0) {
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
        // running on wrong thread
        public void onNotificationEvent(NotificationPresenter nm,
                                        OpenStatusBarNotification notification,
                                        final int event) {
            super.onNotificationEvent(nm, notification, event);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
            });
        }
    }

    private class BlacklistListener implements SharedList.OnSharedListChangedListener<String> {

        @Override
        // running on ui thread
        public void onPut(String object) {
            updateNotificationStateIndicator();
        }

        @Override
        // running on ui thread
        public void onRemoved(String object) {
            updateNotificationStateIndicator();
        }
    }

    private class OverflowClickListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            synchronized (mPresenter.monitor) {
                switch (item.getItemId()) {
                    case R.id.action_add_to_blacklist:
                        mBlacklist.put(getActivity(), mPresenter.getSelectedNotification());
                        break;
                    case R.id.action_remove_from_blacklist:
                        mBlacklist.remove(getActivity(), mPresenter.getSelectedNotification());
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }

    }
}
