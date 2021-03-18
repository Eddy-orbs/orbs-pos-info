package com.orbs.info.api;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

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

    private Handler homeCallback = null;
    private Handler eventsCallback = null;

    private int cachedDays;

    public InfoProvider() {
    }

    public static InfoProvider getInstance() {
        if (mInstance == null) {
            mInstance = new InfoProvider();
        }
        return mInstance;
    }

    private void loadAllInfo() {
        Log.d(LOG_TAG, "loadAllInfo");

        cachedTotalStake = Web3ApiManager.getInstance().getTotalStakedTokens();
        cachedUncap = Web3ApiManager.getInstance().getUncappedDelegatedStake();

        getEventsData(true);
        RestApiManager.getInstance().getNetworkStatus();

        lastCachedTimestamp = (new Timestamp(System.currentTimeMillis())).getTime(); // msec
    }

    private boolean needNetworkRefresh() {
        long currentTimestamp = (new Timestamp(System.currentTimeMillis())).getTime(); // msec
        long timeDiff = currentTimestamp - lastCachedTimestamp;
        Log.d(LOG_TAG, "needNetworkRefresh: TimeDiff=" + timeDiff);
        return (timeDiff > 60*60*1000); // 1 Hour diff
    }

    public long getTotalStake() {
        if (needNetworkRefresh()) {
            loadAllInfo();
        }
        return Util.convertToBigDecimal(cachedTotalStake.subtract(cachedUncap)).longValue();
    }

    // this must be the last one
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

    public void updateNetworkStatus(int numOfGuardian, long totalCommitteeStake) {
        cachedNumOfGuardians = numOfGuardian;
        cachedTotalCommitteeStake = totalCommitteeStake;

        if (homeCallback != null) {
            Message msg = new Message();
            msg.what = HomeFragment.GET_NETWORK_STATUS;
            msg.arg1 = cachedNumOfGuardians;
            msg.obj = cachedTotalCommitteeStake;
            homeCallback.sendMessage(msg);
        }
    }

    public void updateAllStakeEvents(String jsonResponse) {
        try {
            cachedAllEvents = (new JSONObject(jsonResponse)).optJSONArray("result");

            Log.d(LOG_TAG, "updateAllStakeEvents #: " + cachedAllEvents.length());

            // sort by timestamp
            ArrayList<JSONObject> listEvents = new ArrayList<JSONObject>();
            for (int i=0; i < cachedAllEvents.length(); i++) {
                listEvents.add(cachedAllEvents.getJSONObject(i));
            }
            Collections.sort(listEvents, new MyJSONComparator());
            cachedAllEvents = new JSONArray(listEvents);
            Log.d(LOG_TAG, cachedAllEvents.toString());
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

    public void getEventsData(boolean isForce) {
        Log.d(LOG_TAG, "getEventRawDataFromNetwork");

        if (needNetworkRefresh() || isForce) {
            // TODO: get Block number of prior 30 days
            long latestBlock = Web3ApiManager.getInstance().getLatestBlock().getNumber().longValue();
            long from = latestBlock - 220000; // 220K blocks == about 33 days
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

    public JSONArray getCachedEventsData() {
        return cachedAllEvents;
    }

    public void registerHomeCallback(Handler callback) {
        homeCallback = callback;
    }

    public void registerEventsCallback(Handler callback) {
        eventsCallback = callback;
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
