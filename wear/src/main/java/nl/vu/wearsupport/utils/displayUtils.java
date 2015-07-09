package nl.vu.wearsupport.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

/**
 * Created by robdeknegt on 08/07/15.
 */
public class DisplayUtils {

    public static float DipToPx(Context c, int dip){
        Resources r = c.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, r.getDisplayMetrics());
    }
}
