package infoboxer.backend.common.dto;


/**
 * Created by ismaro3 on 13/08/15.
 *
 */
public class CountObject implements Comparable<CountObject> {

    String _id;
    int count;
    String label;
    boolean semantic;
    String comment;
    RangeForSemantic rangeForSemantic;



    public CountObject(){

    }

    public CountObject(String _id, int count, boolean semantic) {
        this._id = _id;
        this.count = count;
        this.semantic = semantic;
    }

    public CountObject(String _id, int count){
        this._id = _id;
        this.count = count;
    }


    public RangeForSemantic getRangeForSemantic(){
        return rangeForSemantic;
    }

    public void setRangeForSemantic(RangeForSemantic rangeForSemantic){
        this.rangeForSemantic =  rangeForSemantic;
    }

    public boolean isSemantic() {
        return semantic;
    }

    public void setSemantic(boolean semantic) {
        this.semantic = semantic;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getLabel(){
        return label;
    }

    public void setLabel(String label){
        this.label = label;
    }


    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }


    @Override
    /**
     * Compare first by count, then by length, then lexicograpically
     */
    public int compareTo(CountObject countObject) {

        int count  = countObject.getCount() - this.getCount();

        if(count!=0){
            return count;  //First the one with more instances
        }
        else{

            int lengths = this.get_id().length() - countObject.get_id().length();

            if(lengths!=0){
                //Then the shorter one
                return lengths;

            }
            else{
                //Then lexicographically
                return this.get_id().compareTo(countObject.get_id());
            }
        }
    }

    public static class RangeForSemantic{

        public String _id;
        public String label;

        public RangeForSemantic(){}

        public RangeForSemantic(String _id, String label){
            this._id = _id;
            this.label = label;
        }

        public String get_id() {
            return _id;
        }

        public void set_id(String _id) {
            this._id = _id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
