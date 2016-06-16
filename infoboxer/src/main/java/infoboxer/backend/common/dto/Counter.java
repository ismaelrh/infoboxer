package infoboxer.backend.common.dto;

/**
 * Created by ismaro3 on 3/09/15.
 */
public class Counter {

    int count;

    public Counter(){this.count = 0;}

    public void add(){
        this.count++;
    }

    public void add(int quant){
        this.count += quant;
    }

    public int value(){
        return this.count;
    }
}
