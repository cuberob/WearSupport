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

import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import nl.vu.wearsupport.R;
import nl.vu.wearsupport.activities.ExtensionLauncherActivity;
import nl.vu.wearsupport.objects.PaintTools;
import nl.vu.wearsupport.utils.SettingsManager;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class CustomWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "CustomWatchFaceService";

    /**
     * Update rate in milliseconds for interactive mode. We update once every 30 seconds because only the minutes are displayed
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(30);
    public static final String ACTION_SETTINGS_CHANGED = "nl.vu.wearsupport.action.SETTINGS_CHANGED";
    public static final String ACTION_PLUGIN_SHOW_NOTIFICATION = "nl.vu.wearsupport.action.PLUGIN_NOTIFICATION";
    public static final String ACTION_PLUGIN_DISMISS_NOTIFICATION = "nl.vu.wearsupport.action.PLUGIN_DISMISS_NOTIFICATION";

    /**
     * Reference to the ActivityMonitorService to retrieve step/activity data
     */
    private ActivityMonitorService mActivityMonitorService;

    /**
     * Reference to the ButtonOverlayService Binder to execute it's exposed methods
     */
    private ButtonOverlayService.ButtonOverlayBinder mButtonOverlayServiceBinder;

    /**
     * Daily step goal, used to calculate the completed percentage
     */
    private int mDailyStepGoal;

    /**
     * Whether or not to show the step count on the watch face
     */
    boolean mShowStepCount;

    /**
     * Draw the time in analog format if true, else use digital time
     */
    boolean mAnalogTime;

    /**
     * ServiceConnection object used to setup and destroy connection to ActivityMonitorService
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
     * ServiceConnection object used to setup and destroy connection to ActivityMonitorService
     */
    private ServiceConnection mButtonOverlayServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            mButtonOverlayServiceBinder = (ButtonOverlayService.ButtonOverlayBinder) binder;
            mButtonOverlayServiceBinder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ExtensionLauncherActivity.startExtensionLauncherActivity(CustomWatchFaceService.this, mNotifications);
                }
            });
            mButtonOverlayServiceBinder.enableButton(true);
        }

        public void onServiceDisconnected(ComponentName className) {
            mButtonOverlayServiceBinder = null;
        }
    };

    /**
     * List with the package names of applications that have notifications for the user
     */
    private ArrayList<String> mNotifications = new ArrayList<String>();

    @Override
    public void onCreate() {
        super.onCreate();
        Intent activityServiceIntent= new Intent(this, ActivityMonitorService.class);
        Intent buttonOverlayServiceIntent= new Intent(this, ButtonOverlayService.class);
        mDailyStepGoal = SettingsManager.getDailyStepGoal(this);
        mShowStepCount = SettingsManager.showStepCount(this);
        mAnalogTime = SettingsManager.showTimeAsAnalog(this);
        bindService(activityServiceIntent, mActivityServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(buttonOverlayServiceIntent, mButtonOverlayServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        unbindService(mActivityServiceConnection);
        unbindService(mButtonOverlayServiceConnection);
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine {
        static final float TWO_PI = (float) Math.PI * 2f;
        static final int MSG_UPDATE_TIME = 0;
        public static final int NOTIFICATION_ICON_TOP_PADDING_IF_SHOWING_STEPCOUNT = 55;
        public static final int STEP_COUNT_Y_OFFSET = 50;
        public static final int BATTERY_BOTTOM_PADDING = 40;

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

        final BroadcastReceiver mTimeZoneBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        final BroadcastReceiver mActionsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Received Broadcast with Action: " + intent.getAction());
                if(ACTION_PLUGIN_SHOW_NOTIFICATION.equals(intent.getAction())){
                    handleNewNotification(intent);
                }else if(ACTION_SETTINGS_CHANGED.equals(intent.getAction())){
                    refreshSettings(context);
                }else if(Intent.ACTION_BATTERY_LOW.equals(intent.getAction())){
                    mPaintTools.setLowBattery(true);
                }else if(Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())){
                    mPaintTools.setLowBattery(false);
                }else if(ACTION_PLUGIN_DISMISS_NOTIFICATION.equals(intent.getAction())){
                    handleDismissNotification(intent);
                }
            }
        };

        private void refreshSettings(Context context) {
            //Refresh watch face data
            mShowStepCount = SettingsManager.showStepCount(context);
            mDailyStepGoal = SettingsManager.getDailyStepGoal(context);
            mAnalogTime = SettingsManager.showTimeAsAnalog(context);
            mPaintTools.updateInverse(SettingsManager.isInverseMode(CustomWatchFaceService.this), getApplicationContext());
        }

        private void handleNewNotification(Intent intent) { //Retrieve package name of notification source
            final String package_name = intent.getStringExtra("package_name");
            if(package_name == null || package_name.isEmpty()){
                Log.e(TAG, "Make sure the intent contains package (\"context.getPackageName()\") as string extra [package_name]!");
                return;
            }
            if(!mNotifications.contains(package_name)) { //Add package name to list of notifications to list of notifications
                mNotifications.add(package_name);
                ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
            }
            mPaintTools.setNotificationIcon(mNotifications);
        }

        private void handleDismissNotification(Intent intent) {
            //Retrieve package name of notification source
            final String package_name = intent.getStringExtra("package_name");
            if(package_name == null || package_name.isEmpty()){
                Log.e(TAG, "Make sure the intent contains package (\"context.getPackageName()\") as string extra [package_name]!");
                return;
            }
            if(mNotifications.contains(package_name)) {
                mNotifications.remove(mNotifications.indexOf(package_name));
            }
            mPaintTools.setNotificationIcon(mNotifications);
        }

        boolean mRegisteredActionsReceiver = false;

        /**
         * Paint tools object contains all objects that are used to draw the watchface
         */
        PaintTools mPaintTools;

        /**
         * The area in which we draw the activity arc
         */
        RectF mActivityRect;

        boolean mAmbient;

        Time mTime;

        int centerX;
        int centerY;

        float mYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mPaintTools = new PaintTools(CustomWatchFaceService.this, SettingsManager.isInverseMode(CustomWatchFaceService.this));

            setWatchFaceStyle(new WatchFaceStyle.Builder(CustomWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setShowSystemUiTime(false)
                    .setShowUnreadCountIndicator(false)
                    .build());

            mYOffset = getResources().getDimension(R.dimen.digital_y_offset);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            unregisterReceivers();
            super.onDestroy();
        }

        public void unregisterReceivers(){
            unregisterTimeZoneReceiver();
            unregisterActionsReceiver();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerTimeZoneReceiver();
                registerActionsReceiver();

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

        private void registerTimeZoneReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;

            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            CustomWatchFaceService.this.registerReceiver(mTimeZoneBroadcastReceiver, filter);
        }

        private void registerActionsReceiver(){
            if(mRegisteredActionsReceiver){
                return;
            }
            mRegisteredActionsReceiver = true;

            IntentFilter filter = new IntentFilter(ACTION_SETTINGS_CHANGED);
            filter.addAction(ACTION_PLUGIN_SHOW_NOTIFICATION);
            filter.addAction(ACTION_PLUGIN_DISMISS_NOTIFICATION);
            filter.addAction(Intent.ACTION_BATTERY_LOW);
            filter.addAction(Intent.ACTION_BATTERY_OKAY);
            CustomWatchFaceService.this.registerReceiver(mActionsBroadcastReceiver, filter);
        }

        private void unregisterTimeZoneReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            CustomWatchFaceService.this.unregisterReceiver(mTimeZoneBroadcastReceiver);
            Log.d(TAG, "- Unregistered TimeZoneReceiver -");
        }

        private void unregisterActionsReceiver(){
            if(!mRegisteredActionsReceiver){
                return;
            }
            mRegisteredActionsReceiver = false;
            CustomWatchFaceService.this.unregisterReceiver(mActionsBroadcastReceiver);
            Log.d(TAG, "- Unregistered ActionsBroadcastReceiver -");
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = CustomWatchFaceService.this.getResources();

            int chinHeight = insets.getSystemWindowInsetBottom(); //30 for moto 360

            final int widthPixels = resources.getDisplayMetrics().widthPixels;
            final int heightPixels = resources.getDisplayMetrics().heightPixels + chinHeight;
            centerX = widthPixels / 2;
            centerY = heightPixels / 2;

            int margin = PaintTools.ACTIVITY_PAINT_WIDTH / 2;
            mActivityRect = new RectF(0 + margin, 0 + margin, widthPixels - margin,  heightPixels - margin);
        }



        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                mPaintTools.setLowBitmode(mLowBitAmbient, mAmbient);
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mPaintTools.mBackgroundPaint);

            drawActivity(canvas);
            drawTime(canvas);
            drawNotifications(canvas);
            drawBattery(canvas);
        }

        private void drawTime(Canvas canvas) {
            mTime.setToNow();
            if(mAnalogTime){
                drawAnalogTime(canvas);
            }else {
                // Draw H:MM
                String text = String.format("%d:%02d", mTime.hour, mTime.minute);
                canvas.drawText(text, centerX, mYOffset, mPaintTools.mTimeTextPaint);
            }
        }

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
                            centerX + outerX, centerY + outerY, mPaintTools.mTickPaint);
                }
            }

            float minRot = mTime.minute / 60f * TWO_PI;
            float hours = mTime.hour + mTime.minute / 60f;
            float hrRot = hours / 12f * TWO_PI;

            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mPaintTools.mMinutePaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mPaintTools.mHourPaint);
        }

        /**
         * Draw battery low icon if necessary
         * @param canvas
         */
        private void drawBattery(Canvas canvas){
            if(mPaintTools.mBatteryLowBitmap != null){
                int drawableHeight = mPaintTools.mBatteryLowBitmap.getScaledHeight(canvas);
                int drawableWidth = mPaintTools.mBatteryLowBitmap.getScaledWidth(canvas);
                final int left = centerX - (drawableWidth / 2);
                final int top = (centerY * 2) - drawableHeight - BATTERY_BOTTOM_PADDING;
                canvas.drawBitmap(mPaintTools.mBatteryLowBitmap, left, top, null);
            }
        }

        /**
         * Draw the activity (step) data on the watchface
         * @param canvas on which the activity data should be drawn
         */
        private void drawActivity(Canvas canvas) {
            if(mActivityMonitorService != null) {
                if(mShowStepCount && !mAnalogTime) {
                    canvas.drawText(getString(R.string.Steps) + mActivityMonitorService.getTodaysSteps(), centerX, mYOffset + STEP_COUNT_Y_OFFSET, mPaintTools.mStepTextPaint);
                }
                final int sweepAngle = (int) (mActivityMonitorService.getTodaysActivityCompletion(mDailyStepGoal) * 3.6f);
                canvas.drawArc(mActivityRect, 90, sweepAngle, false, mPaintTools.mActivityPaint);
            }
        }

        /**
         * Draw received notifications on the watch face
         * @param canvas on which the notifications should be drawn
         */
        private void drawNotifications(Canvas canvas){
            if(mPaintTools.mNotificationBitmap != null) {
                if(mPaintTools.mBatteryLowBitmap != null){
                    return; //No room to draw notification icon, give priority to battery low indicator
                }
                int height = mPaintTools.mNotificationBitmap.getScaledHeight(canvas);
                int width = mPaintTools.mNotificationBitmap.getScaledWidth(canvas);
                int topPadding = mShowStepCount ? NOTIFICATION_ICON_TOP_PADDING_IF_SHOWING_STEPCOUNT : 0;
                canvas.drawBitmap(mPaintTools.mNotificationBitmap, centerX - (width / 2), centerY - (height / 2) + topPadding, null);
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

        public void handleOverrideMenuService(){
            if(mButtonOverlayServiceBinder != null) {
                mButtonOverlayServiceBinder.enableButton(isVisible());
            }
        }
    }
}
