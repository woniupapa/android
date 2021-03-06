/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.ui.loginpurchasing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.subscription.InAppPurchasesHelper;
import com.privateinternetaccess.android.pia.utils.DLog;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PurchasingFinalizeFragment extends Fragment {

    @BindView(R.id.fragment_finalize_cost) TextView tvCost;
    @BindView(R.id.fragment_finalize_email) TextView tvEmail;
    @BindView(R.id.fragment_finalize_plan_type) TextView tvPlan;

    public static String PRODUCT_ID_SELECTED;
    public String email;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_purchasing_finalize, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (email == null || email.length() <= 0) {
            email = PiaPrefHandler.getLoginEmail(getContext());
        }

        initView();
    }

    private void initView() {
        tvEmail.setText(email);

        InAppPurchasesHelper.SubscriptionType type = InAppPurchasesHelper.getType(PRODUCT_ID_SELECTED);

        if (type != null) {
            if (type == InAppPurchasesHelper.SubscriptionType.MONTHLY) {
                String monthly = getString(R.string.monthly_only);
                monthly = monthly.substring(0, 1).toUpperCase() + monthly.substring(1);

                tvPlan.setText(monthly);
            }
            else if (type == InAppPurchasesHelper.SubscriptionType.YEARLY) {
                String yearly = getString(R.string.yearly_only);
                yearly = yearly.substring(0, 1).toUpperCase() + yearly.substring(1);

                tvPlan.setText(yearly);
            }
        }

        setupCosts();
    }

    private void setupCosts() {
        String monthly = ((LoginPurchaseActivity) getActivity()).mMonthlyCost;
        String yearly = ((LoginPurchaseActivity) getActivity()).mYearlyCost;

        InAppPurchasesHelper.SubscriptionType type = InAppPurchasesHelper.getType(PRODUCT_ID_SELECTED);

        if (type != null) {
            if (type == InAppPurchasesHelper.SubscriptionType.YEARLY) {
                tvCost.setText(yearly);
            }
            else if (type == InAppPurchasesHelper.SubscriptionType.MONTHLY) {
                tvCost.setText(monthly);
            }
        }

        DLog.d("PurchasingFinalizeFragment", "monthly = " + monthly + " yearly = " + yearly);
    }

    @OnClick(R.id.fragment_purchasing_finalize_submit)
    public void onSubmitPressed() {
        DLog.d("Purchasing", "id = " + PRODUCT_ID_SELECTED);
        ((LoginPurchaseActivity) getActivity()).onSubscribeClicked(email, PRODUCT_ID_SELECTED);
    }
}
