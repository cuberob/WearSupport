package nl.vu.wearsupport.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.wearable.DataMap;

import nl.vu.wearsupport.R;
import nl.vu.wearsupport.services.CustomWatchFaceService;

/**
 * Created by robdeknegt on 15/05/15.
 */
public class SettingsManager {

    public static int getDailyStepGoal(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(context.getString(R.string.daily_step_goal), 5000);
    }


    public static int getSystemFontSizeResourceId(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sizeString = prefs.getString(context.getString(R.string.system_font_size), "2");
        int size = Integer.parseInt(sizeString);
        switch(size){
            case 0:
                //small
                return R.dimen.text_size_small;
            case 1:
                //medium
                return R.dimen.text_size_medium;
            case 2:
                //large
                return R.dimen.text_size_large;
            case 3:
                //extra large
                return R.dimen.text_size_extra_large;
        }
        return R.dimen.text_size_large;
    }

    public static boolean showStepCount(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.show_step_count), true);
    }

    public static void setDailyStepGoal(Context context, int stepGoal){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(context.getString(R.string.daily_step_goal), stepGoal);
        edit.commit();
    }

    public static void setSystemFontSize(Context context, String fontSize){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(context.getString(R.string.system_font_size), fontSize);
        edit.commit();
    }

    public static void setShowStepCount(Context context, boolean show){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(context.getString(R.string.show_step_count), show);
        edit.commit();
    }

    public static void updateSettings(Context context, DataMap dataMap){
        setDailyStepGoal(context, dataMap.getInt(context.getString(R.string.daily_step_goal)));
        setSystemFontSize(context, dataMap.getString(context.getString(R.string.system_font_size)));
        setShowStepCount(context, dataMap.getBoolean(context.getString(R.string.show_step_count)));
        setAnalogTimeMode(context, dataMap.getBoolean(context.getString(R.string.analog_time_mode)));
        setInverseMode(context, dataMap.getBoolean(context.getString(R.string.inverse_display_mode)));
        notifyWatchFace(context);
    }

    private static void notifyWatchFace(Context context){
        context.sendBroadcast(new Intent(CustomWatchFaceService.ACTION_SETTINGS_CHANGED));
    }

    public static boolean showTimeAsAnalog(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.analog_time_mode), false);
    }

    public static void setAnalogTimeMode(Context context, boolean analog){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(context.getString(R.string.analog_time_mode), analog);
        edit.commit();
    }

    public static boolean isInverseMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.inverse_display_mode), false);
    }

    public static void setInverseMode(Context context, boolean inverse){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(context.getString(R.string.inverse_display_mode), inverse);
        edit.commit();
    }
}
