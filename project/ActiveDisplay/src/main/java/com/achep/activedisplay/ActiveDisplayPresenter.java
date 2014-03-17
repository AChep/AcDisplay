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
package com.achep.activedisplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.telephony.TelephonyManager;

import com.achep.activedisplay.activities.ActiveDisplayActivity;
import com.achep.activedisplay.services.LockscreenService;
import com.achep.activedisplay.utils.LogUtils;
import com.achep.activedisplay.utils.PowerUtils;

import java.util.ArrayList;

/**
 * Created by Artem on 07.03.14.
 */
public class ActiveDisplayPresenter {

    private static final String TAG = "ActiveDisplayPresenter";
    private static final String WAKE_LOCK_TAG = "AcDisplay starter.";

    public void stop(Context context) {
        if (mActivity != null
                && mActivity.hasWindowFocus()
                && mActivity.getTimeout().getTimeout() != 0
                && PowerUtils.isScreenOn(context)) {
            mActivity.lock();
        }
    }

    public void start(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (pm.isScreenOn() || ts.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            return;
        }

        // Wake up from possible deep sleep.
        //
        //           )))
        //          (((
        //        +-----+
        //        |     |]
        //        `-----'    Good morning! ^-^
        //      ___________
        //      `---------'
        pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).acquire(1000);

        LockscreenService.ignoreCurrentTurningOn();
        if (mActivity != null) mActivity.finish();
        context.startActivity(new Intent(Intent.ACTION_MAIN, null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_FROM_BACKGROUND)
                .putExtra(ActiveDisplayActivity.EXTRA_TURN_SCREEN_ON, true)
                .setClass(context, ActiveDisplayActivity.class));

        LogUtils.track();
    }

    public void kill() {
        if (mActivity != null) mActivity.finish();
    }

    /**
     * Listener to ActiveDisplayPresenter state.
     */
    public interface OnActiveDisplayStateChangedListener {
        public void OnActiveDisplayStateChanged(Activity activity);
    }

    private static ActiveDisplayPresenter sActiveDisplayPresenter;

    private ArrayList<OnActiveDisplayStateChangedListener> mListeners;
    private ActiveDisplayActivity mActivity;

    public static synchronized ActiveDisplayPresenter getInstance() {
        if (sActiveDisplayPresenter == null)
            sActiveDisplayPresenter = new ActiveDisplayPresenter();
        return sActiveDisplayPresenter;
    }

    private ActiveDisplayPresenter() {
        mListeners = new ArrayList<>(4);
    }

    public void addOnActiveDisplayStateChangedListener(OnActiveDisplayStateChangedListener listener) {
        if (!mListeners.contains(listener)) mListeners.add(listener);
    }

    public void removeOnActiveDisplayStateChangedListener(OnActiveDisplayStateChangedListener listener) {
        if (mListeners.contains(listener)) mListeners.remove(listener);
    }

    public void attachActivity(ActiveDisplayActivity activity) {
        mActivity = activity;

        for (OnActiveDisplayStateChangedListener listener : mListeners) {
            listener.OnActiveDisplayStateChanged(mActivity);
        }
    }

    public void detachActivity() {
        attachActivity(null);
    }

    public boolean isActivityAttached() {
        return mActivity != null;
    }

    public ActiveDisplayActivity getActivity() {
        return mActivity;
    }

}
