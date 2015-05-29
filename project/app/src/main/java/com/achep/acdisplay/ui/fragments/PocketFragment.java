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
package com.achep.acdisplay.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;

import com.achep.base.ui.fragments.leakcanary.LeakWatchFragment;

/**
 * Non-UI fragment that listens to the proximity sensor to let us know
 * when to turn screen off.
 * <p>
 * <b>Please note, that you must
 * {@link #setListener(PocketFragment.OnSleepRequestListener) initialize listener}
 * before attaching fragment!</b>
 * </p>
 *
 * @author Artem Chepurnoy
 * @see #setListener(PocketFragment.OnSleepRequestListener)
 */
public class PocketFragment extends LeakWatchFragment implements SensorEventListener {

    public static final String TAG = "PocketFragment";

    private static final int SLEEP_DELAY = 1500; // 1.5 sec.
    private static final int MSG_SLEEP = 0;

    private SensorManager mSensorManager;
    private Sensor mProximitySensor;

    private boolean mProximityAvailable;

    private boolean mNear;
    private boolean mFirstChange;
    private float mMaximumRange;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SLEEP:
                    if (mListener != null) {
                        mListener.onSleepRequest();
                    }
                    break;
            }
        }
    };

    private OnSleepRequestListener mListener;

    /**
     * Interface definition for a callback to be invoked
     * when device has been put to the pocket.
     */
    public interface OnSleepRequestListener {

        /**
         * Called when parent activity may go to sleep because we're
         * in pocket at this moment.
         */
        boolean onSleepRequest();

    }

    public static PocketFragment newInstance() {
        return new PocketFragment();
    }

    public PocketFragment setListener(OnSleepRequestListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mProximityAvailable = mProximitySensor != null;

        if (mProximityAvailable) {
            mMaximumRange = mProximitySensor.getMaximumRange();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProximityAvailable) {
            mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            mFirstChange = true;
            mNear = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSensorManager = null;
        mProximitySensor = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float distance = event.values[0];
        final boolean isNear = distance < mMaximumRange || distance < 1.0f;
        final boolean changed = mNear != (mNear = isNear) || mFirstChange;

        if (!changed) {
            // Well just in cause if proximity sensor is NOT always eventual.
            // This should not happen, but who knows... I found maximum
            // range buggy enough.
            return;
        }

        mHandler.removeCallbacksAndMessages(null);

        if (!isNear) {
            return;
        }

        mHandler.sendEmptyMessageDelayed(MSG_SLEEP, SLEEP_DELAY);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { /* unused */ }
}
