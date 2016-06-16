package infoboxer.backend.common.dto;

import java.util.Comparator;

/**
 * Created by ismaro3 on 3/09/15.
 */
public class CountObjectComparatorAlphabetical implements Comparator<CountObject> {

    public int compare(CountObject o1, CountObject o2) {
        String nombre1 = o1.getLabel();
        String nombre2 = o2.getLabel();
        return nombre1.compareToIgnoreCase(nombre2);
    }
}