package com.achep.base.interfaces;

/**
 * @author Artem Chepurnoy
 */
public interface IPowerSave {

    /**
     * Returns {@code true} if the device is currently in power save mode.
     * When in this mode, applications should reduce their functionality
     * in order to conserve battery as much as possible.
     *
     * @return {@code true} if the device is currently in power save mode, {@code false} otherwise.
     */
    boolean isPowerSaveMode();

    /**
     * Inverse function to {@link #isPowerSaveMode()}
     */
    /*
     * I hate using `if (!...)` construction so this
     * method was created
     */
    boolean isNotPowerSaveMode();

}
