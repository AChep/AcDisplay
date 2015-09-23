/*
 * Copyright (C) 2015 AChep@xda <artemchep@gmail.com>
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
package com.achep.base.permissions;

import android.content.Context;
import android.support.annotation.NonNull;

import com.achep.base.interfaces.IOnLowMemory;
import com.achep.base.interfaces.IPermission;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.ArrayList;

/**
 * A permission that consists of {@link Permission permissions}.
 *
 * @author Artem Chepurnoy
 */
public class PermissionGroup implements IOnLowMemory, IPermission {

    @NonNull
    public final Permission[] permissions;

    public PermissionGroup(@NonNull Permission[] permissions) {
        this.permissions = permissions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(37, 49)
                .append(permissions)
                .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof PermissionGroup)) return false;

        PermissionGroup pg = (PermissionGroup) o;
        return new EqualsBuilder()
                .append(permissions, pg.permissions)
                .isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGranted() {
        for (IPermission permission : permissions)
            if (!permission.isGranted()) return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(@NonNull Context context) {
        for (IPermission permission : permissions)
            if (!permission.exists(context)) return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLowMemory() {
        for (IPermission permission : permissions) {
            permission.onLowMemory();
        }
    }

    /**
     * @author Artem Chepurnoy
     */
    public static class Builder {

        @NonNull
        private final Context mContext;
        @NonNull
        private final ArrayList<String> mList;

        public Builder(@NonNull Context context) {
            mContext = context;
            mList = new ArrayList<>();
        }

        /**
         * Adds new permission to the group.
         *
         * @see com.achep.base.permissions.Permission#PERMISSION_ACCESSIBILITY
         * @see com.achep.base.permissions.Permission#PERMISSION_DEVICE_ADMIN
         * @see com.achep.base.permissions.Permission#PERMISSION_NOTIFICATION_LISTENER
         * @see com.achep.base.permissions.Permission#PERMISSION_USAGE_STATS
         */
        @NonNull
        public Builder add(@NonNull String permission) {
            mList.add(permission);
            return this;
        }

        @NonNull
        public PermissionGroup build() {
            final Permission[] p = new Permission[mList.size()];
            for (int i = 0; i < p.length; i++)
                p[i] = Permission.newInstance(mContext, mList.get(i));
            return new PermissionGroup(p);
        }

    }

}
