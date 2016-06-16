package infoboxer.backend.common.dto;

import java.util.HashMap;

/**
 * Created by ismaro3 on 11/11/15.
 */
public class ValueStore {


    HashMap<IdAndLabel, Integer>  store;

    public ValueStore(){
        store = new HashMap<IdAndLabel,Integer>();
    }

    public void add(IdAndLabel idLabel, int quantity){

        Integer q = store.get(idLabel);
        if(q==null){
            store.put(idLabel,quantity);
        }
        else{
            q += quantity;
            store.put(idLabel,q);
        }
    }

    public HashMap<IdAndLabel,Integer> getMap(){
        return store;
    }


    public static class IdAndLabel{
        public String _id;
        public String _label;

        @Override
        public int hashCode(){
            return _id.hashCode();
        }

        @Override
        public boolean equals(Object o){
            return this._id.equals(((IdAndLabel)o)._id);
        }
    }
}
