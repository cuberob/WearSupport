package nl.vu.wearsupport.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.wearable.PutDataMapRequest;

import nl.vu.common.keys.SettingKeys;
import nl.vu.wearsupport.R;
import nl.vu.wearsupport.services.MessageService;

/**
 * Created by robdeknegt on 13/05/15.
 */
public class SettingsManager {


    public static final String DEFAULT_REMOTE_REQUEST_PASSWORD = "wearsupport";
    public static final String DEFAULT_DAILY_STEP_GOAL = "5000";

    public static String getRequestPassword(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.request_password_setting_key), DEFAULT_REMOTE_REQUEST_PASSWORD);
    }

    public static boolean isRemoteRequestEnabled(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.request_enabled_setting_key), false);
    }

    public static int getDailyStepGoal(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String goal = prefs.getString(context.getString(R.string.daily_step_goal), DEFAULT_DAILY_STEP_GOAL);
        return Integer.valueOf(goal);
    }

    public static String getSystemFontSize(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.system_font_size), "2");
    }

    public static boolean showStepCount(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.show_step_count), true);
    }

    public static boolean useInvertedColors(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.inverse_display_mode), false);
    }

    public static boolean useAnalogWatch(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.analog_time_mode), false);
    }

    public static String getWarningPhoneNumber(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.no_activity_warning_phone_number), "");
    }

    public static void requestSync(Context context){
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(SettingKeys.MAIN_SETTINGS_PATH);
        putDataMapReq.getDataMap().putInt(context.getString(R.string.daily_step_goal), getDailyStepGoal(context));
        putDataMapReq.getDataMap().putString(context.getString(R.string.system_font_size), getSystemFontSize(context));
        putDataMapReq.getDataMap().putBoolean(context.getString(R.string.show_step_count), showStepCount(context));
        putDataMapReq.getDataMap().putBoolean(context.getString(R.string.analog_time_mode), useAnalogWatch(context));
        putDataMapReq.getDataMap().putBoolean(context.getString(R.string.inverse_display_mode), useInvertedColors(context));

        putDataMapReq.getDataMap().putLong("FORCE_SYNC", System.currentTimeMillis());

        MessageService.putDataRequest(putDataMapReq.asPutDataRequest(), context);
    }
}
