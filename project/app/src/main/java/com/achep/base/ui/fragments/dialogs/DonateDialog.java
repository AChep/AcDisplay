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
package com.achep.base.ui.fragments.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.achep.acdisplay.R;
import com.achep.base.AppHeap;
import com.achep.base.billing.Bitcoin;
import com.achep.base.billing.PayPal;
import com.achep.base.interfaces.IActivityBase;
import com.achep.base.ui.adapters.BetterArrayAdapter;
import com.achep.base.ui.widgets.HeaderGridView;
import com.achep.base.ui.widgets.TextView;
import com.achep.base.utils.CoinUtils;
import com.achep.base.utils.RippleUtils;
import com.achep.base.utils.ToastUtils;
import com.achep.base.utils.ViewUtils;
import com.afollestad.materialdialogs.MaterialDialog;

import org.solovyev.android.checkout.ActivityCheckout;
import org.solovyev.android.checkout.BillingRequests;
import org.solovyev.android.checkout.Checkout;
import org.solovyev.android.checkout.Inventory;
import org.solovyev.android.checkout.ProductTypes;
import org.solovyev.android.checkout.Purchase;
import org.solovyev.android.checkout.RequestListener;
import org.solovyev.android.checkout.ResponseCodes;
import org.solovyev.android.checkout.Sku;

import java.util.Comparator;

import static com.achep.base.Build.DEBUG;

/**
 * Created by Artem Chepurnoy on 13.12.2014.
 */
public class DonateDialog extends DialogFragment {

    private static final String TAG = "DonateFragment";

    private static final int SCREEN_LOADING = 1;
    private static final int SCREEN_INVENTORY = 2;
    private static final int SCREEN_EMPTY_VIEW = 4;

    private TextView mEmptyView;
    private ProgressBar mProgressBar;

    private Inventory mInventory;
    private ActivityCheckout mCheckout;
    private final PurchaseListener mPurchaseListener = new PurchaseListener();
    private final InventoryLoadedListener mInventoryLoadedListener = new InventoryLoadedListener();

    private SkusAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof IActivityBase) {
            IActivityBase ma = (IActivityBase) activity;
            mCheckout = ma.getCheckout();

            if (mCheckout == null) {
                String message = "You must call #requestCheckout() on the activity before!";
                throw new RuntimeException(message);
            }
        } else throw new RuntimeException("Host activity must be an " +
                "instance of IActivityBase.class!");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInventory = mCheckout.loadInventory();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog md = initDialog();
        LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        FrameLayout frameLayout = new FrameLayout(getActivity()); //

        // Init description message.
        TextView textView = (TextView) inflater.inflate(R.layout.dialog_message, frameLayout, false);
        textView.setText(R.string.donate_dialog_message);
        textView.setPadding(0, textView.getPaddingTop(), 0, textView.getPaddingBottom() / 2);

        // Init view with error view and progressbar-s.
        View phView = inflater.inflate(R.layout.dialog_donate_placeholder, frameLayout, false);
        mProgressBar = (ProgressBar) phView.findViewById(R.id.progress);
        mEmptyView = (TextView) phView.findViewById(R.id.empty);

        HeaderGridView gv = (HeaderGridView) md.getCustomView().findViewById(R.id.grid);
        gv.addHeaderView(textView, null, false);
        gv.addHeaderView(phView, null, false);
        gv.setAdapter(mAdapter = new SkusAdapter(getActivity(), R.layout.sku));
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SkuUi skuUi = (SkuUi) parent.getAdapter().getItem(position);
                purchase(skuUi.sku);
            }
        });

        return md;
    }

    @NonNull
    private MaterialDialog initDialog() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                .iconRes(R.drawable.ic_action_donate_white)
                .title(R.string.donate_dialog_title)
                .customView(R.layout.dialog_donate, false)
                .neutralText(R.string.close);

        if (!getResources().getBoolean(R.bool.config_alternative_payments)) {
            return builder.build();
        }

        final Bitcoin btc = new Bitcoin();
        final PayPal pp = new PayPal();
        return builder
                .positiveText(btc.getNameResource())
                .negativeText(pp.getNameResource())
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        startPaymentIntentWithWarningAlertDialog(CoinUtils.getPaymentIntent(btc));
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        startPaymentIntentWithWarningAlertDialog(CoinUtils.getPaymentIntent(pp));
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        dismiss();
                    }
                })
                .autoDismiss(false)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mCheckout.createPurchaseFlow(mPurchaseListener);
        reloadInventory();
    }

    @Override
    public void onStop() {
        mCheckout.destroyPurchaseFlow();
        super.onStop();
    }

    /**
     * Shows a warning alert dialog to note, that those methods
     * may suck hard and nobody will care about it.<br/>
     * Starts an intent if user is agree with it.
     */
    private void startPaymentIntentWithWarningAlertDialog(final Intent intent) {
        new MaterialDialog.Builder(getActivity())
                .content(R.string.donate_alert_no_responsibility)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        try {
                            startActivity(intent);
                            dismiss(); // Dismiss main fragment
                        } catch (ActivityNotFoundException e) { /* hell no */ }
                    }
                })
                .build()
                .show();
    }

    private void showScene(int visibility) {
        ViewUtils.setVisible(mProgressBar, visibility == SCREEN_LOADING);
        ViewUtils.setVisible(mEmptyView, visibility == SCREEN_EMPTY_VIEW);
    }

    private void reloadInventory() {
        showScene(SCREEN_LOADING);
        mInventory.load().whenLoaded(mInventoryLoadedListener);
    }

    private void purchase(@NonNull final Sku sku) {
        if (DEBUG) Log.d(TAG, "Purchasing " + sku.toString() + "...");
        mCheckout.whenReady(new Checkout.ListenerAdapter() {
            @Override
            public void onReady(@NonNull BillingRequests requests) {
                requests.purchase(sku, null, mCheckout.getPurchaseFlow());
            }
        });
    }

    private class InventoryLoadedListener implements Inventory.Listener {

        @Override
        public void onLoaded(@NonNull Inventory.Products products) {
            final Inventory.Product product = products.get(ProductTypes.IN_APP);
            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();

            if (product.supported) {
                for (Sku sku : product.getSkus()) {
                    final Purchase purchase = product.getPurchaseInState(sku, Purchase.State.PURCHASED);
                    final SkuUi skuUi = new SkuUi(sku, purchase != null);
                    mAdapter.add(skuUi);
                }

                // Sort items by prices.
                mAdapter.sort(new Comparator<SkuUi>() {
                    @Override
                    public int compare(@NonNull SkuUi l, @NonNull SkuUi r) {
                        return (int) (l.sku.detailedPrice.amount - r.sku.detailedPrice.amount);
                    }
                });
                showScene(SCREEN_INVENTORY);
            } else {
                mEmptyView.setText(R.string.donate_billing_not_supported);
                showScene(SCREEN_EMPTY_VIEW);
            }

            mAdapter.notifyDataSetChanged();
        }

    }

    private abstract class BaseRequestListener<T> implements RequestListener<T> {

        @Override
        public void onError(int response, @NonNull Exception e) {
            ToastUtils.showShort(getActivity(), e.getLocalizedMessage());
        }

    }

    private class PurchaseListener extends BaseRequestListener<Purchase> {

        @Override
        public void onSuccess(@NonNull Purchase purchase) {
            purchased();
        }

        @Override
        public void onError(int response, @NonNull Exception e) {
            switch (response) {
                case ResponseCodes.ITEM_ALREADY_OWNED:
                    purchased();
                    break;
                default:
                    super.onError(response, e);
            }
        }

        private void purchased() {
            reloadInventory();
            ToastUtils.showLong(getActivity(), R.string.donate_thanks);
        }

    }

    /**
     * Created by Artem Chepurnoy on 23.12.2014.
     */
    private static class SkusAdapter extends BetterArrayAdapter<SkuUi> {

        private static final class ViewHolder extends BetterArrayAdapter.ViewHolder {

            @NonNull
            private final android.widget.TextView description;

            @NonNull
            private final android.widget.TextView price;

            @NonNull
            private final android.widget.TextView currency;

            @NonNull
            private final ImageView done;

            public ViewHolder(@NonNull View view) {
                super(view);
                description = (android.widget.TextView) view.findViewById(R.id.description);
                View layout = view.findViewById(R.id.cost);
                price = (android.widget.TextView) layout.findViewById(R.id.price);
                currency = (android.widget.TextView) layout.findViewById(R.id.currency);
                done = (ImageView) layout.findViewById(R.id.done);
            }

        }

        public SkusAdapter(@NonNull Context context, @LayoutRes int layoutRes) {
            super(context, layoutRes);
        }

        @NonNull
        @Override
        public BetterArrayAdapter.ViewHolder onCreateViewHolder(@NonNull View view) {
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BetterArrayAdapter.ViewHolder viewHolder, int i) {
            fill(mContext, (ViewHolder) viewHolder, getItem(i));
        }

        private static void fill(@NonNull Context context,
                                 @NonNull ViewHolder holder,
                                 @NonNull SkuUi skuUi) {
            RippleUtils.makeFor(holder.view, true);
            holder.description.setText(skuUi.getDescription());

            int visibility;
            if (skuUi.isPurchased()) {
                visibility = View.GONE;
                holder.done.setVisibility(View.VISIBLE);
            } else {
                visibility = View.VISIBLE;
                holder.price.setText(skuUi.getPriceAmount());
                holder.currency.setText(skuUi.getPriceCurrency());
                holder.done.setVisibility(View.GONE);
            }

            holder.price.setVisibility(visibility);
            holder.currency.setVisibility(visibility);
        }

    }

    /**
     * @author Artem Chepurnoy
     */
    private static class SkuUi {

        @NonNull
        private static final String TAG = "SkuUi";

        private static final long MICRO = 1_000_000; // defines how much 'micro' is

        @NonNull
        public final Sku sku;

        private final boolean isPurchased;

        @Nullable
        private String description;

        public SkuUi(@NonNull Sku sku, boolean isPurchased) {
            this.sku = sku;
            this.isPurchased = isPurchased;
        }

        @NonNull
        private static String createDescription(@NonNull Sku sku) {
            String prefix = "donation_";
            if (sku.id.startsWith(prefix)) {
                int[] data = new int[]{
                        1, R.string.donation_1,
                        4, R.string.donation_4,
                        10, R.string.donation_10,
                        20, R.string.donation_20,
                        50, R.string.donation_50,
                        99, R.string.donation_99,
                };

                int price = Integer.parseInt(sku.id.substring(prefix.length()));
                for (int i = 0; i < data.length; i += 2) {
                    if (price == data[i]) {
                        Context context = AppHeap.getContext();
                        return context.getString(data[i + 1]);
                    }
                }
            }

            Log.wtf(TAG, "Alien sku found!");
            return "Alien sku found!";
        }

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
            if (description == null)
                description = createDescription(sku);
            return description;
        }

        /**
         * @return {@code true} if the sku is purchased, {@code false} otherwise.
         */
        public boolean isPurchased() {
            return isPurchased;
        }

    }
}
