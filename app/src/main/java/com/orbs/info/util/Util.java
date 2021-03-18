package com.orbs.info.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.app.PendingIntent;
import android.app.AlarmManager;

import androidx.appcompat.app.AlertDialog;

//import com.google.zxing.BarcodeFormat;
//import com.google.zxing.MultiFormatWriter;
//import com.google.zxing.WriterException;
//import com.google.zxing.common.BitMatrix;
//import com.journeyapps.barcodescanner.BarcodeEncoder;
//import com.orbs.pos.AlarmReceiver;
//import com.orbs.pos.DeviceBootReceiver;
//import com.orbs.pos.R;
//import com.orbs.pos.api.KeyStoreManager;
//import com.orbs.pos.data.ErrorCode;
//import com.orbs.pos.data.WalletMode;
//import com.samsung.android.sdk.coldwallet.ScwErrorCode;
//import com.tomergoldst.tooltips.ToolTip;
//import com.tomergoldst.tooltips.ToolTipsManager;

import com.orbs.info.api.API;
import com.orbs.info.ui.events.EventsListActivity;
import com.orbs.info.ui.events.EventsListAdapter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Util {

    public static final String LOG_TAG = "Util";
    public static final String HD_PATH_PREFIX = "m/44'/60'/0'/0/";
    public static final String ACCOUNT_NAME_PREFIX = "Account ";
    public static final String DB_NAME = "open_crypto_wallet_db";
    public static final int defaultAccountId = AccountID.ID_1;
    public static final int defaultTxSpeed = TransactionSpeed.FAST;
    public static final int defaultGasFee = 10;
    public static final long defaultGasLimit = 800000;
    private static int qrCodeWidth = 10;
    private static int qrCodeHeight = 10;
    public static int requiredAPILevel = 4;
    public static final double DEFAULT_GUARDIAN_SETTING_PERCENT = Double.parseDouble("66667"+"E-5") ; //0.66667;

    public static final String ACTION_USB_PERMISSION = "com.orbs.pos.ledger.USB_PERMISSION";

//    public static boolean isAPILevelMatched(Context context) {
//        int APILevel = KeyStoreManager.getInstance(context).getKeystoreApiLevel();
//        if (APILevel < requiredAPILevel) {
//            Log.e(Util.LOG_TAG, "API Level is used is below required level-" + APILevel);
//            return false;
//        } else {
//            Log.i(Util.LOG_TAG, "API Level is used meets required level-" + APILevel);
//            return true;
//        }
//    }

    public static void launchDeepLink(Context context, String uriString) {
        Uri uri = Uri.parse(uriString);
        Intent displayIntent = new Intent(Intent.ACTION_VIEW, uri);
        displayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(displayIntent);
    }

    public static ArrayList<String> stringToArrayList(String inputString) {
        return new ArrayList<String>(Arrays.asList(inputString));
    }

//    public static Bitmap generateQRCode(String text) {
//        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
//        Bitmap bitmap = Bitmap.createBitmap(qrCodeWidth, qrCodeHeight, Bitmap.Config.RGB_565);
//        try {
//            BitMatrix bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, 1000, 1000);
//            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
//            bitmap = barcodeEncoder.createBitmap(bitMatrix);
//        } catch (WriterException e) {
//            e.printStackTrace();
//        }
//
//        return bitmap;
//    }

//    public static AlertDialog alertDialogMessage(Context context, String msg) {
//        return alertDialogMessage(context, msg, null);
//    }

//    public static AlertDialog alertDialogMessage(Context context, String msg, final Callback callback) {
//        AlertDialog.Builder alert = new AlertDialog.Builder(context, R.style.MyAlertDialog);
//        alert.setPositiveButton(R.string.common_button_close, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        alert.setMessage(msg);
//
//        AlertDialog dialog = alert.create();
//        dialog.show();
//        dialog.getWindow().getDecorView().setBackgroundColor(context.getColor(R.color.dialog_bg));
//        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        TextView textView = dialog.findViewById(android.R.id.message);
//        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
//                context.getResources().getDimension(R.dimen.alert_message_size));
//
//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                dialog.dismiss();
//                if (callback != null) {
//                    callback.callBackMethod();
//                }
//            }
//        });
//
//        return dialog;
//    }

    public static void setVisibility(View view, int flag) {
        view.setVisibility(flag);
    }

    public static String getSHA256(String str){
        String SHA = "";
        try{
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte byteData[] = sh.digest();
            StringBuffer sb = new StringBuffer();
            for(int i = 0 ; i < byteData.length ; i++){
                sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
            }
            SHA = sb.toString();

        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            SHA = null;
        }
        return SHA;
    }

//    public static String getErrorMessage(Context context, int errorCode) {
//        String stringMessage;
//
//        switch (errorCode) {
//            case ErrorCode.ERROR_NONE: {
//                stringMessage = context.getText(R.string.common_message_transaction_success).toString();
//                break;
//            }
//            case ErrorCode.ERROR_SCW_NOT_SUPPORT:
//                stringMessage = context.getText(R.string.error_scw_not_support).toString();
//                break;
//            case -32000:
//            case ScwErrorCode.ERROR_INSUFFICIENT_FUNDS:
//                stringMessage = context.getText(R.string.error_insufficient_funds).toString();
//                break;
//            case ScwErrorCode.ERROR_OP_INTERRUPTED:
//                stringMessage = context.getText(R.string.error_user_canceled).toString();
//                break;
//            case ScwErrorCode.ERROR_OUT_OF_BOUND_VALUE:
//                stringMessage = context.getText(R.string.error_out_of_bound).toString();
//                break;
//            case ScwErrorCode.ERROR_PACKAGE_SIGNATURE_VERIFICATION_FAILED:
//                stringMessage = context.getText(R.string.error_app_verification_fail).toString();
//                break;
//            case ErrorCode.ERROR_LEDGER_CONNECTION:
//                stringMessage = context.getText(R.string.error_ledger_connection).toString();
//                break;
//            case ErrorCode.ERROR_LEDGER_FAILED:
//                stringMessage = context.getText(R.string.error_ledger_sign_fail).toString();
//                break;
//            case -1:
//            default:
//                stringMessage = "Error! Please try again (Code:" + errorCode + ")";
//                break;
//        }
//        return stringMessage;
//    }

    public static void toggleViewWithSlideAnimation(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            slideUp(view);
        } else {
            slideDown(view);
        }
    }

    private static void slideDown(final View view) {
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0.f);

        if (view.getHeight() > 0) {
            slideDownNow(view);
        } else {
            // wait till height is measured
            view.post(new Runnable() {
                @Override
                public void run() {
                    slideDownNow(view);
                }
            });
        }
    }

    private static void slideDownNow(final View view) {
        view.setTranslationY(-50.f);
        view.animate()
                .translationY(0)
                .alpha(1.f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.VISIBLE);
                        view.setAlpha(1.f);
                    }
                });
    }

    private static void slideUp(final View view) {
        view.animate()
                .translationY(0.f)
                .alpha(1.f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        // superfluous restoration
                        view.setVisibility(View.GONE);
                        view.setAlpha(1.f);
                        view.setTranslationY(view.getHeight());
                    }
                });
    }

//    public static void showTooltip(Context context, ToolTipsManager manager, View anchorView, ViewGroup viewGroup, String message) {
//        View toolView = manager.find(anchorView.getId());
//        if (toolView != null && manager.isVisible(toolView)) {
//            manager.dismissAll();
//            return;
//        }
//        manager.dismissAll();
//
//        // tooltip github: https://github.com/tomergoldst/tooltips
//        ToolTip.Builder builder = new ToolTip.Builder(context, anchorView, viewGroup, message, ToolTip.POSITION_BELOW);
//        builder.setBackgroundColor(context.getColor(R.color.tooltip_bg_color));
//        builder.setGravity(ToolTip.GRAVITY_CENTER);
//        builder.setTextAppearance(R.style.TooltipTextAppearance);
//
//        manager.show(builder.build());
//    }

    public static BigDecimal convertToBigDecimal(BigInteger value) {
//        if (Feature.ROPSTEN_TESTNET)
//            return new BigDecimal(value);
//        else
        return Convert.fromWei(new BigDecimal(value), Convert.Unit.ETHER);
    }

    public static BigInteger convertToBigInteger(BigDecimal value) {
//        if (Feature.ROPSTEN_TESTNET)
//            return value.toBigInteger();
//        else
        return Convert.toWei(value.toString(), Convert.Unit.ETHER).toBigInteger();
    }

    public static BigInteger convertToBigInteger(String value) {
//        if (Feature.ROPSTEN_TESTNET)
//            return new BigInteger(value);
//        else
        return Convert.toWei(value, Convert.Unit.ETHER).toBigInteger();
    }

    public static String formatToString(BigInteger inputNum) {
        return formatToString(new BigDecimal(inputNum));
    }

    public static String formatToString(BigDecimal inputNum) {
        return formatToString(inputNum, 0);
    }

    public static String formatToString(BigDecimal inputNum, int decimals) {
        String pattern = ",###";
        if (decimals > 0) {
            pattern += ".";
            while (decimals > 0) {
                pattern += "#";
                decimals--;
            }
        }
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        decimalFormat.setRoundingMode(RoundingMode.FLOOR);
        return decimalFormat.format(inputNum.doubleValue());
    }

    public static String trimCommaOfString(String string) {
        if(string.contains(",")){
            return string.replace(",","");}
        else {
            return string;
        }
    }

    public class TransactionSpeed {
        public static final int CUSTOM = 0;
        public static final int SLOW = 1;
        public static final int AVERAGE = 2;
        public static final int FAST = 3;
    }

    public class AccountID {
        public static final int ID_1 = 0;
        public static final int ID_2 = 1;
        public static final int ID_3 = 2;
    }

    public class Fiat {
        public static final String USD = "USD";
        public static final String KRW = "KRW";
        public static final String JPY = "JPY";
    }

    public class Locale {
        public static final String KR = "ko";
        public static final String JP = "ja";
        public static final String US = "en";
        public static final String SYSTEM = "system";
    }

    public interface Callback {
        public void callBackMethod();
    }


    public static boolean isNetworkAvailable(Context context) {
        if(context == null)  return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    }  else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                        return true;
                    }
                }
            }
            else {
                try {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        Log.i(LOG_TAG, "Network is available : true");
                        return true;
                    }
                } catch (Exception e) {
                    Log.i(LOG_TAG, "" + e.getMessage());
                }
            }
        }
        Log.i(LOG_TAG,"Network is available : FALSE ");
        return false;
    }

//    public static void setAlarmNotification(Context context, long timeStamp) {
//        PackageManager pm = context.getPackageManager();
//        ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);
//        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//
//        if (pendingIntent != null && alarmManager != null) {
//
//            alarmManager.set(AlarmManager.RTC_WAKEUP, timeStamp, pendingIntent);
//            //Toast.makeText(this,"Notifications were disabled",Toast.LENGTH_SHORT).show();
//
//            pm.setComponentEnabledSetting(receiver,
//                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
//                    PackageManager.DONT_KILL_APP);
//
//            SharedPreferenceManager.setAlarmTime(context, timeStamp);
//
//            Log.i(LOG_TAG,"setAlarmNotification: " + timeStamp);
//        }
//    }

//    public static void cancelAlarmNotification(Context context) {
//        PackageManager pm = context.getPackageManager();
//        ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);
//        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//
//        if (pendingIntent != null && alarmManager != null) {
//            alarmManager.cancel(pendingIntent);
//            //Toast.makeText(this,"Notifications were disabled",Toast.LENGTH_SHORT).show();
//        }
//        pm.setComponentEnabledSetting(receiver,
//                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
//                PackageManager.DONT_KILL_APP);
//
//        SharedPreferenceManager.setAlarmTime(context, 0);
//
//        Log.i(LOG_TAG,"cancelAlarmNotification");
//    }

    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            int parsedInt = Integer.parseInt(str, 16);
            if (parsedInt == 0) { // end of string
                break;
            }
            output.append((char) parsedInt);
        }

        return output.toString();
    }

    public static JSONObject convertWeb3Log2Json(EthLog ethLog) throws Exception {
        JSONObject retJson = new JSONObject();
        JSONArray tempJsonArray = new JSONArray();

        Log.d(LOG_TAG, "convertWeb3Log2Json");
            List<EthLog.LogResult> logResults = ethLog.getLogs();

        for (int i=0; i < logResults.size(); i++) {
            EthLog.LogObject log = (EthLog.LogObject)logResults.get(i);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("transactionHash", log.getTransactionHash());
            JSONArray topics = new JSONArray(log.getTopics());
            jsonObject.put("topics", topics);
            jsonObject.put("blockNumber", log.getBlockNumber());

            tempJsonArray.put(jsonObject);
        }
        retJson.put("logs", tempJsonArray);

        return retJson;
    }

    public static int getActionType(String topic) {
        if (topic.equalsIgnoreCase(API.CONTRACT_STAKE_TOPIC_STAKE)) {
            return EventsListActivity.ACTION_STAKE;
        } else if (topic.equalsIgnoreCase(API.CONTRACT_STAKE_TOPIC_UNSTAKE)) {
            return EventsListActivity.ACTION_UNSTAKE;
        } else if (topic.equalsIgnoreCase(API.CONTRACT_STAKE_TOPIC_WITHDREW)) {
            return EventsListActivity.ACTION_WITHDRAW;
        }

        return EventsListActivity.ACTION_UNKNOWN;
    }

}