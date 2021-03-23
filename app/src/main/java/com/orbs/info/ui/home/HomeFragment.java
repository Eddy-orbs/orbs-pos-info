package com.orbs.info.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.orbs.info.R;
import com.orbs.info.api.InfoProvider;

import java.text.DecimalFormat;

public class HomeFragment extends Fragment {
    private static final String LOG_TAG = "HomeFragment";

    public static final int GET_NETWORK_STATUS = 1000;
    public static final int GET_TOTAL_STAKE = 1001;

    private View root;
    private long totalStake = 0;
    private int numberOfGuardians = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");

        root = inflater.inflate(R.layout.fragment_home, container, false);

        Handler handler = new Handler(Looper.myLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadInfo(false);
            }
        }, 0);

        setButtonListener();

        setPullDownReloadListener();

        return root;
    }

    private void loadInfo(boolean isForce) {
        // load and update total stake
        InfoProvider.getInstance().getTotalStake(isForce);

        // load and update # of guardians
        InfoProvider.getInstance().getNetworkStatus(isForce);
    }



    private Handler networkStatusReceiver = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case GET_NETWORK_STATUS:
                    // num of guardians
                    numberOfGuardians = msg.arg1;
                    TextView tvNumOfGuardians = root.findViewById(R.id.text_active_guardian_number);
                    tvNumOfGuardians.setText(String.format("%,d", numberOfGuardians));

                    // rewards rate
                    double percentage = InfoProvider.getInstance().getRewardsRate();

                    TextView tvRewardRate = root.findViewById(R.id.text_reward_rate);
                    String rateStr = (new DecimalFormat("#.##%").format(percentage));
                    tvRewardRate.setText(rateStr);

                    Log.d(LOG_TAG, "handleMessage:" + rateStr);

                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                    break;

                case GET_TOTAL_STAKE:
                    totalStake = (Long) msg.obj;
                    Log.d(LOG_TAG, "handleMessage2:" + totalStake);
                    TextView tvTotalStake = root.findViewById(R.id.text_total_stake);
                    tvTotalStake.setText(String.format("%,d", totalStake));

                    break;
                default:
                    break;
            }
            return true;
        }
    });

    private void setButtonListener() {

        // button to pos wallet
        Button btnStake = (Button) root.findViewById(R.id.button_stake);
        btnStake.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                String url = "https://play.google.com/store/apps/details?id=com.orbs.pos";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        // button to guardian list
        Button btnGuardian = (Button) root.findViewById(R.id.button_guardians_list);
        btnGuardian.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                String url = "https://status.orbs.network/";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        // button to calculator
        Button btnCalculator = (Button) root.findViewById(R.id.button_goto_calculation);
        btnCalculator.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                navController.navigate(R.id.navigation_calculator);
            }
        });

        // button to analytics
        TextView toAnalytics = (TextView) root.findViewById(R.id.button_to_analytics);
        toAnalytics.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
                String url = "https://analytics.orbs.network/";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");

        InfoProvider.getInstance().registerHomeCallback(networkStatusReceiver);
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause()");

        InfoProvider.getInstance().registerHomeCallback(null);
        super.onPause();
    }

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private void setPullDownReloadListener() {
        mSwipeRefreshLayout = root.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(LOG_TAG, "SwipeRefresh - onRefresh()");
                loadInfo(true);
            }
        });
    }
}