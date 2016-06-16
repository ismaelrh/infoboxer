package infoboxer.backend.common.dto;

import java.util.Comparator;

/**
 * Created by ismaro3 on 3/09/15.
 */
public class CountObjectComparator implements Comparator<CountObject> {

    public int compare(CountObject o1, CountObject o2) {
        int cuenta1 = o1.getCount();
        int cuenta2 = o2.getCount();
        return cuenta2 - cuenta1;
    }
}