package nl.vu.common.object;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by robdeknegt on 18/05/15.
 */
public class AppInfo implements Parcelable {

    String appName;
    ComponentName componentName;
    boolean hasNotification;


    public AppInfo(String appName, ComponentName componentName) {
        this.appName = appName;
        this.componentName = componentName;
        this.hasNotification = false;
    }

    public AppInfo(String appName, ComponentName componentName, boolean hasNotification) {
        this.appName = appName;
        this.componentName = componentName;
        this.hasNotification = hasNotification;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getAppIcon(Context context){
        PackageManager manager = context.getPackageManager();
        try {
            return manager.getApplicationIcon(componentName.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ComponentName getComponentName() {
        return componentName;
    }

    public void setComponentName(ComponentName componentName) {
        this.componentName = componentName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.appName);
        dest.writeParcelable(this.componentName, 0);
        dest.writeByte(hasNotification ? (byte) 1 : (byte) 0);
    }

    private AppInfo(Parcel in) {
        this.appName = in.readString();
        this.componentName = in.readParcelable(ComponentName.class.getClassLoader());
        this.hasNotification = in.readByte() != 0;
    }

    public static final Creator<AppInfo> CREATOR = new Creator<AppInfo>() {
        public AppInfo createFromParcel(Parcel source) {
            return new AppInfo(source);
        }

        public AppInfo[] newArray(int size) {
            return new AppInfo[size];
        }
    };

    public boolean hasNotification() {
        return this.hasNotification;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "appName='" + appName + '\'' +
                ", componentName=" + componentName +
                ", hasNotification=" + hasNotification +
                '}';
    }
}
