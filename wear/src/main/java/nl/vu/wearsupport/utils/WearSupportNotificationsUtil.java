package nl.vu.wearsupport.utils;

import android.content.Context;
import android.content.Intent;

import nl.vu.wearsupport.services.helpers.NotificationHelper;

/**
 * Created by robdeknegt on 26/05/15.
 */
public class WearSupportNotificationsUtil {
    public static void sendDismissBroadcast(Context context, String packageName){
        final Intent intent = new Intent(NotificationHelper.ACTION_PLUGIN_DISMISS_NOTIFICATION);
        intent.putExtra("package_name", packageName);
        context.sendBroadcast(intent);
    }
}
