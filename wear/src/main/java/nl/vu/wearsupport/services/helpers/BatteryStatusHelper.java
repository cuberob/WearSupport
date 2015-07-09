package nl.vu.wearsupport.services.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

/**
 * Created by robdeknegt on 08/07/15.
 */
public class BatteryStatusHelper {

    private static final String TAG = BatteryStatusHelper.class.getSimpleName();

    public interface BatteryStatusListener{
        void onBatteryChanged(boolean low);
    }

    BatteryStatusListener mListener;

    public BatteryStatusHelper(Context context, BatteryStatusListener listener){
        mListener = listener;
        initBatteryCheck(context);
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        context.registerReceiver(BROADCAST_RECEIVER, filter);
    }

    public void unregisterBatteryStatusReceiver(Context context){
        context.unregisterReceiver(BROADCAST_RECEIVER);
    }

    public void initBatteryCheck(Context context){
        Intent intent  = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(intent == null){
            Log.e(TAG, "Failed to retrieve battery level intent");
            return;
        }
        int    level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int    scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int    percent = (level*100)/scale;
        if(mListener != null){
            boolean low = (percent <= 15);
            mListener.onBatteryChanged(low);
        }
    }

    public final BroadcastReceiver BROADCAST_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())){
                if(mListener != null){mListener.onBatteryChanged(true);}
            } else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())){
                if(mListener != null){mListener.onBatteryChanged(false);}
            }
        }
    };

    public static void fakeBattery(Context c, boolean low){
        Intent i = low ? new Intent(Intent.ACTION_BATTERY_LOW) : new Intent(Intent.ACTION_BATTERY_OKAY);
        c.sendBroadcast(i);
    }
}
