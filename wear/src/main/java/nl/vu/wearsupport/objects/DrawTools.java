package nl.vu.wearsupport.objects;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;

import java.util.List;

import nl.vu.wearsupport.R;
import nl.vu.wearsupport.utils.DisplayUtils;

/**
 * Created by rob on 28/05/15.
 * This class contains all objects used to draw on the canvas and also provides some helper methods to adjust these objects.
 */
public class DrawTools {


    public static final int ACTIVITY_PAINT_WIDTH_DIP = 14;

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private Context context;

    public Paint mBackgroundPaint = new Paint();
    public Paint mTimeTextPaint;
    public Paint mStepTextPaint;
    public Paint mActivityPaint;
    public Paint mHourPaint;
    public Paint mMinutePaint;
    public Paint mTickPaint;

    public Bitmap mNotificationBitmap;
    public Bitmap mBatteryLowBitmap;

    public DrawTools(Context context, boolean inverse) {
        this.context = context.getApplicationContext();
        initPaint(inverse);
    }

    private void initPaint(boolean inverse){
        initActivityPaint();
        initDigitalPaint();
        initAnalogPaint();
        setupPaintColors(inverse);
    }

    public void setupPaintColors(boolean inverse){
        Resources resources = context.getResources();
        if(inverse){
            mBackgroundPaint.setColor(resources.getColor(R.color.digital_background_inverse));
            mTimeTextPaint.setColor(resources.getColor(R.color.digital_text_inverse));
            mStepTextPaint.setColor(resources.getColor(R.color.digital_text_inverse));
            mHourPaint.setColor(resources.getColor(R.color.digital_text_inverse));
            mMinutePaint.setColor(resources.getColor(R.color.digital_text_inverse));
            mTickPaint.setColor(resources.getColor(R.color.digital_text_inverse));
        }else{
            mBackgroundPaint.setColor(resources.getColor(R.color.digital_background));
            mTimeTextPaint.setColor(resources.getColor(R.color.digital_text));
            mStepTextPaint.setColor(resources.getColor(R.color.digital_text));
            mHourPaint.setColor(resources.getColor(R.color.digital_text));
            mMinutePaint.setColor(resources.getColor(R.color.digital_text));
            mTickPaint.setColor(resources.getColor(R.color.digital_text));
        }
    }

    private void initAnalogPaint() {
        mHourPaint = new Paint();
        mHourPaint.setARGB(255, 200, 200, 200);
        mHourPaint.setStrokeWidth(8.f);
        mHourPaint.setAntiAlias(true);
        mHourPaint.setStrokeCap(Paint.Cap.ROUND);

        mMinutePaint = new Paint();
        mMinutePaint.setARGB(255, 200, 200, 200);
        mMinutePaint.setStrokeWidth(6.f);
        mMinutePaint.setAntiAlias(true);
        mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

        mTickPaint = new Paint();
        mTickPaint.setARGB(255, 255, 255, 255);
        mTickPaint.setStrokeWidth(3.f);
        mTickPaint.setAntiAlias(true);
    }

    private void initDigitalPaint() {
        mTimeTextPaint = createTextPaint(context.getResources().getColor(R.color.digital_text));
        mStepTextPaint = createTextPaint(context.getResources().getColor(R.color.digital_text));

        mTimeTextPaint.setTextSize(context.getResources().getDimension(R.dimen.digital_text_size_round));
        mStepTextPaint.setTextSize(context.getResources().getDimension(R.dimen.step_count_text_size_round));
    }


    /**
     * @param notifications Package names of the applications with a pending notification
     */
    public void updateNotificationIcon(List<Notification> notifications){
        if(notifications.size() == 0) { //No notification, clear all icons
            this.mNotificationBitmap = null;
        }else if(notifications.size() == 1){ //Only one 1 notification, use apps icon
            this.mNotificationBitmap = getIconBitMap(notifications.get(0).getPackageName());
        }else{ //More than 1 notification, use general notification icon
            mNotificationBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_notifications);
        }
    }

    private Bitmap getIconBitMap(String packageName) {
        try {
            return ((BitmapDrawable) context.getPackageManager().getApplicationIcon(packageName)).getBitmap();
        } catch (PackageManager.NameNotFoundException e) { //Failed to get app icon, set to default notification icon
            return BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_notifications);
        }
    }

    public void updateLowBattery(boolean lowBattery) {
        if(lowBattery){
            this.mBatteryLowBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.battery_low_ic);
        }else{
            this.mBatteryLowBitmap = null;
        }
    }

    private Paint createTextPaint(int textColor) {
        Paint paint = new Paint();
        paint.setColor(textColor);
        paint.setTypeface(NORMAL_TYPEFACE);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        return paint;
    }

    private void initActivityPaint(){
        mActivityPaint = new Paint();
        mActivityPaint.setStrokeWidth(DisplayUtils.DipToPx(context, ACTIVITY_PAINT_WIDTH_DIP));
        mActivityPaint.setAntiAlias(true);
        mActivityPaint.setStrokeCap(Paint.Cap.SQUARE);
        mActivityPaint.setStyle(Paint.Style.STROKE);
        mActivityPaint.setColor(Color.GREEN);

        //Code used when experimenting with a gradient in stead of a solid green color for the activity circle. But for visual simplicity ended up using only the green color
        /*int[] colors = {Color.YELLOW, Color.GREEN};
        float[] positions = {0,1};
        SweepGradient gradient = new SweepGradient(context.getResources().getDisplayMetrics().widthPixels / 2,
                context.getResources().getDisplayMetrics().heightPixels / 2,
                colors ,
                positions);
        paint.setShader(gradient);*/
    }

    public void setLowBitmode(boolean lowBitmode, boolean inAmbientMode) {
        if (lowBitmode) {
            mTimeTextPaint.setAntiAlias(!inAmbientMode);
            mStepTextPaint.setAntiAlias(!inAmbientMode);
            mActivityPaint.setAntiAlias(!inAmbientMode);
            mHourPaint.setAntiAlias(!inAmbientMode);
            mMinutePaint.setAntiAlias(!inAmbientMode);
        }
    }
}
