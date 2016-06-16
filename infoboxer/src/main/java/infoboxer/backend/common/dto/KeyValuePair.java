package infoboxer.backend.common.dto;

/**
 * Created by ismaro3 on 28/02/16.
 */
public class KeyValuePair<T> {

    public String key;



    public T value;

    public KeyValuePair(){

    }

    public KeyValuePair(String key, T value){
        this.key = key;
        this.value = value;
    }


    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }


}
