package infoboxer.backend.common.dto;

import java.util.ArrayList;

/**
 * Created by ismaro3 on 28/08/15.
 */
public class PropertyHolderSimulator {

    public String property;
    ArrayList<String> classes;


    public PropertyHolderSimulator(){
        this.classes = new ArrayList<String>();

    }

    public PropertyHolderSimulator(String property, ArrayList<String> classes) {
        this.property = property;
        this.classes = classes;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public ArrayList<String> getClasses() {
        return classes;
    }

    public void setClasses(ArrayList<String> classes) {
        this.classes = classes;
    }

    public void addClass(String _class){
        this.classes.add(_class);
    }

    public int classNumber(){
        return this.classes.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyHolderSimulator that = (PropertyHolderSimulator) o;

        return property.equals(that.property);

    }

    @Override
    public int hashCode() {
        return property.hashCode();
    }
}
