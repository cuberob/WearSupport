package nl.vu.wearsupport.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import nl.vu.common.object.AppInfo;
import nl.vu.wearsupport.objects.Notification;

/**
 * Created by robdeknegt on 28/05/15.
 */
public class ExtensionUtils {

    public static final String TAG = ExtensionUtils.class.getSimpleName();
    public static final String LAUNCH_PLUGIN_ACTION_FILTER = "nl.vu.wearsupport.intent.action.LAUNCH_PLUGIN";

    public static void startPlugin(Context context, ComponentName componentName){
        WearSupportNotificationsUtil.sendDismissBroadcast(context, componentName.getPackageName());
        Intent i = new Intent();
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        i.setComponent(componentName);
        context.startActivity(i);
    }

    public static AppInfo[] getExtensions(Context context, List<Notification> packageNamesWithNotification){
        ArrayList<AppInfo> mAppInfoList = new ArrayList<AppInfo>();

        final Intent mainIntent = new Intent(LAUNCH_PLUGIN_ACTION_FILTER, null);
        PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0);

        for(ResolveInfo resolveInfo : pkgAppsList){
            final String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
            final AppInfo appInfo = new AppInfo(resolveInfo.loadLabel(packageManager).toString(),
                    new ComponentName(packageName, resolveInfo.activityInfo.name),
                    packageNamesWithNotification.contains(new Notification(packageName)));
            mAppInfoList.add(appInfo);
            Log.i(TAG, appInfo.toString());
        }

        AppInfo[] result = new AppInfo[mAppInfoList.size()];
        mAppInfoList.toArray(result);
        return result;
    }
}
