package com.orbs.info.ui.events;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.orbs.info.R;
import com.orbs.info.api.Web3ApiManager;
import com.orbs.info.util.Util;

import org.json.JSONObject;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class EventsListAdapter extends ArrayAdapter<JSONObject> {

    private final String LOG_TAG = "EventsListAdapter";

    private HashMap<Integer, Integer> visibleMap;

    public EventsListAdapter(Context context, ArrayList<JSONObject> eventsList)
    {
        super(context, android.R.layout.simple_list_item_1, eventsList.toArray(new JSONObject[eventsList.size()]));
        Log.d(LOG_TAG, "EventsListAdapter() - " + eventsList);
        visibleMap = new HashMap<Integer, Integer>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        //Log.d(LOG_TAG, "getView() - " + position);

        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_events, parent, false);
        }

        JSONObject eventItem = getItem(position);
        final int currentVisible = visibleMap.getOrDefault(position, View.GONE);

        Log.d(LOG_TAG, "getView() - " + position + " / " + currentVisible);

        TextView textTitle1 = convertView.findViewById(R.id.text_title_1);
        TextView textTitle2 = convertView.findViewById(R.id.text_title_2);
        TextView textAddress = convertView.findViewById(R.id.text_address);
        TextView textTime = convertView.findViewById(R.id.text_time);
        TextView textTxID = convertView.findViewById(R.id.text_txid);
        TextView textAmount = convertView.findViewById(R.id.text_amounts);
        TextView textAction = convertView.findViewById(R.id.text_action);

        String address = "0x" + eventItem.optJSONArray("topics").optString(1).substring(26);
        String jsonTimeStamp = eventItem.optString("timeStamp").substring(2); // remove "0x"
        String jsonBlock = eventItem.optString("blockNumber").substring(2); // remove "0x"
        String timeString = getDate((new BigInteger(jsonTimeStamp, 16)).longValue());
        String timeString2 = timeString + " UTC (# " + (new BigInteger(jsonBlock, 16).intValue()) + ")";

        String topic0 = eventItem.optJSONArray("topics").optString(0);
        String data1 = eventItem.optString("data").substring(2,66);
        BigInteger readInteger = new BigInteger(data1, 16);
        BigDecimal amount = Convert.fromWei(new BigDecimal(readInteger), Convert.Unit.ETHER);;

        int type = Util.getActionType(eventItem.optJSONArray("topics").optString(0));
        String actionType = "-";
        if (type == EventsListActivity.ACTION_STAKE) {
            actionType = "STAKE";
        } else if (type == EventsListActivity.ACTION_UNSTAKE) {
            actionType = "UNSTAKE";
        } else {
            actionType = "WITHDRAW";
        }

        textTitle1.setText(timeString);
        textTitle2.setText(String.format("%,d", amount.longValue()) + " ORBS");

        textAction.setText(actionType);
        textTime.setText(timeString2);
        textAddress.setText(address);
        textAddress.setPaintFlags(textAddress.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        textTxID.setText(eventItem.optString("transactionHash")); // TODO: link to etherscan
        textTxID.setPaintFlags(textTxID.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        textAmount.setText(String.format("%,d", amount.longValue()) + " ORBS");

        View layoutEventItem = convertView.findViewById(R.id.row_title);
        final View layoutContents = convertView.findViewById(R.id.row_contents);
        layoutContents.setVisibility(currentVisible);

        layoutEventItem.setOnClickListener(v -> {
            Log.d(LOG_TAG, "onClick() Title - " + position);
                toggleAnswerRow(layoutContents, position);
        });

//        layoutContents.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(LOG_TAG, "onClick() Body - " + position);
//                toggleAnswerRow(layoutContents);
//            }
//        });

        textTxID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DO what you want to recieve on btn click there.
                String text = ((TextView)v).getText().toString();
                //Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
                // Do something in response to button click
                String url = Web3ApiManager.ETHERSCAN_URL + "tx/" + text;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                getContext().startActivity(intent);
            }
        });

        textAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // DO what you want to recieve on btn click there.
                String text = ((TextView)v).getText().toString();
                //Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
                // Do something in response to button click
                String url = Web3ApiManager.ETHERSCAN_URL + "address/" + text;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                getContext().startActivity(intent);
            }
        });

        return convertView;
    }

    private String getDate(long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time * 1000);
        String date = DateFormat.format("yyyy-MM-dd (hh:mm)", cal).toString();
        return date;
    }

    private void toggleAnswerRow(View view, int position) {
        if (view.getVisibility() == View.VISIBLE) {
            visibleMap.put(position, View.GONE);
        } else {
            visibleMap.put(position, View.VISIBLE);
        }
        Util.toggleViewWithSlideAnimation(view);
    }
}