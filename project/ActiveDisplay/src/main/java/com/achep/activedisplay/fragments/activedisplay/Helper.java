/*
 * Copyright (C) 2013-2014 AChep@xda <artemchep@gmail.com>
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
package com.achep.activedisplay.fragments.activedisplay;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.achep.activedisplay.notifications.NotificationPresenter;
import com.achep.activedisplay.notifications.NotificationUtils;
import com.achep.activedisplay.notifications.OpenStatusBarNotification;
import com.achep.activedisplay.utils.ViewUtils;
import com.achep.activedisplay.widgets.NotificationView;

import java.util.ArrayList;

/**
 * Created by Artem on 02.02.14.
 */
class Helper {

    @SuppressWarnings("ConstantConditions")
    public static boolean updateNotificationList(NotificationPresenter presenter,
                                                 ViewGroup container, int layoutRes,
                                                 LayoutInflater inflater) {
        final ArrayList<OpenStatusBarNotification> list = presenter.getList();
        final int notifyCount = list.size();

        boolean visible = notifyCount > 1;
        ViewUtils.setVisible(container, visible);
        if (!visible) return false;

        final int childCount = container.getChildCount();
        final boolean[] notifyUsed = new boolean[notifyCount];
        final boolean[] childUsed = new boolean[childCount];

        // Does not need an update
        for (int i = 0; i < childCount; i++) {
            NotificationView child = (NotificationView) container.getChildAt(i);
            assert child != null;
            OpenStatusBarNotification target = child.getNotification();

            for (int j = 0; j < notifyCount; j++) {
                OpenStatusBarNotification n = list.get(j);
                if (NotificationUtils.equals(target, n)) {

                    notifyUsed[j] = true;
                    childUsed[i] = true;

                    if (target != n) {
                        child.setNotification(n);
                    }
                    break;
                }
            }
        }

        // Re-use free views and remove redundant views.
        boolean removeAllAfter = false;
        for (int source = 0, j = 0, offset = 0; source < childCount; source++) {
            if (childUsed[source]) continue;
            final int i = source + offset;

            NotificationView child = (NotificationView) container.getChildAt(i);
            super_continue:
            {
                if (!removeAllAfter)
                    for (; j < notifyCount; j++) {
                        if (notifyUsed[j]) continue;

                        assert child != null;
                        notifyUsed[j] = true;
                        child.setNotification(list.get(j));
                        break super_continue;
                    }
                removeAllAfter = true;
                container.removeViewAt(i);
                offset--;
            }
        }

        // Create new views
        for (int i = 0; i < notifyCount; i++) {
            if (notifyUsed[i])
                continue;

            View view = inflater.inflate(layoutRes, container, false);
            ((NotificationView) view).setNotification(list.get(i));

            container.addView(view, i);
        }

        return true;
    }

}
