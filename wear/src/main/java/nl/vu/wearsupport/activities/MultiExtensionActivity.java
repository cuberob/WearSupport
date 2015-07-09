package nl.vu.wearsupport.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import nl.vu.common.object.AppInfo;
import nl.vu.wearsupport.R;
import nl.vu.wearsupport.utils.ExtensionUtils;
import nl.vu.wearsupport.utils.SettingsManager;

/**
 * Created by robdeknegt on 26/05/15.
 */
public class MultiExtensionActivity extends Activity {

    public static final String APPINFO_LIST = "appinfoList";
    private AppInfo[] mAppInfoList;

    private Button mPreviousButton, mNextButton;
    private View mHolder;
    private ImageView mIconImageView;
    private TextView mTitleTextView;

    private int mSelectedMenuItem = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_extension);

        findViews();

        Parcelable[] parcels = getIntent().getParcelableArrayExtra(APPINFO_LIST);
        mAppInfoList = new AppInfo[parcels.length];
        for(int i = 0; i < parcels.length; i++){
            mAppInfoList[i] = (AppInfo) parcels[i];
        }
        if(mAppInfoList.length == 1){ //Only one extension, hide previous and next buttons
            mPreviousButton.setVisibility(View.INVISIBLE);
            mNextButton.setVisibility(View.INVISIBLE);
        }

        setupFontSize();
        setupViews();
    }

    private void setupFontSize(){
        float size = getResources().getDimension(SettingsManager.getSystemFontSizeResourceId(this));
        mTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }

    private void setupViews() {
        mIconImageView.setImageDrawable(mAppInfoList[0].getAppIcon(this));
        mTitleTextView.setText(mAppInfoList[0].getAppName());
        mHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick(mAppInfoList[mSelectedMenuItem].getComponentName());
            }
        });
        if(mAppInfoList[0].hasNotification()) {
            mHolder.setBackgroundResource(R.drawable.stroke_notified);
        }
    }

    private void handleClick(ComponentName componentName) {
        ExtensionUtils.startPlugin(MultiExtensionActivity.this, componentName);
    }

    private void findViews() {
        mPreviousButton = (Button) findViewById(R.id.previous_button);
        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoPrev();
            }
        });
        mNextButton = (Button) findViewById(R.id.next_button);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoNext();
            }
        });
        mHolder = findViewById(R.id.holder);
        mIconImageView = (ImageView) findViewById(R.id.icon);
        mTitleTextView = (TextView) findViewById(R.id.title);
    }


    public static void startMultiExtensionActivity(Context context, AppInfo[] appInfoList){
        Intent i = new Intent(context, MultiExtensionActivity.class);
        i.putExtra(APPINFO_LIST, appInfoList);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    private void gotoPrev() {
        mSelectedMenuItem -= (mSelectedMenuItem == 0) ? (-mAppInfoList.length-1) : 1;
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIconImageView.setImageDrawable(mAppInfoList[mSelectedMenuItem].getAppIcon(getApplicationContext()));
                mTitleTextView.setText(mAppInfoList[mSelectedMenuItem].getAppName());
                mHolder.startAnimation(AnimationUtils.loadAnimation(MultiExtensionActivity.this, android.R.anim.slide_in_left));
                if(mAppInfoList[mSelectedMenuItem].hasNotification()) {
                    mHolder.setBackgroundResource(R.drawable.stroke_notified);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        Animation animation = AnimationUtils.loadAnimation(MultiExtensionActivity.this, android.R.anim.slide_out_right);
        animation.setAnimationListener(listener);
        mHolder.startAnimation(animation);
    }

    private void gotoNext() {
        mSelectedMenuItem += (mSelectedMenuItem < (mAppInfoList.length-1)) ? 1 : -mSelectedMenuItem;
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIconImageView.setImageDrawable(mAppInfoList[mSelectedMenuItem].getAppIcon(getApplicationContext()));
                mTitleTextView.setText(mAppInfoList[mSelectedMenuItem].getAppName());
                mHolder.startAnimation(AnimationUtils.loadAnimation(MultiExtensionActivity.this, R.anim.slide_in_right));
                if(mAppInfoList[mSelectedMenuItem].hasNotification()) {
                    mHolder.setBackgroundResource(R.drawable.stroke_notified);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        Animation animation = AnimationUtils.loadAnimation(MultiExtensionActivity.this, R.anim.slide_out_left);
        animation.setAnimationListener(listener);
        mHolder.startAnimation(animation);
    }


    @Override
    protected void onPause() {
        super.onPause();
        finish(); //makes sure notification data is updated
    }
}
