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

package com.achep.acdisplay.iab;

import android.content.res.Resources;

/**
 * Created by achep on 07.05.14 for AcDisplay.
 *
 * @author Artem Chepurnoy
 */
public class DonationItems {

    public static Donation[] get(Resources res) {
        int[] data = new int[] {
                1, R.string.donation_1,
                4, R.string.donation_4,
                10, R.string.donation_10,
                20, R.string.donation_20,
                50, R.string.donation_50,
                99, R.string.donation_99,
        };

        Donation[] donation = new Donation[data.length / 2];

        int length = donation.length;
        for (int i = 0; i < length; i++) {
            donation[i] = new Donation(data[i * 2],
                    res.getString(data[i * 2 + 1]));
        }
        return donation;
    }

}
