package infoboxer.backend.common.dto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ismaro3 on 28/08/15.
 */
public class PropertyHolderSimulatorList {

    private List<PropertyHolderSimulator> lista;


    public PropertyHolderSimulatorList(){
        lista = new ArrayList<PropertyHolderSimulator>();
    }

    public void add(String property,String _class){

        if(this.contains(property)){
            boolean found = false;
            Iterator<PropertyHolderSimulator> iterator = this.lista.iterator();
            while(iterator.hasNext() && !found){
                PropertyHolderSimulator phs = iterator.next();
                if(phs.getProperty().equalsIgnoreCase(property)){
                    phs.addClass(_class);
                }

            }

        }
        else{
            ArrayList<String> array = new ArrayList<String>();
            array.add(_class);
            PropertyHolderSimulator phs = new PropertyHolderSimulator(property,array);
            this.lista.add(phs);
        }

    }

    public boolean contains(String property){

        boolean contains = false;
        Iterator<PropertyHolderSimulator> iterator = this.lista.iterator();
        while(iterator.hasNext() && !contains){
            PropertyHolderSimulator phs = iterator.next();
            contains = phs.getProperty().equalsIgnoreCase(property);
        }
        return contains;
    }

    public List<PropertyHolderSimulator> getList(){
        return this.lista;
    }
}
