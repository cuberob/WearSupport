package nl.vu.wearsupport.services;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import nl.vu.common.keys.MessageKeys;
import nl.vu.common.keys.SettingKeys;
import nl.vu.wearsupport.services.helpers.BatteryStatusHelper;
import nl.vu.wearsupport.utils.SettingsManager;

/**
 * Created by robdeknegt on 18/05/15.
 */
public class MessageService extends WearableListenerService{

    private static final String TAG = "MessageService";
    public static final int CONNECTION_TIMEOUT_MILLIS = 5000;

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            Log.d(TAG, "Data Changed with path: " + event.getDataItem().getUri().getPath());
            handleEvent(event);
        }
    }

    private void handleEvent(DataEvent event) {
        String path = event.getDataItem().getUri().getPath();

        if(SettingKeys.MAIN_SETTINGS_PATH.equals(path)){
            SettingsManager.updateSettings(getApplicationContext(), DataMapItem.fromDataItem(event.getDataItem()).getDataMap());
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        if(MessageKeys.FAKE_BATTERY_LOW.equals(path)){
            BatteryStatusHelper.fakeBattery(getApplicationContext(), true);
        } else if (MessageKeys.FAKE_BATTERY_OK.equals(path)){
            BatteryStatusHelper.fakeBattery(getApplicationContext(), false);
        }
        super.onMessageReceived(messageEvent);
    }

    public static void broadcastMessage(final String path, final byte[] bytes, final Context context){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                mGoogleApiClient.blockingConnect(CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                if(!mGoogleApiClient.isConnected()){
                    Log.e(TAG, "Failed to connect to google api client");
                    return null;
                }

                List<Node> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await().getNodes();
                for(Node node : nodes) {
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, bytes);
                }
                mGoogleApiClient.disconnect();
                return null;
            }
        }.execute();
    }

    public static void sendMessage(final String path, final byte[] bytes, final Context context, final String nodeId){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                mGoogleApiClient.blockingConnect(CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                if(!mGoogleApiClient.isConnected()){
                    Log.e(TAG, "Failed to connect to google api client");
                    return null;
                }
                Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, path, bytes);
                mGoogleApiClient.disconnect();
                return null;
            }
        }.execute();
    }

    public static void putDataRequest(final PutDataRequest request, final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                mGoogleApiClient.blockingConnect(CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                if (!mGoogleApiClient.isConnected()) {
                    Log.e(TAG, "Failed to connect to google api client");
                    return null;
                }
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(mGoogleApiClient, request).await(CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                if(result.getStatus().isSuccess()){
                    Log.d(TAG, "Successfully put into datalayer: " + request.getUri().getPath());
                }else {
                    Log.d(TAG, "Failed tio put into datalayer: " + request.getUri().getPath());
                }
                return null;
            }
        }.execute();
    }
}
