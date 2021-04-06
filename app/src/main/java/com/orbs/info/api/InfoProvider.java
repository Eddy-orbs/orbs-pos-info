package com.orbs.info.api;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.orbs.info.ui.calculation.CalculationFragment;
import com.orbs.info.ui.events.EventsFragment;
import com.orbs.info.ui.home.HomeFragment;
import com.orbs.info.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class InfoProvider {
    private static final String LOG_TAG = "InfoProvider";

    static private InfoProvider mInstance;

    private long lastCachedTimestamp;

    private JSONArray cachedAllEvents;

    private BigInteger cachedTotalStake;
    private BigInteger cachedUncap;

    private int cachedNumOfGuardians = 0;
    private long cachedTotalCommitteeStake = 0;
    private double cachedRewardsRate = 0;

    private double cachedETHprice = 0;
    private double cachedORBSprice = 0;
    private int cachedGasFee = 0;

    private Handler homeCallback = null;
    private Handler eventsCallback = null;
    private Handler calculatorCallback = null;

    private int cachedDays;

    public InfoProvider() {
    }

    public static InfoProvider getInstance() {
        if (mInstance == null) {
            mInstance = new InfoProvider();
        }
        return mInstance;
    }

    public void loadAllInfo() {
        Log.d(LOG_TAG, "loadAllInfo");

        cachedTotalStake = Web3ApiManager.getInstance().getTotalStakedTokens();
        cachedUncap = Web3ApiManager.getInstance().getUncappedDelegatedStake();

        // for Home fragment
        RestApiManager.getInstance().getNetworkStatus();

        // for Events fragment
        getEventsData(true);

        // for Calc fragment
        cachedGasFee = Web3ApiManager.getInstance().getGasPrice();
        RestApiManager.getInstance().getCoinPriceUSD();

        lastCachedTimestamp = (new Timestamp(System.currentTimeMillis())).getTime(); // msec

        // temp code: send noti
        getTotalStake(false);
        getGasFee(false);
    }

    private boolean needNetworkRefresh() {
        long currentTimestamp = (new Timestamp(System.currentTimeMillis())).getTime(); // msec
        long timeDiff = currentTimestamp - lastCachedTimestamp;
        Log.d(LOG_TAG, "needNetworkRefresh: TimeDiff=" + timeDiff);
        return (timeDiff > 60*60*1000); // 1 Hour diff
    }

    // get methods
    public void getTotalStake(boolean isForce) {
        if (needNetworkRefresh() || isForce) {
            loadAllInfo();
        } else {
            if (homeCallback != null && cachedTotalStake.compareTo(BigInteger.ZERO) > 0) {
                Message msg = new Message();
                msg.what = HomeFragment.GET_TOTAL_STAKE;
                msg.arg1 = cachedNumOfGuardians;
                msg.obj = Util.convertToBigDecimal(cachedTotalStake.subtract(cachedUncap)).longValue();
                homeCallback.sendMessage(msg);
            }
        }
    }

    // this must be the last one for home fragment
    public void getNetworkStatus(boolean isForce) {
        if (needNetworkRefresh() || isForce) {
            loadAllInfo();
        } else {
            if (homeCallback != null && cachedNumOfGuardians > 0) {
                Message msg = new Message();
                msg.what = HomeFragment.GET_NETWORK_STATUS;
                msg.arg1 = cachedNumOfGuardians;
                msg.obj = Long.valueOf(cachedTotalCommitteeStake);
                homeCallback.sendMessage(msg);
            }
        }
    }

    public void getGasFee(boolean isForce) {
        if (needNetworkRefresh() || isForce) {
            loadAllInfo();
        } else {
            if (calculatorCallback != null && cachedGasFee > 0) {
                Message msg = new Message();
                msg.what = CalculationFragment.GET_GAS_FEE;
                msg.arg1 = cachedGasFee;
                calculatorCallback.sendMessage(msg);
            }
        }
    }

    private boolean[] eventUpdatedFlag = {false, false, false};
    public void getEventsData(boolean isForce) {
        Log.d(LOG_TAG, "getEventRawDataFromNetwork");

        if (needNetworkRefresh() || isForce) {
            // TODO: get Block number of prior 30 days
            long latestBlock = Web3ApiManager.getInstance().getLatestBlock().getNumber().longValue();
            long from = latestBlock - 220000; // 220K blocks == about 33 days
            eventUpdatedFlag[0] = eventUpdatedFlag[1] = eventUpdatedFlag[2] = false;
            cachedAllEvents = new JSONArray();
            RestApiManager.getInstance().getAllStakingEvents(from);
        } else {
            if (eventsCallback != null && cachedAllEvents != null) {
                Message msg = new Message();
                msg.what = EventsFragment.GET_EVENTS;
                msg.obj = cachedAllEvents;
                eventsCallback.sendMessage(msg);
            }
        }
    }

    public void getTokenPrice(boolean isForce) {
        Log.d(LOG_TAG, "getTokenPrice");

        if (needNetworkRefresh() || isForce) {
            // TODO: get Block number of prior 30 days
            loadAllInfo();
        } else {
            if (calculatorCallback != null) {
                Message msg = new Message();
                msg.what = CalculationFragment.GET_ETH_USD_PRICE;
                msg.obj = (Double) cachedETHprice;
                calculatorCallback.sendMessage(msg);

                Message msg2 = new Message();
                msg2.what = CalculationFragment.GET_ORBS_USD_PRICE;
                msg2.obj = (Double) cachedORBSprice;
                calculatorCallback.sendMessage(msg2);
            }
        }
    }

    // update methods (update cached data and noti to UI if needed)
    public void updateNetworkStatus(int numOfGuardian, long totalCommitteeStake) {
        cachedNumOfGuardians = numOfGuardian;
        cachedTotalCommitteeStake = totalCommitteeStake;

        double maxCapAnnualReward = 80000000;
        double maxCapRate = 0.12;
        double percentage = Math.min((maxCapAnnualReward / cachedTotalCommitteeStake), maxCapRate);
        cachedRewardsRate = percentage * 0.6667; // default delegator's rate is hard-coded here

        if (homeCallback != null) {
            Message msg = new Message();
            msg.what = HomeFragment.GET_NETWORK_STATUS;
            msg.arg1 = cachedNumOfGuardians;
            msg.obj = cachedTotalCommitteeStake;
            homeCallback.sendMessage(msg);
        }
    }

    public void updateTokenPrice(double eth, double orbs) {
        cachedETHprice = eth;
        cachedORBSprice = orbs;

        if (calculatorCallback != null) {
            Message msg = new Message();
            msg.what = CalculationFragment.GET_ETH_USD_PRICE;
            msg.obj = (Double) cachedETHprice;
            calculatorCallback.sendMessage(msg);

//            msg.what = CalculationFragment.GET_ORBS_USD_PRICE;
//            msg.obj = (Double) cachedORBSprice;
//            calculatorCallback.sendMessage(msg);
        }
    }

    public void updateRewardsRate(double rate) {
        cachedRewardsRate = rate;
    }

    public double getRewardsRate() {
        return cachedRewardsRate;
    }

    public void updateStakeEventsByTopic(String jsonResponse, int topicIndex) {
        try {

//            cachedAllEvents = (new JSONObject(jsonResponse)).optJSONArray("result");
            JSONArray events = (new JSONObject(jsonResponse)).optJSONArray("result");

            Log.d(LOG_TAG, "updateStakeEventsByTopic index:" + topicIndex + "/#:" + events.length());

            if (eventUpdatedFlag[topicIndex] == false) {
                for (int i = 0; i < events.length(); i++) {
                    cachedAllEvents.put(events.optJSONObject(i));
                }
                eventUpdatedFlag[topicIndex] = true;
            }

            if (eventUpdatedFlag[0])  {
                if (eventUpdatedFlag[1]) {
                    if (eventUpdatedFlag[2]) {
                        // sort by timestamp
                        ArrayList<JSONObject> listEvents = new ArrayList<JSONObject>();
                        for (int i=0; i < cachedAllEvents.length(); i++) {
                            listEvents.add(cachedAllEvents.getJSONObject(i));
                        }
                        Collections.sort(listEvents, new MyJSONComparator());
                        cachedAllEvents = new JSONArray(listEvents);
                        Log.d(LOG_TAG, "updateStakeEventsByTopic: All done. length:" + cachedAllEvents.length());
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (eventsCallback != null) {
            Message msg = new Message();
            msg.what = EventsFragment.GET_EVENTS;
            msg.obj = cachedAllEvents;
            eventsCallback.sendMessage(msg);
        }
    }

    public JSONArray getCachedEventsData() {
        return cachedAllEvents;
    }

    public void registerHomeCallback(Handler callback) {
        homeCallback = callback;
    }

    public void registerEventsCallback(Handler callback) {
        eventsCallback = callback;
    }

    public void registerCalcCallback(Handler callback) {
        calculatorCallback = callback;
    }

    class MyJSONComparator implements Comparator<JSONObject> {
        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            String v1 = (String) o1.optString("timeStamp");
            String v3 = (String) o2.optString("timeStamp");
            return v3.compareTo(v1);
        }
    }
}
