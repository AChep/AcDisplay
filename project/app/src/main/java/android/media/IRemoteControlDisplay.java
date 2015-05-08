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

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.RemoteException;

/*
    This is needed to be able to get to those
    damn internal APIs.
 */
public interface IRemoteControlDisplay {

    abstract class Stub implements IRemoteControlDisplay {

        public abstract void setAllMetadata(int generationId, Bundle metadata, Bitmap bitmap);

        public abstract void setArtwork(int generationId, Bitmap bitmap);

        public abstract void setCurrentClientId(int clientGeneration, PendingIntent mediaIntent,
                                                boolean clearing) throws RemoteException;

        public abstract void setMetadata(int generationId, Bundle metadata);

        public abstract void setPlaybackState(int generationId, int state, long stateChangeTimeMs);

        public abstract void setTransportControlFlags(int generationId, int flags);

    }
}