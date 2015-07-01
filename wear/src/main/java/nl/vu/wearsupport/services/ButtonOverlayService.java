package nl.vu.wearsupport.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import nl.vu.wearsupport.R;

/**
 * Created by robdeknegt on 15/05/15.
 *
 * When started, this service overlays a transparent layer over the complete, which launches the ExtensionLauncher activity when clicked.
 * The starting and stopping of the service should be handled elsewhere.
 */
public class ButtonOverlayService extends Service {

    public static final String TAG = "MenuButtonService";


    /**
     * Binder used by an activity or service to get a reference to this service
     */
    private final IBinder mBinder = new ButtonOverlayBinder();

    /**
     * The transparent view thats is drawn over the watch face to intercept the clicks
     */
    private View mMenuButtonView;

    public ButtonOverlayService() {
        //Required empty constructor
    }

    /**
     * MyBinder class that exposes the methods available through the binder
     */
    public class ButtonOverlayBinder extends Binder {
        void enableButton(boolean enable){if(enable){showButton();}else{hideButton();}}
        void setOnClickListener(View.OnClickListener listener){
            mClickListener = listener;
        }
    }

    View.OnClickListener mClickListener;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if(mMenuButtonView != null){
            manager.removeView(mMenuButtonView);
        }
        super.onDestroy();
    }

    private void setupButton(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mMenuButtonView = inflater.inflate(R.layout.service_menu_button, null);
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;

        mMenuButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mClickListener != null){
                    mClickListener.onClick(mMenuButtonView);
                }
            }
        });

        mMenuButtonView.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if(mClickListener != null){
                    mClickListener.onClick(mMenuButtonView);
                    return true;
                }
                return false;
            }
        });

        manager.addView(mMenuButtonView, params);
    }

    private void hideButton(){
        if(mMenuButtonView != null){
            mMenuButtonView.setVisibility(View.GONE);
        }
    }

    private void showButton(){
        if(mMenuButtonView != null){
            mMenuButtonView.setVisibility(View.VISIBLE);
        }else{
            setupButton();
        }
    }
}
