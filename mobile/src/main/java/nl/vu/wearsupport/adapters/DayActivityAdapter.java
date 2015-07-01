package nl.vu.wearsupport.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import nl.vu.common.object.DayActivity;
import nl.vu.wearsupport.R;

/**
 * Created by robdeknegt on 22/06/15.
 */
public class DayActivityAdapter extends ArrayAdapter<DayActivity> {

    Context context;
    List<DayActivity> mActivityHistory;

    public DayActivityAdapter(Context context, List<DayActivity> mActivityHistory) {
        super(context, R.layout.day_activity_row);
        this.context = context;
        this.mActivityHistory = mActivityHistory;
        Collections.reverse(this.mActivityHistory); //To make the most recent data display at the top of the list
    }

    static class ViewHolder {
        private TextView date;
        private TextView steps;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mViewHolder = null;
        if(convertView == null){
            mViewHolder = new ViewHolder();

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.day_activity_row, parent, false);

            mViewHolder.date = (TextView) convertView.findViewById(R.id.date_textview);
            mViewHolder.steps = (TextView) convertView.findViewById(R.id.steps_textview);

            convertView.setTag(mViewHolder);
        }else{
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        mViewHolder.date.setText(context.getString(R.string.date) + sdf.format(mActivityHistory.get(position).getDate().getTimeInMillis()));
        mViewHolder.steps.setText(context.getString(R.string.steps) + mActivityHistory.get(position).stepCount);

        return convertView;
    }

    public List<DayActivity> getActivityHistory(){
        return mActivityHistory;
    }

    @Override
    public int getCount() {
        return mActivityHistory == null ? 0 : mActivityHistory.size();
    }
}
