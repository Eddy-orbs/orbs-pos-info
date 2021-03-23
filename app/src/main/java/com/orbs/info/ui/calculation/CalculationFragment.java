package com.orbs.info.ui.calculation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.orbs.info.R;
import com.orbs.info.api.InfoProvider;
import com.orbs.info.util.Util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class CalculationFragment extends Fragment {
    private static final String LOG_TAG = "CalculationFragment";

    public static final int GET_ETH_USD_PRICE = 1000;
    public static final int GET_ORBS_USD_PRICE = 1001;
    public static final int GET_GAS_FEE = 1002;

    private View root;

    private double ethPriceUsd;
    private double orbsPriceUsd;
    private int currentGasFee;

    private double rewardsRate;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_calculation, container, false);
        InfoProvider.getInstance().registerCalcCallback(calculationInfoReceiver);

        InfoProvider.getInstance().getGasFee(false);
        InfoProvider.getInstance().getTokenPrice(false);
        rewardsRate = InfoProvider.getInstance().getRewardsRate();

        //rewardsRate = InfoProvider.getInstance().getRewardsRate();

        setPullDownReloadListener();

        Button btnCalculate = (Button) root.findViewById(R.id.button_calculate);
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                updateDisplay();
            }
        });

        // set Text change listener
        EditText inputAmountsText = root.findViewById(R.id.edit_amount);
        inputAmountsText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateDisplay();
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });

        return root;
    }

    private void updateDisplay() {
        // Do calculate rewards and display
        double rewardsYear = 0;
        double rewardsWeek = 0;
        try {
            EditText inputAmountsText = root.findViewById(R.id.edit_amount);
            BigDecimal inputAmount = new BigDecimal(inputAmountsText.getText().toString());
            rewardsRate = InfoProvider.getInstance().getRewardsRate();
            if (rewardsRate == 0) {
                InfoProvider.getInstance().loadAllInfo();
            }
            rewardsYear = inputAmount.doubleValue() * rewardsRate;
            rewardsWeek = rewardsYear / 365 * 7;
        } catch (NumberFormatException e) {
            e.printStackTrace();
//            Toast.makeText(getContext(), "Please input number", Toast.LENGTH_SHORT).show();
        }

        TextView weekOrbs = root.findViewById(R.id.text_rate_week);
        TextView weekUsd = root.findViewById(R.id.text_rate_week_usd);
        TextView yearOrbs = root.findViewById(R.id.text_rate_year);
        TextView yearUsd = root.findViewById(R.id.text_rate_year_usd);

        weekOrbs.setText(Util.formatToString(BigDecimal.valueOf(rewardsWeek), 2));
        weekUsd.setText("(" + Util.formatToString(BigDecimal.valueOf(rewardsWeek * orbsPriceUsd), 2) + " USD)");
        yearOrbs.setText(Util.formatToString(BigDecimal.valueOf(rewardsYear), 2));
        yearUsd.setText("(" + Util.formatToString(BigDecimal.valueOf(rewardsYear * orbsPriceUsd), 2) + " USD)");

        // Do calculate fees and display
        TextView gasGwei = root.findViewById(R.id.text_gwei);
        gasGwei.setText(Integer.toString(currentGasFee) + " Gwei");

        TextView gas1 = root.findViewById(R.id.text_gas_new);
        TextView gas1Usd = root.findViewById(R.id.text_gas_new_usd);
        TextView gas2 = root.findViewById(R.id.text_gas_increase);
        TextView gas2Usd = root.findViewById(R.id.text_gas_increase_usd);
        TextView gas3 = root.findViewById(R.id.text_gas_unstake);
        TextView gas3Usd = root.findViewById(R.id.text_gas_unstake_usd);
        TextView gas4 = root.findViewById(R.id.text_gas_claim1);
        TextView gas4Usd = root.findViewById(R.id.text_gas_claim1_usd);
        TextView gas5 = root.findViewById(R.id.text_gas_claim2);
        TextView gas5Usd = root.findViewById(R.id.text_gas_claim2_usd);

        // https://github.com/orbs-network/orbs-ethereum-contracts-v2/blob/master/GAS.md
        int[] gasCostBase = {292837, 214802, 188899, 342404, 272122};
        TextView[] arrayGas = {gas1, gas2, gas3, gas4, gas5};
        TextView[] arrayGasUsd = {gas1Usd, gas2Usd, gas3Usd, gas4Usd, gas5Usd};

        for (int i=0; i < 5; i++) {
            double gasFee = (double)gasCostBase[i] * (double)currentGasFee / (double)1000000000;
            arrayGas[i].setText(Util.formatToString(BigDecimal.valueOf(gasFee), 6));
            arrayGasUsd[i].setText("(" + Util.formatToString(BigDecimal.valueOf(gasFee * ethPriceUsd), 2) + " USD)");
        }
    }

    private Handler calculationInfoReceiver = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case GET_ETH_USD_PRICE:
                    ethPriceUsd = (Double) (msg.obj);
                    break;
                case GET_ORBS_USD_PRICE:
                    orbsPriceUsd = (Double) (msg.obj);
                    break;
                case GET_GAS_FEE:
                    currentGasFee = msg.arg1;
                    break;
                default:
                    break;
            }

            updateDisplay();

            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            return true;
        }
    });


    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
        rewardsRate = InfoProvider.getInstance().getRewardsRate();
        InfoProvider.getInstance().registerCalcCallback(calculationInfoReceiver);
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause()");
        InfoProvider.getInstance().registerCalcCallback(null);
        super.onPause();
    }

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private void setPullDownReloadListener() {
        mSwipeRefreshLayout = root.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(LOG_TAG, "SwipeRefresh - onRefresh()");
                InfoProvider.getInstance().loadAllInfo();
            }
        });
    }
}