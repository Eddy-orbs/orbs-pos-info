package com.orbs.info.api;

import android.content.Context;
import android.util.Log;

//import com.orbs.pos.Feature;
//import com.orbs.pos.data.DataManager;
//import com.orbs.pos.data.Guardian;
//import com.orbs.pos.data.Rewards;
//import com.orbs.pos.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.*;

import cz.msebera.android.httpclient.Header;

public class RestApiManager {
    private static final String LOG_TAG = "RestApiManager";

    // TODO: redefine api links
    static final private String ETHERSCAN_API_URL = "https://api.etherscan.io/api?module=logs&action=getLogs&fromBlock=10400000&toBlock=latest";
    static final private String ETHERSCAN_V1_API_URL = "https://api.etherscan.io/api?module=logs&action=getLogs&fromBlock=8048900&toBlock=latest";
    static final private String ETHERSCAN_API_KEY = "&apikey=4IJ1V34SP4R5DKCGWY7W6GWUU9E2Y79MTT";
    static final private String ETHERSCAN_STAKING_DIST_CONTRACT_TOPIC0 = "&address=0xB52daF3f853bF570814d6AeA1ec7BFF30339BD0c&topic0=0x8b66abc81eb9e4b88acacafcc6d75ece18e30943c5ac9d9fd331ddb42eaffe3c";
    static final private String ETHERSCAN_STAKING_DIST_API = ETHERSCAN_API_URL + ETHERSCAN_STAKING_DIST_CONTRACT_TOPIC0 + ETHERSCAN_API_KEY;

    static final private String ETHERSCAN_STAKING_NO_LOCK_DIST_CONTRACT_TOPIC0 = "&address=0xb2969e54668394bcA9B8AF61bC39B92754b7A7a0&topic0=0x8b66abc81eb9e4b88acacafcc6d75ece18e30943c5ac9d9fd331ddb42eaffe3c";
    static final private String ETHERSCAN_STAKING_NO_LOCK_DIST_API = ETHERSCAN_V1_API_URL + ETHERSCAN_STAKING_NO_LOCK_DIST_CONTRACT_TOPIC0 + ETHERSCAN_API_KEY;

    static final private String ORBS_V2_GUARDIAN_API = "http://54.241.122.39/services/management-service/status";
    static final private String ORBS_V2_GUARDIAN_API_SUB = "http://54.180.218.84/services/management-service/status";


    static public RestApiManager mInstance;
    private Context mContext;

    private int numOfGuardians = 0;

    private RestApiManager(Context context) {
        mContext = context;
    }

    public static RestApiManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new RestApiManager(context);
        } else {
            mInstance.setContext(context);
        }
        return mInstance;
    }

    public static RestApiManager getInstance() {
        return getInstance(null);
    }

    private void setContext(Context context) {
        mContext = context;
    }

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    public void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        httpClient.get(url, params, responseHandler);
    }

    public void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        httpClient.post(url, params, responseHandler);
    }

    public void getOrbsFiatPrice() {
        // Instantiate the RequestQueue.

    }

//    public String getEthFiatPrice(Handler callback) {
//        // Instantiate the RequestQueue.
//
//        return get(PRICE_ETH_API_URL);
//    }

    public void getNetworkStatus() {
        JsonHttpResponseHandler httpResponseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d(LOG_TAG, "getNetworkStatus: onSuccess");
                //Log.d(LOG_TAG, response.toString());

                try {

                    // send # of Guardians
                    JSONObject jsonAllRegistered = response.getJSONObject("AllRegisteredNodes");
                    int numOfGuardian = jsonAllRegistered.length();
                    Log.d(LOG_TAG, "numOfGuardian=" + numOfGuardian);

                    // send total Committee stake number
                    JSONObject jsonCommitteeNodes = response.getJSONObject("CommitteeNodes");
                    JSONArray names = jsonCommitteeNodes.names();
                    long totalCommitteeStake = 0;
                    for(int i=0; i<jsonCommitteeNodes.length(); i++) {
                        JSONObject guardian = jsonCommitteeNodes.getJSONObject((String)names.get(i));
                        totalCommitteeStake += guardian.optLong("EffectiveStake", 0);
                    }

                    InfoProvider.getInstance().updateNetworkStatus(numOfGuardian, totalCommitteeStake);

                } catch (JSONException e) {
                    // handle exception
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        };

        get(API.ORBS_WEB_API_URL, null, httpResponseHandler);
    }

    public void getAllStakingEvents(long targetBlock) {
        getStakingEventsByTopic(API.CONTRACT_STAKE_TOPIC_STAKE, targetBlock, 0);
        getStakingEventsByTopic(API.CONTRACT_STAKE_TOPIC_UNSTAKE, targetBlock, 1);
        getStakingEventsByTopic(API.CONTRACT_STAKE_TOPIC_WITHDREW, targetBlock, 2);
    }

    public void getStakingEventsByTopic(String topic0, long targetBlock, int topicIndex) {
        JsonHttpResponseHandler httpResponseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d(LOG_TAG, "getAllStakingEvents: onSuccess");
                InfoProvider.getInstance().updateStakeEventsByTopic(response.toString(), topicIndex);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        };

        RequestParams params = new RequestParams();
        //params.put("topic0", topic0);
        params.put("action", "getLogs");
        params.put("address", API.CONTRACT_STAKE);
        params.put("apikey", API.ETHERSCAN_API_KEY);
        params.put("topic0", topic0);
        params.put("module", "logs");
        params.put("toBlock", "latest");
        params.put("fromBlock", targetBlock);

        get(API.ETHERSCAN_API_URL, params, httpResponseHandler);
    }

    public void getCoinPriceUSD() {
        JsonHttpResponseHandler httpResponseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                double eth, orbs;
                eth = response.optJSONObject("ETH").optDouble("USD");
                orbs = response.optJSONObject("ORBS").optDouble("USD");

                Log.d(LOG_TAG, "getAllStakingEvents: onSuccess - ETHUSD:" + eth + "/ ORBSUSD:" + orbs);

                InfoProvider.getInstance().updateTokenPrice(eth, orbs);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        };

        get(API.TOKEN_PRICE_API_URL, null, httpResponseHandler);
    }
}
