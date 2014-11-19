package com.achep.acdisplay.acdisplay;

import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Log;

/**
 * Thread used to turn the brightness up and down,
 * creating a pusling notification screen
 * <p/>
 * Battery impact not known yet!
 *
 * @author Nils Strelow
 *         Created by Nils Strelow on 08.09.2014.
 */
public class PulsingThread extends Thread {

    // TODO: PRIORITY: Go back to initial brightness
    // TODO: Start from initial brightness
    // Could TODO: make it smoother, cooler

    int brightness;
    int initialBrightness = 125;
    ContentResolver mContentResolver;
    private int BRIGHTNESS_MULTIPLIER = 8;
    private int MAX_BRIGHTNESS = 255;
    private boolean run = true;

    public PulsingThread(ContentResolver contentResolver) {
        mContentResolver = contentResolver;
    }

    @Override
    public void run() {
        try {
            initialBrightness = Settings.System.getInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            Log.e("Setting not found", e.toString());
        }

        int i = initialBrightness;

        int k = 0;

        while (run) {
            k++;
            // Start from initialBrightness
            for (i = i; i <= MAX_BRIGHTNESS / BRIGHTNESS_MULTIPLIER; i++) {
                brightness = i * BRIGHTNESS_MULTIPLIER;
                Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
                if (!run) {
                    break;
                }
            }
            for (i = i; i >= 1; i--) {
                brightness = i * BRIGHTNESS_MULTIPLIER;
                Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
                if (!run) {
                    break;
                }
            }
            if (k >= 10) {
                run = false;
            }
        }

        // smoothly go back to initialBrightness
        for (i = i; i <= initialBrightness / BRIGHTNESS_MULTIPLIER; i++) {
            brightness = i * BRIGHTNESS_MULTIPLIER;
            Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
        }
        // Set back to initialBrightness
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, initialBrightness);
    }

    public void requestStop() {
        run = false;
    }
}
