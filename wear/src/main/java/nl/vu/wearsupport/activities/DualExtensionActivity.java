package nl.vu.wearsupport.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import nl.vu.common.object.AppInfo;
import nl.vu.wearsupport.R;
import nl.vu.wearsupport.utils.ExtensionUtils;
import nl.vu.wearsupport.utils.SettingsManager;

/**
 * Created by robdeknegt on 26/05/15.
 */
public class DualExtensionActivity extends Activity {

    public static final String APPINFO_LIST = "appinfoList";

    private AppInfo[] mAppInfoList;

    private TextView mTopTextView, mBottomTextView;
    private ImageView mTopImageView, mBottomImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual_extension);

        Parcelable[] parcels = getIntent().getParcelableArrayExtra(APPINFO_LIST);
        mAppInfoList = new AppInfo[parcels.length];
        for(int i = 0; i < parcels.length; i++){
            mAppInfoList[i] = (AppInfo) parcels[i];
        }

        findViews();
        setupFontSize();
        fillViews();
    }

    private void setupFontSize(){
        float size = getResources().getDimension(SettingsManager.getSystemFontSizeResourceId(this));
        mTopTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        mBottomTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }

    private void fillViews(){
        if(mAppInfoList.length >= 1){
            setupTopButton();
        }
        if(mAppInfoList.length >= 2){
            setupBottomButton();
        }
    }

    private void setupTopButton() {
        final AppInfo info = mAppInfoList[0];
        mTopTextView.setText(info.getAppName());
        mTopImageView.setImageDrawable(info.getAppIcon(this));
        final View parent = (View) mTopImageView.getParent();
        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick(info.getComponentName());
            }
        });
        if(info.hasNotification()){
            parent.setBackgroundResource(R.drawable.stroke_notified);
        }
    }

    private void setupBottomButton() {
        final AppInfo info = mAppInfoList[1];
        mBottomTextView.setText(info.getAppName());
        mBottomImageView.setImageDrawable(info.getAppIcon(this));
        final View parent = (View) mBottomImageView.getParent();
        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick(info.getComponentName());
            }
        });
        if(info.hasNotification()){
            parent.setBackgroundResource(R.drawable.stroke_notified);
        }
    }

    private void handleClick(ComponentName componentName) {
        ExtensionUtils.startPlugin(DualExtensionActivity.this, componentName);
    }

    private void findViews(){
        mTopTextView = (TextView) findViewById(R.id.top_text);
        mBottomTextView = (TextView) findViewById(R.id.bottom_text);

        mTopImageView = (ImageView) findViewById(R.id.top_icon);
        mBottomImageView = (ImageView) findViewById(R.id.bottom_icon);
    }

    public static void startDualExtensionActivity(Context context, AppInfo[] appInfoList){
        Intent i = new Intent(context, DualExtensionActivity.class);
        if(appInfoList.length > 2){
            throw new UnsupportedOperationException("DualExtensionActivity does not support more than 2 apps in launcher! Use MultiExtensionAcivity?");
        }
        i.putExtra(APPINFO_LIST, appInfoList);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish(); //makes sure notification data is updated
    }
}
