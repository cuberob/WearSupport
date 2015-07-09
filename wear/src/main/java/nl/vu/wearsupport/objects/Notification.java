package nl.vu.wearsupport.objects;

import java.io.Serializable;

/**
 * Created by robdeknegt on 09/07/15.
 */
public class Notification implements Serializable {

    private String packageName;

    public Notification(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Notification that = (Notification) o;

        return packageName.equals(that.packageName);

    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }
}
