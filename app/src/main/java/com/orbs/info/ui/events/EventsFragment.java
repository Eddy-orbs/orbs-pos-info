package com.orbs.info.ui.events;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.orbs.info.R;
import com.orbs.info.api.API;
import com.orbs.info.api.InfoProvider;
import com.orbs.info.util.SharedPreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;

public class EventsFragment extends Fragment {
    private static final String LOG_TAG = "EventsFragment";

    public static final int GET_EVENTS = 1001;

    private View root;

    private int defaultDays;

    private Button prevSelectedButton = null;
    private Button btn3Days;
    private Button btn7Days;
    private Button btn30Days;

    private boolean dataReceived = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");

        InfoProvider.getInstance().registerEventsCallback(eventsReceiver);

        root = inflater.inflate(R.layout.fragment_events, container, false);

        btn3Days = (Button) root.findViewById(R.id.button_3days);
        btn7Days = (Button) root.findViewById(R.id.button_7days);
        btn30Days = (Button) root.findViewById(R.id.button_30days);

        selectDefaultPeriod();
        setButtonListener();

        setPullDownReloadListener();

        return root;
    }

    private void selectDefaultPeriod() {
        defaultDays = SharedPreferenceManager.getDefaultPeriod(getContext());
        setPrevSelectedPeriod(defaultDays);
        setButtonColor(prevSelectedButton, R.drawable.button_custom_round2);
    }

    private void setPrevSelectedPeriod(int period) {
        if (period == 3) {
            prevSelectedButton = btn3Days;
        } else if (period == 7) {
            prevSelectedButton = btn7Days;
        } else {
            prevSelectedButton = btn30Days;
        }

        SharedPreferenceManager.setDefaultPeriod(getContext(), period);
        defaultDays = period;

        // retrieve data
        InfoProvider.getInstance().getEventsData(false);
    }

    private void setButtonListener() {
        // button to calculator

        btn3Days.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
//                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
//                navController.navigate(R.id.navigation_calculator);

                setButtonColor(btn3Days, R.drawable.button_custom_round2);
                setButtonColor(btn7Days, R.drawable.button_custom_round);
                setButtonColor(btn30Days, R.drawable.button_custom_round);

                setPrevSelectedPeriod(3);
            }
        });

        btn7Days.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
//                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
//                navController.navigate(R.id.navigation_calculator);

                setButtonColor(btn3Days, R.drawable.button_custom_round);
                setButtonColor(btn7Days, R.drawable.button_custom_round2);
                setButtonColor(btn30Days, R.drawable.button_custom_round);

                setPrevSelectedPeriod(7);
            }
        });

        btn30Days.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
//                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
//                navController.navigate(R.id.navigation_calculator);

                setButtonColor(btn3Days, R.drawable.button_custom_round);
                setButtonColor(btn7Days, R.drawable.button_custom_round);
                setButtonColor(btn30Days, R.drawable.button_custom_round2);

                setPrevSelectedPeriod(30);
            }
        });

        // Buttons to open list
        Button btnListStaked = (Button) root.findViewById(R.id.button_stake);
        Button btnListUnStaked = (Button) root.findViewById(R.id.button_unstake);
        Button btnListWithdrew = (Button) root.findViewById(R.id.button_withdraw);

        btnListStaked.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!dataReceived) return;
                Intent intent = new Intent(getActivity(), EventsListActivity.class);
                intent.putExtra(EventsListActivity.ACTION_TYPE, EventsListActivity.ACTION_STAKE);
                startActivity(intent);
            }
        });

        btnListUnStaked.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!dataReceived) return;
                Intent intent = new Intent(getActivity(), EventsListActivity.class);
                intent.putExtra(EventsListActivity.ACTION_TYPE, EventsListActivity.ACTION_UNSTAKE);
                startActivity(intent);
            }
        });

        btnListWithdrew.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!dataReceived) return;
                Intent intent = new Intent(getActivity(), EventsListActivity.class);
                intent.putExtra(EventsListActivity.ACTION_TYPE, EventsListActivity.ACTION_WITHDRAW);
                startActivity(intent);
            }
        });
    }

    private void setButtonColor(Button btn, int id) {
        btn.setBackground(getActivity().getDrawable(id));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
        InfoProvider.getInstance().registerEventsCallback(eventsReceiver);
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause()");
        InfoProvider.getInstance().registerEventsCallback(null);
        super.onPause();
    }

    private Handler eventsReceiver = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case GET_EVENTS:
                    JSONArray eventsArray = (JSONArray) msg.obj;
                    Log.d(LOG_TAG, "handleMessage:" + eventsArray);
                    parseAndSumForEachEvent(eventsArray);
                    dataReceived = true;
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    private void parseAndSumForEachEvent(JSONArray eventsArray) {
        Log.d(LOG_TAG, "parseAndSumForEachEvent: days=" + defaultDays);

        BigDecimal stakedAmount = BigDecimal.ZERO;
        BigDecimal unstakedAmount = BigDecimal.ZERO;
        BigDecimal withdrewAmount = BigDecimal.ZERO;

        long targetTimestamp = (new Timestamp(System.currentTimeMillis())).getTime() / 1000; // sec
        targetTimestamp = targetTimestamp - (60 * 60 * 24 * defaultDays);

        for (int i = 0; i < eventsArray.length(); i++) {
            JSONObject currentEvent = eventsArray.optJSONObject(i);

            String jsonTimeStamp = currentEvent.optString("timeStamp").substring(2); // remove "0x"
            long eventTimestamp = (new BigInteger(jsonTimeStamp, 16)).longValue();
            if (eventTimestamp < targetTimestamp) {
                continue;
            }

            String topic0 = currentEvent.optJSONArray("topics").optString(0);
            String data1 = currentEvent.optString("data").substring(2,66);
            BigInteger readInteger = new BigInteger(data1, 16);
            BigDecimal amount = Convert.fromWei(new BigDecimal(readInteger), Convert.Unit.ETHER);;

            //Log.d(LOG_TAG, "parseAndSumForEachEvent - amount = " + amount + " / data1 = " + data1);

            if (API.CONTRACT_STAKE_TOPIC_STAKE.equals(topic0)) {
                stakedAmount = stakedAmount.add(amount);
            } else if (API.CONTRACT_STAKE_TOPIC_RESTAKE.equals(topic0)) {
                stakedAmount = stakedAmount.add(amount);
            } else if (API.CONTRACT_STAKE_TOPIC_UNSTAKE.equals(topic0)) {
                unstakedAmount = unstakedAmount.add(amount);
            } else if (API.CONTRACT_STAKE_TOPIC_WITHDREW.equals(topic0)) {
                withdrewAmount = withdrewAmount.add(amount);
            } else {
                Log.d(LOG_TAG, "parseAndSumForEachEvent: other Topic = " + topic0);
            }
        }

        TextView tvStaked = root.findViewById(R.id.text_total_stake);
        TextView tvUnstaked = root.findViewById(R.id.text_total_unstake);
        TextView tvWithdrew = root.findViewById(R.id.text_total_withdrew);

        tvStaked.setText(String.format("%,d", stakedAmount.longValue()));
        tvUnstaked.setText(String.format("%,d", unstakedAmount.longValue()));
        tvWithdrew.setText(String.format("%,d", withdrewAmount.longValue()));

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private void setPullDownReloadListener() {
        mSwipeRefreshLayout = root.findViewById(R.id.swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(LOG_TAG, "SwipeRefresh - onRefresh()");
                InfoProvider.getInstance().getEventsData(true);
            }
        });
    }
}