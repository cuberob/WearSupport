package nl.vu.wearsupport.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import nl.vu.common.keys.StorageKeys;
import nl.vu.common.object.DayActivity;
import nl.vu.wearsupport.R;
import nl.vu.wearsupport.adapters.DayActivityAdapter;

public class DailyActivityOverviewActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks {

    public static final String TAG = "DailyActivityOverViewActivity";
    private ListView mListView;
    private GoogleApiClient mGoogleApiClient;
    DayActivityAdapter mDayActivityAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_activity_overview);
        mListView = (ListView) findViewById(R.id.list);
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        getDailyActivityHistory();
    }

    private void getDailyActivityHistory() {
        if(!mGoogleApiClient.isConnected()){
            Toast.makeText(this, "Could not load data", Toast.LENGTH_SHORT).show();
            mGoogleApiClient.connect();
            return;
        }
        Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (DataItem item : dataItems) {
                    if (StorageKeys.ACTIVITY_DATA_PATH.equals(item.getUri().getPath())) {
                        DataMapItem map = DataMapItem.fromDataItem(item);
                        String activityHistoryJson = map.getDataMap().getString(StorageKeys.ACTIVITY_DATA_BUNDLE_KEY);
                        List<DayActivity> mActivityHistory = new Gson().fromJson(activityHistoryJson, new TypeToken<List<DayActivity>>() {
                        }.getType());
                        onActivityDataRetrieved(mActivityHistory);
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void onActivityDataRetrieved(List<DayActivity> mActivityHistory){
        if(mActivityHistory == null || mActivityHistory.isEmpty()){
            Toast.makeText(this, "No Activity History Available yet", Toast.LENGTH_SHORT).show();
            return;
        }

        if(newHistoryIsMoreRecent(mActivityHistory)){
            mDayActivityAdapter = new DayActivityAdapter(DailyActivityOverviewActivity.this, mActivityHistory);
        }

        mListView.setAdapter(mDayActivityAdapter);
        Toast.makeText(DailyActivityOverviewActivity.this, getString(R.string.activity_data_retrieved), Toast.LENGTH_SHORT).show();;
    }

    /**
     * Check if the provided activity history is more recent than the one currently in the adapter, if adapter is not set returns true
     * @param mActivityHistory
     * @return
     */
    private boolean newHistoryIsMoreRecent(List<DayActivity> mActivityHistory) {
        if(mDayActivityAdapter == null){
            return true;
        }
        return 1 == mDayActivityAdapter.getActivityHistory().get(0).getDate().compareTo(mActivityHistory.get(0).getDate());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_daily_activity_overview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_refresh){
            getDailyActivityHistory();
        }
        return super.onOptionsItemSelected(item);
    }
}
