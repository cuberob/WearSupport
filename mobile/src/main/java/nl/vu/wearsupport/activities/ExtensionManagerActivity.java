package nl.vu.wearsupport.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import nl.vu.common.object.AppInfo;
import nl.vu.wearsupport.R;
import nl.vu.wearsupport.adapters.AppInfoListAdapter;

public class ExtensionManagerActivity extends ActionBarActivity {

    public static final String EXTENSION_EDIT_SETTING_ACTION = "nl.vu.wearsupport.intent.action.EDIT_SETTING";
    public static final String TAG = "Ext.ManagerActivity";
    ArrayList<AppInfo> mAppInfoList;
    private ListView mListview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extension_manager);
        findApps();

        mListview = (ListView) findViewById(R.id.list);
        mListview.setAdapter(new AppInfoListAdapter(getApplicationContext(), mAppInfoList));
        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                i.setComponent(mAppInfoList.get(position).getComponentName());
                startActivity(i);
            }
        });
    }

    private void findApps(){
        mAppInfoList = new ArrayList<AppInfo>();

        final Intent mainIntent = new Intent(EXTENSION_EDIT_SETTING_ACTION, null);
        PackageManager packageManager = this.getPackageManager();
        final List<ResolveInfo> pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0);

        for(ResolveInfo resolveInfo : pkgAppsList){
            final AppInfo appInfo = new AppInfo(resolveInfo.loadLabel(packageManager).toString(),
                    new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name));
            mAppInfoList.add(appInfo);
            Log.d(TAG, appInfo.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_extension_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}
