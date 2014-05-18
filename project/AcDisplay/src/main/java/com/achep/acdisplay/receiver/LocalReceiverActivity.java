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
package com.achep.acdisplay.receiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.achep.acdisplay.utils.IntentUtils;
import com.achep.acdisplay.utils.PackageUtils;
import com.achep.acdisplay.utils.ToastUtils;

/**
 * Created by Artem on 12.03.14.
 */
public class LocalReceiverActivity extends Activity {

    private static final String TAG = "LocalReceiverActivity";

    private static final String HOST_UNINSTALL = "uninstall";
    private static final String HOST_LAUNCH_DEVICE_ADMINS = "launch_device_admins_activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        } else {
            Log.wtf(TAG, "Got an empty launch intent.");
        }

        finish();
    }

    static String extractHost(Intent intent) {
        Uri data = intent.getData();
        return data != null ? data.getHost() : null;
    }

    private void handleIntent(Intent intent) {
        String host = extractHost(intent);
        if (host == null) {
            Log.wtf(TAG, "Got an empty launch intent.");
            return;
        }

        switch (host) {
            case HOST_LAUNCH_DEVICE_ADMINS:
                Intent launchIntent = new Intent().setComponent(new ComponentName(
                        "com.android.settings",
                        "com.android.settings.DeviceAdminSettings"));
                if (IntentUtils.hasActivityForThat(this, launchIntent)) {
                    startActivity(launchIntent);
                } else {
                    ToastUtils.showShort(this,
                            getString(R.string.device_admin_could_not_be_started));
                }
                break;
            case HOST_UNINSTALL:
                Uri packageUri = Uri.parse("package:" + PackageUtils.getName(this));
                launchIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                if (IntentUtils.hasActivityForThat(this, launchIntent)) {
                    startActivity(launchIntent);
                } else {
                    ToastUtils.showShort(this,
                            getString(R.string.package_could_not_be_uninstalled));
                }
                break;
            default:
                Log.wtf(TAG, "Got an unknown intent: " + host);
                return;
        }

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(host);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

}
