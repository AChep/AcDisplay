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
package com.achep.acdisplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.achep.acdisplay.acdisplay.AcDisplayActivity;
import com.achep.acdisplay.activities.KeyguardActivity;
import com.achep.acdisplay.utils.PowerUtils;

import java.util.ArrayList;

/**
 * Created by Artem on 07.03.14.
 */
public class Presenter {

    private static final String TAG = "AcDisplayPresenter";
    private static final String WAKE_LOCK_TAG = "AcDisplay launcher.";

    /**
     * Requests to lock screen from AcDisplay activity.
     *
     * @return true if locked, false otherwise
     */
    @Deprecated
    public boolean stop(Context context) {
        //noinspection SimplifiableIfStatement
        if (mActivity != null
                && PowerUtils.isScreenOn(context)) {
            return mActivity.lock();
        }
        return false;
    }

    public void start(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (PowerUtils.isScreenOn(pm) || ts.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
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

        Config config = Config.getInstance();

        kill();
        context.startActivity(new Intent(Intent.ACTION_MAIN, null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_FROM_BACKGROUND)
                .putExtra(KeyguardActivity.EXTRA_TURN_SCREEN_ON, true)
                .putExtra(KeyguardActivity.EXTRA_FINISH_ON_SCREEN_OFF, !config.isKeyguardEnabled())
                .setClass(context, AcDisplayActivity.class));

        Log.i(TAG, "Launching AcDisplay activity.");
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

    private static Presenter sPresenter;

    private ArrayList<OnActiveDisplayStateChangedListener> mListeners;
    private AcDisplayActivity mActivity;

    public static synchronized Presenter getInstance() {
        if (sPresenter == null) {
            sPresenter = new Presenter();
        }
        return sPresenter;
    }

    private Presenter() {
        mListeners = new ArrayList<>(4);
    }

    public void registerListener(OnActiveDisplayStateChangedListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(OnActiveDisplayStateChangedListener listener) {
        mListeners.remove(listener);
    }

    public void attachActivity(AcDisplayActivity activity) {
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

    public AcDisplayActivity getActivity() {
        return mActivity;
    }

    public void launchAcDisplay(Context context) {
        if (mActivity != null) {
        }

        context.startActivity(new Intent(Intent.ACTION_MAIN, null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_FROM_BACKGROUND)
                .putExtra(KeyguardActivity.EXTRA_TURN_SCREEN_ON, true)
                .putExtra(KeyguardActivity.EXTRA_FINISH_ON_SCREEN_OFF, true)
                .setClass(context, AcDisplayActivity.class));
    }

}
