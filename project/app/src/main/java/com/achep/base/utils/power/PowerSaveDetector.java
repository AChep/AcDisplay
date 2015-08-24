package com.achep.base.utils.power;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.achep.acdisplay.Atomic;
import com.achep.base.Device;
import com.achep.base.interfaces.IPowerSave;
import com.achep.base.interfaces.ISubscriptable;

import java.util.ArrayList;

import static com.achep.base.Build.DEBUG_POWER_SAVING;

/**
 * @author Artem Chepurnoy
 */
public abstract class PowerSaveDetector implements
        ISubscriptable<PowerSaveDetector.OnPowerSaveChanged>,
        IPowerSave {

    private static boolean sPowerSaveMode;

    @NonNull
    public static PowerSaveDetector newInstance(@NonNull Context context) {
        return Device.hasLollipopApi()
                ? new PowerSaveLollipop(context)
                : new PowerSaveCompat(context);
    }

    /**
     * Returns {@code true} if the device is currently in power save mode.
     * When in this mode, applications should reduce their functionality
     * in order to conserve battery as much as possible.
     *
     * @return {@code true} if the device is currently in power save mode, {@code false} otherwise.
     */
    public static boolean isPowerSaving() {
        return sPowerSaveMode;
    }

    /**
     * Inverse function to {@link #isPowerSaveMode()}
     */
    /*
     * I hate using `if (!...)` construction so this
     * method was created
     */
    public static boolean isNotPowerSaving() {
        return !isPowerSaving();
    }

    protected final ArrayList<OnPowerSaveChanged> mListeners;
    protected final Context mContext;
    protected boolean mPowerSaveMode;

    /**
     * @author Artem Chepurnoy
     */
    public interface OnPowerSaveChanged {

        void onPowerSaveChanged(boolean powerSaving);
    }

    private PowerSaveDetector(@NonNull Context context) {
        mListeners = new ArrayList<>();
        mContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerListener(@NonNull OnPowerSaveChanged listener) {
        mListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterListener(@NonNull OnPowerSaveChanged listener) {
        mListeners.remove(listener);
    }

    public abstract void start();

    public abstract void stop();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPowerSaveMode() {
        return mPowerSaveMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotPowerSaveMode() {
        return !isNotPowerSaving();
    }

    protected void setPowerSaveMode(boolean psm) {
        if (DEBUG_POWER_SAVING) psm = true;
        if (mPowerSaveMode == psm) return;
        sPowerSaveMode = mPowerSaveMode = psm;
        notifyListeners();
    }

    private void notifyListeners() {
        for (OnPowerSaveChanged listener : mListeners) {
            listener.onPowerSaveChanged(mPowerSaveMode);
        }
    }

    /**
     * @author Artem Chepurnoy
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class PowerSaveLollipop extends PowerSaveDetector {

        private final PowerManager mPowerManager;
        private final BroadcastReceiver mReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        switch (intent.getAction()) {
                            case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED:
                                setPowerSaveMode(mPowerManager.isPowerSaveMode());
                                break;
                        }
                    }
                };

        @NonNull
        private final Atomic mAtomic = new Atomic(new Atomic.Callback() {
            @Override
            public void onStart(Object... objects) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
                mContext.registerReceiver(mReceiver, intentFilter);
                setPowerSaveMode(mPowerManager.isPowerSaveMode());
            }

            @Override
            public void onStop(Object... objects) {
                mContext.unregisterReceiver(mReceiver);
            }
        });

        public PowerSaveLollipop(@NonNull Context context) {
            super(context);
            mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        }

        @Override
        public void start() {
            mAtomic.start();
        }

        @Override
        public void stop() {
            mAtomic.stop();
        }
    }

    /**
     * @author Artem Chepurnoy
     */
    private static class PowerSaveCompat extends PowerSaveDetector {

        public PowerSaveCompat(@NonNull Context context) {
            super(context);
        }

        @Override
        public void start() { /* empty */ }

        @Override
        public void stop() { /* empty */ }

    }

}