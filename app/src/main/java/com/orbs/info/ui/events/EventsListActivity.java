package com.orbs.info.ui.events;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.orbs.info.R;
import com.orbs.info.api.API;
import com.orbs.info.api.InfoProvider;
import com.orbs.info.util.SharedPreferenceManager;
import com.orbs.info.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;

public class EventsListActivity extends AppCompatActivity {
    private static final String LOG_TAG = "EventsListActivity";

    static final public String ACTION_TYPE = "TYPE";
    static final public int ACTION_UNKNOWN = 0;
    static final public int ACTION_STAKE = 1;
    static final public int ACTION_UNSTAKE = 2;
    static final public int ACTION_WITHDRAW = 3;

    private int currentType = ACTION_STAKE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_list);

        if (getIntent() != null) {
            currentType = getIntent().getIntExtra(ACTION_TYPE, ACTION_STAKE);
            Log.d(LOG_TAG, "onCreate() - TYPE:" + currentType);
        }

        // set toolbar
//        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String title = "";
        if (currentType == ACTION_STAKE) {
            title += "STAKED";
        } else if (currentType == ACTION_UNSTAKE) {
            title += "UNSTAKED";
        } else {
            title += "WITHDREW";
        }
        title += " Events list";
        getSupportActionBar().setTitle(title);

        // TODO: change font and color in Actionbar
//        View titleView = getSupportActionBar().getCustomView();
//        titleView.set


        getListToDisplayFromAllData();

        TextView countText = findViewById(R.id.text_count);
        int defaultDays = SharedPreferenceManager.getDefaultPeriod(this);
        String countTextStr = String.format("There are %d events during last %d days", jsonDataList.size(), defaultDays);
        countText.setText(countTextStr);

        ListView eventsListView = findViewById(R.id.event_listview);
        EventsListAdapter myAdapter = new EventsListAdapter(this, jsonDataList);
        eventsListView.setAdapter(myAdapter);
    }

    ArrayList<JSONObject> jsonDataList;
    private void getListToDisplayFromAllData(){
        jsonDataList = new ArrayList<>();

        JSONArray allData = InfoProvider.getInstance().getCachedEventsData();

        int defaultDays = SharedPreferenceManager.getDefaultPeriod(this);
        long targetTimestamp = (new Timestamp(System.currentTimeMillis())).getTime() / 1000; // sec
        targetTimestamp = targetTimestamp - (60 * 60 * 24 * defaultDays);

        for (int i=0; i < allData.length(); i++) {
            JSONObject currentEvent = allData.optJSONObject(i);

            String jsonTimeStamp = currentEvent.optString("timeStamp").substring(2); // remove "0x"
            long eventTimestamp = (new BigInteger(jsonTimeStamp, 16)).longValue();
            if (eventTimestamp < targetTimestamp) {
                continue;
            }

            int type = Util.getActionType(currentEvent.optJSONArray("topics").optString(0));
            if (type == currentType) {
                jsonDataList.add(currentEvent);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}