package nl.vu.wearsupport.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

import nl.vu.wearsupport.BuildConfig;
import nl.vu.wearsupport.utils.ActivityDataStorage;

/**
 * Created by robdeknegt on 15/05/15.
 */
public class ActivityMonitorService extends Service implements SensorEventListener{

    private static final String TAG = "ActivityMonitorService";

    /**
     * Intent action used to indicate todays step data should be saved and reset
     */
    private static final String ACION_SAVE_AND_RESET_STEPS = "nl.vu.wearsupport.action.SAVE_AND_RESET";

    /**
     * Indicates todays start steps have not been set and thus should be set on first reading
     */
    public static final int START_STEPS_NOT_INITIALIZED = -1;

    /**
     * Binder used by an activity or service to get a reference to this service
     */
    private final IBinder mBinder = new MyBinder();

    /**
     * SensorManager used to register and unregister the pedometer sensor
     */
    private SensorManager mSensorManager;

    /**
     * Since the pedometer returns an ever increasing number of steps this int is used to keep the number of steps at the start of the day.
     */
    private int mStartSteps = START_STEPS_NOT_INITIALIZED;

    /**
     * Number of steps taken today
     */
    private int mTodaysSteps = 0;

    @Override
    public IBinder onBind(Intent intent) {
        //Useful to have some basic steps to test the interface when debugging
        //mTodaysSteps = 1000;
        return mBinder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float stepCount = event.values[0];
            handleNewStepCount(stepCount);
        }
    }

    private void handleNewStepCount(float stepCount) {
        Log.d(TAG, "Total Steps: " + stepCount);
        if(mStartSteps == START_STEPS_NOT_INITIALIZED){
            mStartSteps = (int) stepCount - ActivityDataStorage.getTodaysSteps(this);
        }
        mTodaysSteps = ((int) stepCount) - mStartSteps;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Unused for step sensor
    }


    /**
     * MyBinder class that exposes the methods available through the binder
     */
    public class MyBinder extends Binder {
        ActivityMonitorService getService(){
            return ActivityMonitorService.this;
        }
    }


    /**
     * Get todays step count
     * @return int value representing the number of steps taken today
     */
    public int getTodaysSteps(){
        return mTodaysSteps;
    }

    /**
     * Get todays activity completion (100% = completed)
     * @return Todays completion as a percentage
     */
    public int getTodaysActivityCompletion(int dailyStepGoal){
        if(dailyStepGoal < mTodaysSteps){
            return 100;
        }
        float completion = ((float)mTodaysSteps) / dailyStepGoal;
        return (int)(completion * 100);
    }

    /**
     * Initializes and start the step sensor
     */
    private void initStepSensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            mSensorManager.registerListener(ActivityMonitorService.this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e(TAG, "StepCount sensor (pedometer) not available!");
            Toast.makeText(getApplicationContext(), "Error: No Step Sensor!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initStepSensor();
        registerSaveReceiver();
        setupMidnightSaveAlarm();
    }

    @Override
    public void onDestroy() {
        saveIntermediateSteps();
        mSensorManager.unregisterListener(ActivityMonitorService.this);
        ActivityMonitorService.this.unregisterReceiver(saveActivityDataBroadcastReceiver);
        Log.d(TAG, "!!!Activity Monitor Service Destroyed!!!");
        super.onDestroy();
    }

    public void saveIntermediateSteps(){
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(System.currentTimeMillis());
        ActivityDataStorage.saveData(getApplicationContext(), date, getTodaysSteps());
    }


    final BroadcastReceiver saveActivityDataBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Request to save and reset steps received");
            saveAndResetSteps();
        }
    };

    /**
     * Registers the broadcast receiver to listen for the save and reset intent
     */
    private void registerSaveReceiver(){
        ActivityMonitorService.this.registerReceiver(saveActivityDataBroadcastReceiver, new IntentFilter(ACION_SAVE_AND_RESET_STEPS));
    }

    private void saveAndResetSteps(){
        //Since this event is triggered at 00:00 deduct half a day to make sure it is saved for the correct (previous) day
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(System.currentTimeMillis() - AlarmManager.INTERVAL_HALF_DAY);
        ActivityDataStorage.saveData(getApplicationContext(), date, getTodaysSteps());

        //Reset steps for a new day
        mStartSteps = START_STEPS_NOT_INITIALIZED;
        mTodaysSteps = 0;
    }

    /**
     * Sets the alarmmanager to send the save broadcast at midnight everyday
     */
    private void setupMidnightSaveAlarm(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + AlarmManager.INTERVAL_DAY);
        calendar.set(Calendar.SECOND, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.AM_PM, Calendar.AM);

        //The fired Intent
        Intent intent = new Intent(ACION_SAVE_AND_RESET_STEPS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //Set up so it repeats daily
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent); //Make sure it's not set twice
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent); //Set it to repeat at midnight

        if(BuildConfig.DEBUG){
            Log.d(TAG, "Successfully set up alarm for: " + calendar.getTime().toString());
        }
    }
}
