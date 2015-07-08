package nl.vu.wearsupport.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import nl.vu.common.object.AppInfo;
import nl.vu.wearsupport.utils.PluginUtils;

/**
 * Created by robdeknegt on 15/05/15.
 */
public class ExtensionLauncherActivity extends Activity {

    public static final String TAG = ExtensionLauncherActivity.class.getSimpleName();
    public static final String PACKAGES_EXTRA_KEY = "packages";


    public static void startExtensionLauncherActivity(Context context, List<String> mPackageNamesWithNotifications){
        Intent i = new Intent(context, ExtensionLauncherActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(PACKAGES_EXTRA_KEY, (ArrayList<String>) mPackageNamesWithNotifications);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<String> mPackagesWithNotification = (ArrayList<String>) getIntent().getSerializableExtra(PACKAGES_EXTRA_KEY);
        startCorrectLauncherActivity(PluginUtils.getExtensions(ExtensionLauncherActivity.this, mPackagesWithNotification));
    }

    private void startCorrectLauncherActivity(AppInfo[] mAppInfoList){
        int nrOfApps = mAppInfoList.length;
        if(nrOfApps == 0){
            finish();
        }else if(nrOfApps == 1){
            MultiExtensionActivity.startMultiExtensionActivity(this, mAppInfoList);
        }else if(nrOfApps <= 2){
            DualExtensionActivity.startDualExtensionActivity(this, mAppInfoList);
        }else if(nrOfApps <= 4){
            QuadExtensionActivity.startQuadExtensionActivity(this, mAppInfoList);
        }else{
            MultiExtensionActivity.startMultiExtensionActivity(this, mAppInfoList);
        }
        finish();
    }

}
