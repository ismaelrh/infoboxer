package infoboxer.backend.common.dto;

/**
 * Created by ismaro3 on 14/11/15.
 */
public class InfoboxGenerationRequest {

    public int sessionId;
    public long timestamp;
    public String category;
    public String pageName;
    public String givenInfobox;

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public String getGivenInfobox() {
        return givenInfobox;
    }

    public void setGivenInfobox(String givenInfobox) {
        this.givenInfobox = givenInfobox;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }
}
