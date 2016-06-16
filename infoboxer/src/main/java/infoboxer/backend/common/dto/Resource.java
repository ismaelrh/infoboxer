package infoboxer.backend.common.dto;

/**
 * Created by ismaro3 on 29/02/16.
 */
public class Resource {

    public String uri;
    public String label;
    public String type;


    public Resource(){

    }
    public Resource(String label, String uri, String type) {
        this.label = label;
        this.uri = uri;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
