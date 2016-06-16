package infoboxer.backend.common.dto;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by ismaro3 on 13/08/15.
 */
@XmlRootElement
public class SimpleCountObject {

    int count;

    public SimpleCountObject(){};
    public SimpleCountObject(int count){
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }


}
