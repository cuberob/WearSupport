package nl.vu.wearsupport.broadcastreceivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nl.vu.common.keys.StorageKeys;
import nl.vu.common.object.DayActivity;
import nl.vu.wearsupport.BuildConfig;
import nl.vu.wearsupport.R;
import nl.vu.wearsupport.utils.SettingsManager;

public class ActivityDataRequestReceiver extends BroadcastReceiver {

    public static final String TAG = "ActivityDataReqRec";
    public static final String REQUEST_ACTIVITY_MSG_POSTFIX = " request activity";

    public static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String ACTION_CHECK_ACTIVITY = "nl.vu.wearsupport.intent.action.CHECK_ACTIVITY";


    public ActivityDataRequestReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(ACTION_SMS_RECEIVED.equals(intent.getAction())) {
            handleRemoteRequest(context, intent);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            setupActivityCheck(context);
        } else if (ACTION_CHECK_ACTIVITY.equals(intent.getAction())){
            checkActivity(context);
        }
    }

    private void checkActivity(final Context context) {
        final String warningPhoneNumber = SettingsManager.getWarningPhoneNumber(context);
        if(warningPhoneNumber.isEmpty()){
            Log.d(TAG, "No Warning Number set up");
            return;
        }
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                List<DayActivity> mActivityHistory = getActivityHistory(context);
                if(mActivityHistory == null || mActivityHistory.isEmpty()) {
                    sendSMS(warningPhoneNumber, context.getString(R.string.warning_no_activity_ever_message));
                    return null;
                }
                Collections.reverse(mActivityHistory); //Reverse order so 0 is the most recent recorded activity

                Calendar yesterday = Calendar.getInstance();
                yesterday.setTimeInMillis(System.currentTimeMillis() - AlarmManager.INTERVAL_DAY);
                int dayOfYear = yesterday.get(Calendar.DAY_OF_YEAR);

                final DayActivity dayActivity = mActivityHistory.get(0);
                if(dayOfYear == dayActivity.getDate().get(Calendar.DAY_OF_YEAR)){
                    //Entry found for yesterday
                    Log.d(TAG, "Match found in activity history, stepcount: " + dayActivity.stepCount);
                    if (dayActivity.stepCount < 10){ //Low stepcount detected, send warning message!
                        sendSMS(warningPhoneNumber, context.getString(R.string.message_no_activity_warning));
                    }
                }else{
                    //No entry found for yesterday, send semi-warning message
                    Log.d(TAG, "Last item: " + dayActivity.getDate().toString() + " does not match yesterday: " + yesterday.toString());
                    sendSMS(warningPhoneNumber, context.getString(R.string.message_activity_yesterday_unknown));
                }

                return null;
            }
        }.execute();
    }

    public static void setupActivityCheck(Context context){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + AlarmManager.INTERVAL_DAY);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, 9);
        calendar.set(Calendar.AM_PM, Calendar.AM);

        //The fired Intent
        Intent intent = new Intent(ACTION_CHECK_ACTIVITY);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Set up so it repeats daily
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent); //Make sure it's not set twice
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent); //Set it to repeat daily at 6PM

        if(BuildConfig.DEBUG){
            Log.d(TAG, "Successfully set up alarm for: " + calendar.getTime().toString());
        }
    }

    private void handleRemoteRequest(Context context, Intent intent) {
        if(!SettingsManager.isRemoteRequestEnabled(context)){
            Log.i(TAG, "Remote Activity Requests are disabled in WearSupport Settings");
            return;
        }

        final Bundle bundle = intent.getExtras();
        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdusObj.length; i++) {
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                    String senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i(TAG, "senderNum: " + senderNum + "; message: " + message);

                    if(isMessageValidRequest(message, context)){
                        if(BuildConfig.DEBUG){
                            Log.d(TAG, "- Valid Activity Data Request received - ");
                        }
                        initActivityRequest(senderNum, context);
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception smsReceiver" + e);
        }
    }

    private boolean isMessageValidRequest(String msg, Context context){
        String toMatch = SettingsManager.getRequestPassword(context) + REQUEST_ACTIVITY_MSG_POSTFIX;
        toMatch = toMatch.toLowerCase();
        msg = msg.toLowerCase();
        return toMatch.equals(msg);
    }

    private void initActivityRequest(final String senderNum, final Context context){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                List<DayActivity> mActivityHistory = getActivityHistory(context);
                if(mActivityHistory == null || mActivityHistory.isEmpty()){
                    sendSMS(senderNum, context.getString(R.string.no_activity_data_available_yet));
                    return null;
                }
                Collections.reverse(mActivityHistory); //Reverse order so 0 is most recent

                //Build the sms return message, containing history of last 3 available days
                StringBuilder replyMessage = new StringBuilder("History:");
                for(int i = 0; i < 3 && i < mActivityHistory.size(); i++){
                    replyMessage.append("\n");
                    replyMessage.append(mActivityHistory.get(i).toString());
                }
                sendSMS(senderNum, replyMessage.toString());

                return null;
            }
        }.execute();
    }

    /**
     * Can't be run on UI Thread
     * @param context
     * @return A list containing the activity data as available from the datalayer using the Wearable.DataApi
     */
    private List<DayActivity> getActivityHistory(Context context) {
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
        mGoogleApiClient.blockingConnect(5, TimeUnit.SECONDS);
        if(mGoogleApiClient.isConnected()) {
            DataItemBuffer dataItems = Wearable.DataApi.getDataItems(mGoogleApiClient).await(5, TimeUnit.SECONDS);
            for (DataItem item : dataItems) {
                if (StorageKeys.ACTIVITY_DATA_PATH.equals(item.getUri().getPath())) {
                    DataMapItem map = DataMapItem.fromDataItem(item);
                    String activityHistoryJson = map.getDataMap().getString(StorageKeys.ACTIVITY_DATA_BUNDLE_KEY);
                    return new Gson().fromJson(activityHistoryJson, new TypeToken<List<DayActivity>>() {}.getType());
                }
            }
        }
        return null;
    }

    private void sendSMS(String phoneNumber, String message){
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }
}
