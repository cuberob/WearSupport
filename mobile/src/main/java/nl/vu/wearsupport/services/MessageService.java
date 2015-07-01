package nl.vu.wearsupport.services;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MessageService extends WearableListenerService {

    public static final String TAG = "MessageService";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
    }


    public static void broadcastMessage(final String path, final byte[] bytes, final Context context){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                mGoogleApiClient.blockingConnect(1000, TimeUnit.MILLISECONDS);
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
                mGoogleApiClient.blockingConnect(1000, TimeUnit.MILLISECONDS);
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

    public static void putDataRequest(final PutDataRequest request, final Context context){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                mGoogleApiClient.blockingConnect(1000, TimeUnit.MILLISECONDS);
                if (!mGoogleApiClient.isConnected()) {
                    Log.e(TAG, "Failed to connect to google api client");
                    return null;
                }
                boolean success = Wearable.DataApi.putDataItem(mGoogleApiClient, request).await(1000, TimeUnit.MILLISECONDS).getStatus().isSuccess();
                if(success) {
                    Log.i(TAG, "Data item has been put");
                }
                return null;
            }
        }.execute();
    }
}
