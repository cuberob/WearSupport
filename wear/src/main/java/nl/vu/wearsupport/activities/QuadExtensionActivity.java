package nl.vu.wearsupport.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.wearable.view.WatchViewStub;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import nl.vu.common.object.AppInfo;
import nl.vu.wearsupport.R;
import nl.vu.wearsupport.utils.PluginUtils;
import nl.vu.wearsupport.utils.SettingsManager;

/**
 * Created by robdeknegt on 26/05/15.
 */
public class QuadExtensionActivity extends Activity {

    public static final String APPINFO_LIST = "appinfoList";
    public static final String TAG = "QuadExtensionActivity";

    private enum TargetView {TOP, RIGHT, BOTTOM, LEFT}; //For square screens this is top_left, top_right, bottom_right, bottom_left

    private LinearLayout mTopLL, mRightLL, mBottomLL, mLeftLL;
    private TextView mTopTextView, mRightTextView, mBottomTextView, mLeftTextView;
    private ImageView mTopImageView, mRightImageView, mBottomImageView, mLeftImageView;

    private AppInfo[] mAppInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quad_extension);

        Parcelable[] parcels = getIntent().getParcelableArrayExtra(APPINFO_LIST);
        mAppInfoList = new AppInfo[parcels.length];
        for(int i = 0; i < parcels.length; i++){
            mAppInfoList[i] = (AppInfo) parcels[i];
        }

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                findViews(stub);
                setupFontSize();
                fillViews();
            }
        });
    }

    private void setupFontSize(){
        float size = getResources().getDimension(SettingsManager.getSystemFontSizeResourceId(this));
        mLeftTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        mRightTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        mTopTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        mBottomTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }

    private void fillViews() {
        for (int i = 0; i < mAppInfoList.length; i++) {
            //Select the correct target view
            TargetView target = TargetView.LEFT;
            switch (i) {
                case 1:
                    target = TargetView.RIGHT;
                    break;
                case 2:
                    target = TargetView.TOP;
                    break;
                case 3:
                    target = TargetView.BOTTOM;
                    break;
            }
            //Fill the view with the appinfo data
            fillView(mAppInfoList[i], target);
        }
    }

    private void findViews(WatchViewStub stub){
        mTopLL = (LinearLayout) stub.findViewById(R.id.extension_top_view);
        mRightLL = (LinearLayout) stub.findViewById(R.id.extension_right_view);
        mBottomLL = (LinearLayout) stub.findViewById(R.id.extension_bottom_view);
        mLeftLL = (LinearLayout) stub.findViewById(R.id.extension_left_view);

        mTopImageView = (ImageView) stub.findViewById(R.id.extension_top_imageview);
        mRightImageView = (ImageView) stub.findViewById(R.id.extension_right_imageview);
        mBottomImageView = (ImageView) stub.findViewById(R.id.extension_bottom_imageview);
        mLeftImageView = (ImageView) stub.findViewById(R.id.extension_left_imageview);

        mTopTextView = (TextView) stub.findViewById(R.id.extension_top_textview);
        mRightTextView = (TextView) stub.findViewById(R.id.extension_right_textview);
        mBottomTextView = (TextView) stub.findViewById(R.id.extension_bottom_textview);
        mLeftTextView = (TextView) stub.findViewById(R.id.extension_left_textview);

        stub.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                return handleInsets(insets);
            }
        });
    }

    private void fillView(AppInfo info, TargetView targetView) {
        final ComponentName componentName = info.getComponentName();
        TextView targetTextView = null;
        ImageView targetImageView = null;
        LinearLayout targetLL = null;

        switch(targetView){
            case TOP:
                targetTextView = mTopTextView;
                targetImageView = mTopImageView;
                targetLL = mTopLL;
                break;
            case RIGHT:
                targetTextView = mRightTextView;
                targetImageView = mRightImageView;
                targetLL = mRightLL;
                break;
            case BOTTOM:
                targetTextView = mBottomTextView;
                targetImageView = mBottomImageView;
                targetLL = mBottomLL;
                break;
            case LEFT:
                targetTextView = mLeftTextView;
                targetImageView = mLeftImageView;
                targetLL = mLeftLL;
                break;
        }

        targetTextView.setText(info.getAppName());
        targetImageView.setImageDrawable(info.getAppIcon(getApplicationContext()));
        targetLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick(componentName);
            }
        });
        if(info.hasNotification()){
            ((View)targetLL.getParent()).setBackgroundResource(getNotifiedBackgroundResource(targetView));
        }
    }

    private int getNotifiedBackgroundResource(TargetView targetView) {
        switch(targetView){
            case TOP:
                return R.drawable.round_stroke_top_notified;
            case RIGHT:
                return R.drawable.round_stroke_right_notified;
            case BOTTOM:
                return R.drawable.round_stroke_bottom_notified;
            case LEFT:
                return R.drawable.round_stroke_left_notified;
        }
        return R.drawable.stroke_notified;
    }

    private void handleClick(ComponentName componentName) {
        PluginUtils.startPlugin(QuadExtensionActivity.this, componentName);
    }

    public static void startQuadExtensionActivity(Context context, AppInfo[] appInfoList){
        Intent i = new Intent(context, QuadExtensionActivity.class);
        if(appInfoList.length > 4){
            throw new UnsupportedOperationException("QuadExtensionActivity does not support more than 4 apps in launcher! Use MultiExtensionAcivity?");
        }
        i.putExtra(APPINFO_LIST, appInfoList);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    private WindowInsets handleInsets(WindowInsets insets) {
        int bottomChin = insets.getSystemWindowInsetBottom();
        if(bottomChin == 0){
            //No bottom chin, thus no moto 360 like flat tire so we can return immediately
            return insets;
        }

        //Moto 360 style chin detected, add extra padding to bottom view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics()); //TODO: play with this 20dp value
        bottomChin = bottomChin + (int)px;
        params.setMargins(0, 0, 0, bottomChin);

        mBottomTextView.setLayoutParams(params);
        return insets;
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish(); //No need to hang around
    }
}
