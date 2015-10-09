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

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
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
import com.achep.base.billing.SkuUi;
import com.achep.base.interfaces.IActivityBase;
import com.achep.base.interfaces.IConfiguration;
import com.achep.base.ui.adapters.BetterArrayAdapter;
import com.achep.base.ui.widgets.HeaderGridView;
import com.achep.base.ui.widgets.TextView;
import com.achep.base.utils.CoinUtils;
import com.achep.base.utils.RippleUtils;
import com.achep.base.utils.ToastUtils;
import com.achep.base.utils.ViewUtils;
import com.afollestad.materialdialogs.DialogAction;
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
 * @author Artem Chepurnoy
 */
public class DonateDialog extends DialogFragment {

    private static final String TAG = "DonateFragment";

    private static final int SCREEN_LOADING = 1;
    private static final int SCREEN_INVENTORY = 2;
    private static final int SCREEN_EMPTY_VIEW = 4;

    @NonNull
    private final InventoryLoadedListener mInventoryLoadedListener = new InventoryLoadedListener();
    @NonNull
    private final PurchaseListener mPurchaseListener = new PurchaseListener();
    private ActivityCheckout mCheckout;
    private SkusAdapter mAdapter;
    private Inventory mInventory;

    private ProgressBar mProgressBar;
    private TextView mEmptyView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IActivityBase) {
            IActivityBase ma = (IActivityBase) context;
            mCheckout = ma.getCheckout();

            if (mCheckout == null) {
                String message = "You must call #requestCheckout() on the activity before!";
                throw new RuntimeException(message);
            }

            return; // don't crash
        }

        throw new RuntimeException("Host activity must be an instance of IActivityBase.class!");
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
        mEmptyView.setText(R.string.donate_billing_not_supported);

        assert md.getCustomView() != null;
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
                .iconRes(R.drawable.ic_gift_white_24dp)
                .title(R.string.donate_dialog_title)
                .customView(R.layout.dialog_donate, false)
                .neutralText(R.string.close);

        final IConfiguration configuration = AppHeap.getInstance().getConfiguration();
        final boolean hasApl = configuration.getBilling().hasAlternativePaymentMethods();
        if (!hasApl) return builder.build();

        final Bitcoin btc = new Bitcoin();
        final PayPal pp = new PayPal();

        MaterialDialog.SingleButtonCallback callback = new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog materialDialog,
                                @NonNull DialogAction dialogAction) {
                if (dialogAction == DialogAction.POSITIVE) {
                    startPaymentIntentWithWarningAlertDialog(CoinUtils.getPaymentIntent(btc));
                } else if (dialogAction == DialogAction.NEGATIVE) {
                    startPaymentIntentWithWarningAlertDialog(CoinUtils.getPaymentIntent(pp));
                } else if (dialogAction == DialogAction.NEUTRAL) {
                    dismiss();
                }
            }
        };
        return builder
                .positiveText(btc.getNameResource())
                .negativeText(pp.getNameResource())
                .onPositive(callback)
                .onNegative(callback)
                .onNeutral(callback)
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
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog,
                                        @NonNull DialogAction dialogAction) {
                        try {
                            startActivity(intent);
                            dismiss(); // Dismiss main fragment
                        } catch (ActivityNotFoundException e) { /* hell no */ }
                    }
                })
                .build()
                .show();
    }

    private void refreshUi(int visibility) {
        ViewUtils.setVisible(mProgressBar, visibility == SCREEN_LOADING);
        ViewUtils.setVisible(mEmptyView, visibility == SCREEN_EMPTY_VIEW);
    }

    private void reloadInventory() {
        // Set `loading` state.
        refreshUi(SCREEN_LOADING);
        // Reload the inventory.
        mInventory
                .load()
                .whenLoaded(mInventoryLoadedListener);
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

    /**
     * @author Artem Chepurnoy
     */
    private class InventoryLoadedListener implements Inventory.Listener {

        @Override
        public void onLoaded(@NonNull Inventory.Products products) {
            final Inventory.Product product = products.get(ProductTypes.IN_APP);
            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();

            if (product.supported) {
                for (Sku sku : product.getSkus()) {
                    final Purchase purchase = product.getPurchaseInState(sku, Purchase.State.PURCHASED);
                    final SkuUi skuUi = new MySkuUi(sku, purchase != null);
                    mAdapter.add(skuUi);
                }

                // Sort items by prices.
                mAdapter.sort(new Comparator<SkuUi>() {
                    @Override
                    public int compare(@NonNull SkuUi l, @NonNull SkuUi r) {
                        return (int) (l.sku.detailedPrice.amount - r.sku.detailedPrice.amount);
                    }
                });

                // Show the inventory.
                refreshUi(SCREEN_INVENTORY);
            } else refreshUi(SCREEN_EMPTY_VIEW);

            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * @author Artem Chepurnoy
     */
    private abstract class BaseRequestListener<T> implements RequestListener<T> {

        @Override
        public void onError(int response, @NonNull Exception e) {
            ToastUtils.showShort(getActivity(), e.getLocalizedMessage());
        }
    }

    /**
     * @author Artem Chepurnoy
     */
    private final class PurchaseListener extends BaseRequestListener<Purchase> {

        @Override
        public void onSuccess(@NonNull Purchase purchase) {
            onPurchased(false);
        }

        @Override
        public void onError(int response, @NonNull Exception e) {
            switch (response) {
                case ResponseCodes.ITEM_ALREADY_OWNED:
                    onPurchased(true);
                    break;
                default:
                    super.onError(response, e);
            }
        }

        private void onPurchased(boolean alreadyOwned) {
            ToastUtils.showLong(getActivity(), R.string.donate_thanks);

            if (alreadyOwned) {
                // Nothing has changed, so we don't need
                // to reload the inventory.
                return;
            }

            reloadInventory();
        }
    }

    /**
     * @author Artem Chepurnoy
     */
    private static final class SkusAdapter extends BetterArrayAdapter<SkuUi> {

        /**
         * @author Artem Chepurnoy
         */
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
            bindItem((ViewHolder) viewHolder, getItem(i));
        }

        private static void bindItem(@NonNull ViewHolder holder, @NonNull SkuUi skuUi) {
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
    private static class MySkuUi extends SkuUi {

        public MySkuUi(@NonNull Sku sku, boolean isPurchased) {
            super(sku, isPurchased);
        }

        @NonNull
        @Override
        protected String onCreateDescription(@NonNull Sku sku) {
            /*
             * Those are highly app specific and should probably be
             * moved.
             */
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
    }
}
