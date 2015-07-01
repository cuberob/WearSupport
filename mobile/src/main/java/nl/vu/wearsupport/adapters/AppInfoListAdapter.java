package nl.vu.wearsupport.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import nl.vu.common.object.AppInfo;
import nl.vu.wearsupport.R;

/**
 * Created by robdeknegt on 21/05/15.
 */
public class AppInfoListAdapter extends ArrayAdapter<AppInfo> {

    private static final String TAG = "AppInfoListAdapter";

    private ArrayList<AppInfo> mAppInfoList;
    private Context context;

    public AppInfoListAdapter(Context context, ArrayList<AppInfo> mAppInfoList) {
        super(context, R.layout.row_layout_appinfo);
        this.mAppInfoList = mAppInfoList;
        this.context = context.getApplicationContext();
    }

    static class ViewHolder{
        TextView appName;
        ImageView appIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mViewHolder;
        if(convertView == null){
            mViewHolder = new ViewHolder();
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row_layout_appinfo, parent, false);

            mViewHolder.appName = (TextView) convertView.findViewById(R.id.appname_textview);
            mViewHolder.appIcon = (ImageView) convertView.findViewById(R.id.appicon_imageview);

            convertView.setTag(mViewHolder);
        }else{
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        mViewHolder.appName.setText(mAppInfoList.get(position).getAppName());
        try {
            mViewHolder.appIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(mAppInfoList.get(position).getComponentName().getPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find icon of: " + mAppInfoList.get(position).getComponentName().getPackageName() + " (invalid package?)");
        }

        return convertView;
    }

    @Override
    public int getCount() {
        return mAppInfoList.size();
    }
}
