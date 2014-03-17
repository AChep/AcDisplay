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
package com.achep.activedisplay.receiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.achep.activedisplay.Project;
import com.achep.activedisplay.utils.IntentUtils;

/**
 * Created by Artem on 12.03.14.
 */
public class ReceiverActivity extends Activity {

    private static final String TAG = "ReceiverActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {

            if (Project.DEBUG) Log.d(TAG, "" + intent.getPackage());
            if (true) {
                handleLocalIntent(intent);
            } else {
                handleIntent(intent);
            }
        } else {
            Log.wtf(TAG, "Got an empty launch intent.");
        }

        finish();
    }

    private void handleLocalIntent(Intent intent) {
        Uri data = intent.getData();

        if (data == null) {
            return;
        }

        String host = data.getHost();

        if (host == null) {
            return;
        }

        switch (host) {
            case "launch_device_admins_activity":
                Intent launchIntent = new Intent().setComponent(new ComponentName(
                        "com.android.settings",
                        "com.android.settings.DeviceAdminSettings"));
                if (IntentUtils.hasActivityForThat(this, launchIntent)) {
                    startActivity(launchIntent);
                } else {
                    // TODO: Show toast message
                }
                break;
            case "launch_uninstall_screen":
                Uri packageUri = Uri.parse("package:" + Project.getPackageName(this));
                launchIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                if (IntentUtils.hasActivityForThat(this, launchIntent)) {
                    startActivity(launchIntent);
                } else {
                    // TODO: Show toast message
                }
                break;
            default:
                Log.wtf(TAG, "Got an unknown intent: " + host);
                break;
        }
    }

    private void handleIntent(Intent intent) {

    }

}
