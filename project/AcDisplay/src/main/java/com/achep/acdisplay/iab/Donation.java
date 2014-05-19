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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * The helper class of donation item.
 *
 * @author Artem Chepurnoy
 */
public class Donation {

    public int amount;
    public String sku;
    public String text;

    public Donation(int amount, String text) {
        this.amount = amount;
        this.text = text;

        // Notice that all of them are defined in
        // my Play Store's account!
        this.sku = "donation_" + amount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(2, 51)
                .append(amount)
                .append(text)
                .append(sku)
                .toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (!(o instanceof Donation))
            return false;

        Donation donation = (Donation) o;
        return new EqualsBuilder()
                .append(amount, donation.amount)
                .append(text, donation.text)
                .append(sku, donation.sku)
                .isEquals();
    }

}
