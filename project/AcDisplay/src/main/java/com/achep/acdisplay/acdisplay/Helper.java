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

package com.achep.acdisplay.acdisplay;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenStatusBarNotification;
import com.achep.acdisplay.widgets.NotificationView;

import java.util.ArrayList;

/**
 * Created by achep on 14.05.14 for AcDisplay.
 *
 * @author Artem Chepurnoy
 */
public class Helper {

    private void updateNotificationList(Context context, LinearLayout container, int itemLayoutRes) {
        final ArrayList<OpenStatusBarNotification> list = null;//mPresenter.getList();
        final int notifyCount = list.size();

        final int childCount = container.getChildCount();
        final boolean[] notifyUsed = new boolean[notifyCount];
        final boolean[] childUsed = new boolean[childCount];

        // ///////////////////
        // ~~ NOTIFICATIONS ~~
        // ///////////////////

        // Does not need an update
        for (int i = 0; i < childCount; i++) {
            NotificationView item = (NotificationView) container.getChildAt(i);
            assert item != null;
            OpenStatusBarNotification target = item.getNotification();

            // Try to find the notification with the same
            // id, tag and package name as in present.
            for (int j = 0; j < notifyCount; j++) {
                OpenStatusBarNotification n = list.get(j);
                if (NotificationUtils.equals(target, n)) {

                    notifyUsed[j] = true;
                    childUsed[i] = true;

                    if (target != n) {
                        item.setNotification(n);
                    }
                    break;
                }
            }
        }

        // Re-use free views and remove redundant views.
        boolean removeAllAfter = false;
        for (int a = 0, j = 0, offset = 0; a < childCount; a++) {
            if (childUsed[a]) continue;
            final int i = a + offset;

            removing_all_next_views:
            {
                if (!removeAllAfter) {
                    for (; j < notifyCount; j++) {
                        if (notifyUsed[j]) continue;

                        notifyUsed[j] = true;

                        NotificationView item = (NotificationView) container.getChildAt(i);
                        assert item != null;
                        item.setNotification(list.get(j));

                        break removing_all_next_views;
                    }
                }
                removeAllAfter = true;
                container.removeViewAt(i);
                offset--;
            }
        }

        LayoutInflater inflater = null;//context.getLayoutInflater();

        for (int i = 0; i < notifyCount; i++) {
            if (notifyUsed[i]) continue;

            View view = inflater.inflate(itemLayoutRes, container, false);
            assert view != null;
            container.addView(view);

            NotificationView item = (NotificationView) view;
            item.setNotification(list.get(i));
        }
    }
}
