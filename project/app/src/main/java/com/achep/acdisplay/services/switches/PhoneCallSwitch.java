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
package com.achep.acdisplay.services.switches;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.achep.acdisplay.services.Switch;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * Prevents {@link com.achep.acdisplay.services.SwitchService} from working
 * while an phone app is calling.
 *
 * @author Artem Chepurnoy
 */
public final class PhoneCallSwitch extends Switch {

    private static final String TAG = "PhoneCallSwitch";

    @NonNull
    private final CallMonitor mCallMonitor;

    public PhoneCallSwitch(@NonNull Context context, @NonNull Callback callback) {
        super(context, callback);
        mCallMonitor = new CallMonitor(context, new CallMonitor.OnCallStateChangedListener() {
            @Override
            public void onCallStateChanged(int state) {
                if (isActive()) {
                    requestActive();
                } else requestInactive();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        mCallMonitor.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        mCallMonitor.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return !mCallMonitor.isCalling();
    }

    /**
     * @return whether if the phone is calling now
     */
    public boolean isCalling() {
        return mCallMonitor.isCalling();
    }

    /**
     * @author Artem Chepurnoy
     */
    private static class CallMonitor {

        private boolean mStarted;

        /**
         * @author Artem Chepurnoy
         */
        public interface OnCallStateChangedListener {

            /**
             * Callback invoked when device call state changes.
             */
            void onCallStateChanged(int state);

        }

        @NonNull
        private final Context mContext;
        @NonNull
        private final Handler mHandler;
        @NonNull
        private final OnCallStateChangedListener mCallback;

        private Listener mListener;
        private TelephonyManager mTelephonyManager;

        public CallMonitor(
                @NonNull Context context,
                @NonNull OnCallStateChangedListener listener) {
            mContext = context;
            mCallback = listener;
            mHandler = new Handler();
            mListener = new Listener(this);
        }

        public void start() {
            mStarted = true;
            mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        public void stop() {
            mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
            mTelephonyManager = null;
            mStarted = false;
        }

        /**
         * @return the current call state.
         * @see #isCalling()
         */
        public int getCallState() {
            return mListener.getCallState();
        }

        /**
         * @return whether if the phone is calling now
         * @see #getCallState()
         */
        public boolean isCalling() {
            final int state = getCallState();
            return state == TelephonyManager.CALL_STATE_RINGING
                    || state == TelephonyManager.CALL_STATE_OFFHOOK;
        }

        private void notifyCallStateChanged(final int state) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mStarted) mCallback.onCallStateChanged(state);
                }
            });
        }

        /**
         * @author Artem Chepurnoy
         */
        private static class Listener extends PhoneStateListener {

            @NonNull
            private final Reference<CallMonitor> mCallHandlerRef;

            /**
             * The current call state.
             */
            private int mState;

            public Listener(@NonNull CallMonitor callStateListener) {
                mCallHandlerRef = new WeakReference<>(callStateListener);
                mState = TelephonyManager.CALL_STATE_IDLE;
            }

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                if (mState == state) return;
                mState = state;
                // Notify the call handler
                CallMonitor callStateListener = mCallHandlerRef.get();
                if (callStateListener != null) callStateListener.notifyCallStateChanged(state);
            }

            /**
             * @return the current call state.
             */
            public int getCallState() {
                return mState;
            }

        }

    }

}
