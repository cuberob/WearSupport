/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.vu.wearsupport.services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import nl.vu.wearsupport.R;
import nl.vu.wearsupport.activities.ExtensionLauncherActivity;
import nl.vu.wearsupport.objects.DrawTools;
import nl.vu.wearsupport.objects.Notification;
import nl.vu.wearsupport.services.helpers.BatteryStatusHelper;
import nl.vu.wearsupport.services.helpers.NotificationHelper;
import nl.vu.wearsupport.utils.SettingsManager;
import nl.vu.wearsupport.utils.DisplayUtils;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class CustomWatchFaceService extends CanvasWatchFaceService {

    /**
     * LOG TAG
     */
    private static final String TAG = "CustomWatchFaceService";

    /**
     * Update rate in milliseconds for interactive mode. We update once every 60 seconds because only the minutes are displayed
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(60);

    /**
     * Intent Action, triggered when WearSupport settings have changed
     */
    public static final String ACTION_SETTINGS_CHANGED = "nl.vu.wearsupport.action.SETTINGS_CHANGED";

    /**
     * Reference to the ActivityMonitorService to retrieve step/activity data and trigger a save when battery runs low
     */
    private ActivityMonitorService mActivityMonitorService;

    /**
     * Reference to the ButtonOverlayService Binder to execute it's exposed methods, used to show/hide overlay and provide a listener
     */
    private ButtonOverlayService.ButtonOverlayBinder mButtonOverlayServiceBinder;

    /**
     * Daily step goal, used to calculate the completed percentage
     */
    private int mDailyStepGoal;

    /**
     * Indicator on whether or not to show the step count on the watch face, retrieved from the {@link SettingsManager}
     */
    boolean mShowStepCount;

    /**
     * Indicator on whether to draw the time in a digital (default) or analog format, retrieved from the {@link SettingsManager}
     */
    boolean mAnalogTime;

    NotificationHelper mNotificationHelper;
    BatteryStatusHelper mBatteryStatusHelper;

    /**
     * ServiceConnection,  used to setup and destroy connection to {@link ActivityMonitorService}
     */
    private ServiceConnection mActivityServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            ActivityMonitorService.MyBinder b = (ActivityMonitorService.MyBinder) binder;
            mActivityMonitorService = b.getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mActivityMonitorService = null;
        }
    };

    /**
     * ServiceConnection object used to setup and destroy connection to {@link ButtonOverlayService}
     */
    private ServiceConnection mButtonOverlayServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            mButtonOverlayServiceBinder = (ButtonOverlayService.ButtonOverlayBinder) binder;
            mButtonOverlayServiceBinder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ExtensionLauncherActivity.startExtensionLauncherActivity(CustomWatchFaceService.this, mNotificationHelper.getNotifications());
                }
            });
            mButtonOverlayServiceBinder.enableButton(true);
        }

        public void onServiceDisconnected(ComponentName className) {
            mButtonOverlayServiceBinder = null;
        }
    };

    /**
     * Initialize the data required to properly draw the watchface and start monitoring the user's activity
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mDailyStepGoal = SettingsManager.getDailyStepGoal(this);
        mShowStepCount = SettingsManager.showStepCount(this);
        mAnalogTime = SettingsManager.showTimeAsAnalog(this);
        setupActivityMonitorServiceConnection();
        setupButtonOverlayServiceConnection();
    }

    /**
     * Set up the connection to the {@link ActivityMonitorService}
     */
    private void setupActivityMonitorServiceConnection() {
        Intent activityServiceIntent= new Intent(this, ActivityMonitorService.class);
        bindService(activityServiceIntent, mActivityServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Set up the connection to the {@link ButtonOverlayService}
     */
    private void setupButtonOverlayServiceConnection() {
        Intent buttonOverlayServiceIntent= new Intent(this, ButtonOverlayService.class);
        bindService(buttonOverlayServiceIntent, mButtonOverlayServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Not guaranteed to be called (e.g. when battery removed), but attempt to unbind from connected services and save steps
     */
    @Override
    public void onDestroy() {
        mActivityMonitorService.saveIntermediateSteps();
        unbindService(mActivityServiceConnection);
        unbindService(mButtonOverlayServiceConnection);
        super.onDestroy();
    }

    /**
     * Create the engine used to draw the watch face
     * @return the Custom Engine
     */
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine {

        /**
         * Static float used to draw the hour "ticks" when drawing the analog watchface
         */
        static final float TWO_PI = (float) Math.PI * 2f;

        /**
         * INT Indicating what the {@link nl.vu.wearsupport.services.CustomWatchFaceService.Engine#mUpdateTimeHandler} has to do
         */
        static final int MSG_UPDATE_TIME = 0;

        /**
         * Handler to update the time periodically in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /**
         * Boolean indicating whether {@link nl.vu.wearsupport.services.CustomWatchFaceService.Engine#mTimeZoneBroadcastReceiver} is registered
         */
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Receiver that monitors possible changes in the timezone
         */
        final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };


        /**
         * Boolean indicating whether the action receiver has been registered
         */
        boolean mRegisteredSettingsReceiver = false;

        /**
         * Receiver that monitors whether
         * - Settings regarding the watch face have changed
         * When any of these action are received it triggers the corresponding handle method
         */
        final BroadcastReceiver mSettingsChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Received Broadcast with Action: " + intent.getAction());
                if (ACTION_SETTINGS_CHANGED.equals(intent.getAction())){
                    refreshFromSettings();
                }
            }
        };

        /**
         * Refresh how to draw the WatchFace based on data from SettingsManager, triggered when the action {@link #ACTION_SETTINGS_CHANGED} is received
         */
        private void refreshFromSettings() {
            mShowStepCount = SettingsManager.showStepCount(CustomWatchFaceService.this);
            mDailyStepGoal = SettingsManager.getDailyStepGoal(CustomWatchFaceService.this);
            mAnalogTime = SettingsManager.showTimeAsAnalog(CustomWatchFaceService.this);
            mDrawTools.setupPaintColors(SettingsManager.isInverseMode(CustomWatchFaceService.this));
            invalidate();
        }

        /**
         * Draw tools object contains all objects that are used to draw the watchface
         */
        DrawTools mDrawTools;

        /**
         * The area in which we draw the activity arc
         */
        RectF mActivityRect;

        Time mTime;

        int centerX;
        int centerY;

        float mTimeTopYOffset;
        float mBatteryBottomYOffset;
        float mNotificationExtraYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mDrawTools = new DrawTools(CustomWatchFaceService.this, SettingsManager.isInverseMode(CustomWatchFaceService.this));

            setWatchFaceStyle(new WatchFaceStyle.Builder(CustomWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setShowSystemUiTime(false)
                    .setShowUnreadCountIndicator(false)
                    .build());

            Resources resources = getResources();
            mTimeTopYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mBatteryBottomYOffset = resources.getDimension(R.dimen.battery_bottom_y_offset);
            mNotificationExtraYOffset = resources.getDimension(R.dimen.notification_y_offset_if_stepcount);

            mTime = new Time();

            mNotificationHelper = new NotificationHelper(CustomWatchFaceService.this, new NotificationHelper.NotificationListener() {
                @Override
                public void onNotificationReceived(Notification noti) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
                    mDrawTools.updateNotificationIcon(mNotificationHelper.getNotifications());
                    invalidate();
                }

                @Override
                public void onNotificationDismissed(Notification noti) {
                    mDrawTools.updateNotificationIcon(mNotificationHelper.getNotifications());
                    invalidate();
                }
            });

            mBatteryStatusHelper = new BatteryStatusHelper(CustomWatchFaceService.this, new BatteryStatusHelper.BatteryStatusListener() {
                @Override
                public void onBatteryChanged(boolean low) {
                    mDrawTools.updateLowBattery(low);
                    if(low) {
                        mActivityMonitorService.saveIntermediateSteps(); //Save steps in case the battery will run out before midnight
                    }
                    invalidate();
                }
            });

            registerSettingsChangedReceiver();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            unregisterReceivers();
            super.onDestroy();
        }

        /**
         * Unregisters all possibly registered BroadcastReceivers
         */
        public void unregisterReceivers(){
            unregisterTimeZoneReceiver();
            unregisterSettingsChangedReceiver();
            mNotificationHelper.unregisterNotificationReceiver(CustomWatchFaceService.this);
            mNotificationHelper = null;
            mBatteryStatusHelper.unregisterBatteryStatusReceiver(CustomWatchFaceService.this);
            mBatteryStatusHelper = null;
        }

        /**
         * Called to inform you of the watch face becoming visible or hidden.
         * @param visible
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerTimeZoneReceiver();
                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterTimeZoneReceiver();
            }

            handleOverrideMenuService();

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Registers the {@link #mTimeZoneBroadcastReceiver} receiver if it's currently not registered
         */
        private void registerTimeZoneReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;

            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            CustomWatchFaceService.this.registerReceiver(mTimeZoneBroadcastReceiver, filter);
        }

        /**
         * Registers the {@link #mSettingsChangedReceiver} receiver if it's currently not registered
         */
        private void registerSettingsChangedReceiver(){
            if(mRegisteredSettingsReceiver){
                return;
            }
            mRegisteredSettingsReceiver = true;

            IntentFilter filter = new IntentFilter(ACTION_SETTINGS_CHANGED);
            CustomWatchFaceService.this.registerReceiver(mSettingsChangedReceiver, filter);
        }

        /**
         * Unregisters the {@link #mTimeZoneBroadcastReceiver} receiver if it's currently registered
         */
        private void unregisterTimeZoneReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            CustomWatchFaceService.this.unregisterReceiver(mTimeZoneBroadcastReceiver);
            Log.d(TAG, "- Unregistered TimeZoneReceiver -");
        }

        /**
         * Unregisters the {@link #mSettingsChangedReceiver} receiver if it's currently registered
         */
        private void unregisterSettingsChangedReceiver(){
            if(!mRegisteredSettingsReceiver){
                return;
            }
            mRegisteredSettingsReceiver = false;
            CustomWatchFaceService.this.unregisterReceiver(mSettingsChangedReceiver);
            Log.d(TAG, "- Unregistered ActionsBroadcastReceiver -");
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            Resources resources = CustomWatchFaceService.this.getResources();

            int bottomChinHeight = insets.getSystemWindowInsetBottom(); //30 for moto 360
            int topChinHeight = insets.getSystemWindowInsetTop();
            int leftChinHeight = insets.getSystemWindowInsetLeft();
            int rightChinHeight = insets.getSystemWindowInsetRight();

            final int widthPixels = resources.getDisplayMetrics().widthPixels + leftChinHeight + rightChinHeight;
            final int heightPixels = resources.getDisplayMetrics().heightPixels + bottomChinHeight + topChinHeight;
            centerX = widthPixels / 2;
            centerY = heightPixels / 2;

            float margin = DisplayUtils.DipToPx(getApplicationContext(), DrawTools.ACTIVITY_PAINT_WIDTH_DIP) / 2;
            mActivityRect = new RectF(margin, margin, widthPixels - margin,  heightPixels - margin);
        }


        /**
         * Called when the properties of the device are determined.
         * @param properties
         */
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        /**
         * Called periodically to update the time shown by the watch face.
         */
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        /**
         * Called when the device enters or exits ambient mode.
         * @param inAmbientMode
         */
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mDrawTools.setLowBitmode(mLowBitAmbient, inAmbientMode);
            invalidate();

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Method called after invalidating the Engine
         * @param canvas on which the WatchFace should be drawn
         * @param bounds bounds of the canvas
         */
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background to start with a clean slate
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mDrawTools.mBackgroundPaint);

            drawActivity(canvas);
            drawTime(canvas);
            drawNotifications(canvas);
            drawBattery(canvas);
        }

        /**
         * Method that draws the time in digital format
         * @param canvas on which the activity data should be drawn
         */
        private void drawTime(Canvas canvas) {
            mTime.setToNow();
            if(mAnalogTime){
                drawAnalogTime(canvas);
            }else {
                // Draw H:MM
                String text = String.format("%d:%02d", mTime.hour, mTime.minute);
                canvas.drawText(text, centerX, mTimeTopYOffset, mDrawTools.mTimeTextPaint);
            }
        }

        /**
         * Method that draws the time and corresponding ticks in analog format
         * @param canvas on which the activity data should be drawn
         */
        private void drawAnalogTime(Canvas canvas) {
            // Draw the ticks.
            if(!isInAmbientMode()) {
                float innerTickRadius = centerX - 10;
                float outerTickRadius = centerX;
                for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                    float tickRot = tickIndex * TWO_PI / 12;
                    float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                    float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                    float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                    canvas.drawLine(centerX + innerX, centerY + innerY,
                            centerX + outerX, centerY + outerY, mDrawTools.mTickPaint);
                }
            }

            float minRot = mTime.minute / 60f * TWO_PI;
            float hours = mTime.hour + mTime.minute / 60f;
            float hrRot = hours / 12f * TWO_PI;

            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mDrawTools.mMinutePaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mDrawTools.mHourPaint);
        }

        /**
         * Draw battery low icon if necessary
         * @param canvas on which the activity data should be drawn
         */
        private void drawBattery(Canvas canvas){
            if(mDrawTools.mBatteryLowBitmap != null){
                int drawableHeight = mDrawTools.mBatteryLowBitmap.getScaledHeight(canvas);
                int drawableWidth = mDrawTools.mBatteryLowBitmap.getScaledWidth(canvas);
                final int left = centerX - (drawableWidth / 2);
                final float top = (centerY * 2) - drawableHeight - mBatteryBottomYOffset;
                canvas.drawBitmap(mDrawTools.mBatteryLowBitmap, left, top, null);
            }
        }

        /**
         * Draw the activity (step) data on the watchface
         * @param canvas on which the activity data should be drawn
         */
        private void drawActivity(Canvas canvas) {
            if(mActivityMonitorService != null) {
                if(mShowStepCount && !mAnalogTime) {
                    canvas.drawText(getString(R.string.Steps) + mActivityMonitorService.getTodaysSteps(), centerX, (int) (mTimeTopYOffset * 1.4), mDrawTools.mStepTextPaint);
                }
                final int sweepAngle = (int) (mActivityMonitorService.getTodaysActivityCompletion(mDailyStepGoal) * 3.6f);
                canvas.drawArc(mActivityRect, 90, sweepAngle, false, mDrawTools.mActivityPaint);
            }
        }

        /**
         * Draw received notifications on the watch face
         * @param canvas on which the notifications should be drawn
         */
        private void drawNotifications(Canvas canvas){
            if(mDrawTools.mNotificationBitmap != null) {
                if(mDrawTools.mBatteryLowBitmap != null){
                    return; //No room to draw notification icon, give priority to battery low indicator
                }
                int height = mDrawTools.mNotificationBitmap.getScaledHeight(canvas);
                int width = mDrawTools.mNotificationBitmap.getScaledWidth(canvas);
                float topPadding = mShowStepCount ? mNotificationExtraYOffset : 0;
                canvas.drawBitmap(mDrawTools.mNotificationBitmap, centerX - (width / 2), centerY - (height / 2) + topPadding, null);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Helper method to activate/deactive the button overlay service based on the visibility of the WatchFace
         */
        public void handleOverrideMenuService(){
            if(mButtonOverlayServiceBinder != null) {
                mButtonOverlayServiceBinder.enableButton(isVisible());
            }
        }
    }
}