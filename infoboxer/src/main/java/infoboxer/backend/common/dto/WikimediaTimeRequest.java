package infoboxer.backend.common.dto;

/**
 * Created by ismaro3 on 19/03/16.
 */
public class WikimediaTimeRequest {


    public String username;
    public String infobox;
    public int time;


    public WikimediaTimeRequest(){

    }
    public WikimediaTimeRequest(String username, String infobox, int time) {
        this.username = username;
        this.infobox = infobox;
        this.time = time;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInfobox() {
        return infobox;
    }

    public void setInfobox(String infobox) {
        this.infobox = infobox;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
