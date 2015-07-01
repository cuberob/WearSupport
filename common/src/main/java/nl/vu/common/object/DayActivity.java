package nl.vu.common.object;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by robdeknegt on 18/05/15.
 */
public class DayActivity {

    public int stepCount;
    public long date; //Use long (timeInMillis) representation to save using Gson

    public DayActivity(){
    }

    public DayActivity(int stepCount, Calendar date) {
        this.stepCount = stepCount;
        this.date = date.getTimeInMillis();
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return "Date: " + sdf.format(this.date) + " - Steps: " + stepCount;
    }

    public Calendar getDate(){
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(this.date);
        return result;
    }

    /*
     * Returns true when they are of the same type and have the same date
     */
    @Override
    public boolean equals(Object o) {
        if(o instanceof DayActivity){
            DayActivity toCompare = (DayActivity) o;
            return this.getDate().get(Calendar.DAY_OF_YEAR) == toCompare.getDate().get(Calendar.DAY_OF_YEAR);
        }
        return super.equals(o);
    }
}
