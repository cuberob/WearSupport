package nl.vu.wearsupport.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import nl.vu.common.keys.StorageKeys;
import nl.vu.common.object.DayActivity;
import nl.vu.wearsupport.services.MessageService;

/**
 * Created by robdeknegt on 18/05/15.
 */
public class ActivityDataStorage {

    public static final String TAG = "ActivityDataStorage";
    public static final int MAX_HISTORY_SIZE = 50;

    public static void saveData(Context context, Calendar date, int stepCount){
        Log.d(TAG, "Request to save - Date: " + date.get(Calendar.DAY_OF_YEAR) + " StepCount: " + stepCount);
        DayActivity toSave = new DayActivity(stepCount, date);

        List<DayActivity> activityHistory = getActivityHistory(context);
        if(activityHistory.isEmpty()){
            Log.d(TAG, "History empty, saving first item");
            activityHistory.add(toSave);
        }else if(toSave.equals(activityHistory.get(activityHistory.size() - 1))){
            //Save conflict, last saved item matches the item that is to be saved
            DayActivity conflict = activityHistory.get(activityHistory.size() - 1);
            //Resolve setting new stepCount
            Log.d(TAG, "History Conflict detected, replacing old steps (" + conflict.stepCount + ") with new steps (" + toSave.stepCount + ")");
            conflict.stepCount = toSave.stepCount;
        } else {
            Log.d(TAG, "No entry with this date found, putting into storage");
            activityHistory.add(toSave);
        }

        if (activityHistory.size() >= MAX_HISTORY_SIZE){
            activityHistory.remove(0); //Remove oldest entry
        }

        saveActivityHistory(context, activityHistory);
        putHistoryInDataLayer(context);

        Log.d(TAG, "Successfully saved activity data: " + toSave.toString());
    }

    public static void putHistoryInDataLayer(final Context context){
        MessageService.putDataRequest(getActivityDataRequest(context).asPutDataRequest(), context);
    }

    public static PutDataMapRequest getActivityDataRequest(Context context){
        PutDataMapRequest result = PutDataMapRequest.create(StorageKeys.ACTIVITY_DATA_PATH);
        String activityDataJson = PreferenceManager.getDefaultSharedPreferences(context).getString(StorageKeys.ACTIVITY_DATA_BUNDLE_KEY, "");
        result.getDataMap().putString(StorageKeys.ACTIVITY_DATA_BUNDLE_KEY, activityDataJson);
        result.getDataMap().putLong("FORCE_SYNC", System.currentTimeMillis());
        return result;
    }

    public static List<DayActivity> getActivityHistory(Context context){
        final String activitiesJson = PreferenceManager.getDefaultSharedPreferences(context).getString(StorageKeys.ACTIVITY_DATA_BUNDLE_KEY, "");
        List<DayActivity> result = new Gson().fromJson(activitiesJson, new TypeToken<List<DayActivity>>() {
        }.getType());
        if(result == null){
            result = new ArrayList<DayActivity>();
        }
        return result;
    }

    public static void saveActivityHistory(Context context, List<DayActivity> activities){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(StorageKeys.ACTIVITY_DATA_BUNDLE_KEY, new Gson().toJson(activities));
        editor.commit();
    }

    public static int getTodaysSteps(Context context){
        List<DayActivity> history = getActivityHistory(context);
        if(history == null || history.isEmpty()){
            return 0;
        }
        Collections.reverse(history); //Reverse so 0 is latest day
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(System.currentTimeMillis());
        final DayActivity latestStoredDayActivity = history.get(0);
        if(today.get(Calendar.DAY_OF_YEAR) == latestStoredDayActivity.getDate().get(Calendar.DAY_OF_YEAR)){
            return latestStoredDayActivity.stepCount;
        }
        return 0;
    }

}
