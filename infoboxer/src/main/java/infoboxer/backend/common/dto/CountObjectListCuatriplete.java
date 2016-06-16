package infoboxer.backend.common.dto;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ismaro3 on 19/08/15.
 */
public class CountObjectListCuatriplete {

  HashMap<String,List<CountObject>> uses;
    HashMap<String,List<CountObject>> instances;
    HashMap<String,HashMap<String,List<CountObject>>> intersectionValues;
    HashMap<String,HashMap<String,List<CountObject>>> unionValues;




    public CountObjectListCuatriplete(HashMap<String, List<CountObject>> uses, HashMap<String, List<CountObject>> instances,
                                      HashMap<String, HashMap<String, List<CountObject>>> intersectionValues,
                                      HashMap<String, HashMap<String, List<CountObject>>> unionValues) {
        this.uses = uses;
        this.instances = instances;
        this.intersectionValues = intersectionValues;
        this.unionValues = unionValues;
    }

    public HashMap<String, List<CountObject>> getUses() {
        return uses;
    }

    public void setUses(HashMap<String, List<CountObject>> uses) {
        this.uses = uses;
    }

    public HashMap<String, List<CountObject>> getInstances() {
        return instances;
    }

    public void setInstances(HashMap<String, List<CountObject>> instances) {
        this.instances = instances;
    }


    public  HashMap<String,HashMap<String,List<CountObject>>> getIntersectionValues() {
        return intersectionValues;
    }

    public void setIntersectionValues(HashMap<String,HashMap<String,List<CountObject>>> intersectionValues) {
        this.intersectionValues = intersectionValues;
    }


    public  HashMap<String,HashMap<String,List<CountObject>>> getUnionValues() {
        return unionValues;
    }

    public void setUnionValues(HashMap<String,HashMap<String,List<CountObject>>> unionValues) {
        this.unionValues = unionValues;
    }


}
