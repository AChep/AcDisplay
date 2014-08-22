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

package android.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.KeyEvent;

public class RemoteController {

    public RemoteController(Context context, OnClientUpdateListener l) {
        throw new RuntimeException("Shit happened!");
    }

    public boolean setArtworkConfiguration(int width, int height) {
        return false;
    }

    public boolean sendMediaKeyEvent(KeyEvent keyEvent) { return true; }

    public class MetadataEditor {

        public synchronized String getString(int key, String defaultValue) {
            return null;
        }

        public synchronized long getLong(int key, long defaultValue) {
            return 0;
        }

        public synchronized Bitmap getBitmap(int key, Bitmap defaultValue) {
            return null;
        }

    }

    public interface OnClientUpdateListener {

        public void onClientChange(boolean clearing);

        public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor);

        public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed);

        public void onClientPlaybackStateUpdate(int state);

        public void onClientTransportControlUpdate(int transportControlFlags);

    }
}