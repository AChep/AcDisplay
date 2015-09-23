package com.achep.base.services;

import android.app.Service;
import android.content.Intent;

import com.achep.base.interfaces.IPowerSave;
import com.achep.base.utils.power.PowerSaveDetector;

/**
 * @author Artem Chepurnoy
 * @since 4.0.0
 */
public abstract class BaseService extends Service implements IPowerSave {

    private PowerSaveDetector mPsd;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mPsd = PowerSaveDetector.newInstance(this);
        mPsd.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        mPsd.stop();
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        mPsd.stop();
        super.onTaskRemoved(rootIntent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPowerSaveMode() {
        return mPsd.isPowerSaveMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNotPowerSaveMode() {
        return mPsd.isNotPowerSaveMode();
    }
}
