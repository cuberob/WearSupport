package nl.vu.wearsupport.services.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import nl.vu.wearsupport.objects.Notification;

/**
 * Created by robdeknegt on 08/07/15.
 */
public class NotificationHelper {

    private static final String TAG = NotificationHelper.class.getSimpleName();

    /**
     * The extra {@link #ACTION_PLUGIN_SHOW_NOTIFICATION} or {@link #ACTION_PLUGIN_DISMISS_NOTIFICATION} receive to indicate which extension wants to show/dismiss it's notification
     */
    public static final String EXTRA_PACKAGE_NAME = "package_name";

    /**
     * Intent Action, triggered when an extension wants to notify the user of new data
     */
    public static final String ACTION_PLUGIN_SHOW_NOTIFICATION = "nl.vu.wearsupport.action.PLUGIN_NOTIFICATION";

    /**
     * Intent Action, triggered when the notification of an extension should be removed
     */
    public static final String ACTION_PLUGIN_DISMISS_NOTIFICATION = "nl.vu.wearsupport.action.PLUGIN_DISMISS_NOTIFICATION";

    public interface NotificationListener {
        void onNotificationReceived(Notification notification);
        void onNotificationDismissed(Notification notification);
    }


    private NotificationListener mListener;

    /**
     * List of Strings containing the package names, as received in the {@link nl.vu.wearsupport.services.CustomWatchFaceService.Engine#mSettingsChangedReceiver}
     * which currently have a pending notification.
     */
    private List<Notification> mNotifications;

    public NotificationHelper(Context context, NotificationListener listener) {
        mNotifications = new ArrayList<Notification>();
        this.mListener = listener;
        IntentFilter filter = new IntentFilter(ACTION_PLUGIN_SHOW_NOTIFICATION);
        filter.addAction(ACTION_PLUGIN_DISMISS_NOTIFICATION);
        context.registerReceiver(BROADCAST_RECEIVER, filter);
    }

    public void unregisterNotificationReceiver(Context context) {
        context.unregisterReceiver(BROADCAST_RECEIVER);
    }

    public final BroadcastReceiver BROADCAST_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_PLUGIN_SHOW_NOTIFICATION.equals(intent.getAction())) {
                handleNewNotification(getPackageNameExtra(intent));
            } else if (ACTION_PLUGIN_DISMISS_NOTIFICATION.equals(intent.getAction())) {
                handleDismissNotification(getPackageNameExtra(intent));
            }
        }
    };

    /**
     * Method that handles spawning a new notification, triggered when the action {@link #ACTION_PLUGIN_SHOW_NOTIFICATION} is received
     * @param package_name of the notification origin
     */
    private void handleNewNotification(final String package_name) {
        if(package_name == null || package_name.isEmpty()){
            Log.e(TAG, "Make sure the intent contains package (\"context.getPackageName()\") as string extra [package_name]!");
            return;
        }
        Notification incomingNotification = new Notification(package_name);
        if(!mNotifications.contains(incomingNotification)) { //Add package name to list of notifications to list of notifications
            mNotifications.add(incomingNotification);
            if(mListener != null){mListener.onNotificationReceived(incomingNotification);}
        }
    }

    private String getPackageNameExtra(Intent intent) {
        return intent.getStringExtra(EXTRA_PACKAGE_NAME);
    }

    /**
     * Method that handles dismissing a notification, triggered when the action {@link #ACTION_PLUGIN_DISMISS_NOTIFICATION} is received
     * @param package_name of the notification origin
     */
    private void handleDismissNotification(final String package_name) {
        if(package_name == null || package_name.isEmpty()){
            Log.e(TAG, "Make sure the intent contains package (\"context.getPackageName()\") as string extra [package_name]!");
            return;
        }
        Notification dismissNotification = new Notification(package_name);
        if(mNotifications.contains(dismissNotification)) {
            mNotifications.remove(mNotifications.indexOf(dismissNotification));
            if(mListener != null){mListener.onNotificationDismissed(dismissNotification);}
        }
    }

    public List<Notification> getNotifications(){
        return mNotifications;
    }
}
