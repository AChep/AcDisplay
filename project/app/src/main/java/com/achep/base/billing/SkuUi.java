package com.achep.base.billing;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.solovyev.android.checkout.Sku;

/**
 * @author Artem Chepurnoy
 */
public abstract class SkuUi {

    private static final long MICRO = 1_000_000; // defines how much 'micro' is

    @NonNull
    public final Sku sku;

    @Nullable
    private String mDescription;
    private final boolean mIsPurchased;

    public SkuUi(@NonNull Sku sku, boolean isPurchased) {
        this.sku = sku;
        this.mIsPurchased = isPurchased;
    }

    @NonNull
    protected abstract String onCreateDescription(@NonNull Sku sku);

    /**
     * @return the price of the sku in {@link #getPriceCurrency() currency}.
     * @see #getPriceCurrency()
     * @see #getDescription()
     */
    @NonNull
    public String getPriceAmount() {
        long amountMicro = sku.detailedPrice.amount;
        if (amountMicro % MICRO == 0) {
            // Format it 'as int' number to
            // get rid of unused comma.
            long amount = amountMicro / MICRO;
            return String.valueOf(amount);
        }

        double amount = (double) amountMicro / MICRO;
        return String.valueOf(amount);
    }

    /**
     * @return the currency of the price.
     * @see #getPriceAmount()
     */
    @NonNull
    public String getPriceCurrency() {
        return sku.detailedPrice.currency;
    }

    /**
     * The thing that you may buy for that money.
     *
     * @see #getPriceAmount()
     */
    @NonNull
    public String getDescription() {
        if (mDescription == null) {
            mDescription = onCreateDescription(sku);
        }
        return mDescription;
    }

    /**
     * @return {@code true} if the sku is purchased,
     * {@code false} otherwise.
     */
    public boolean isPurchased() {
        return mIsPurchased;
    }

}