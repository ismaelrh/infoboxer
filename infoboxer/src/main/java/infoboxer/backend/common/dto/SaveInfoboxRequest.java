package infoboxer.backend.common.dto;

/**
 * Created by ismaro3 on 28/03/16.
 */
public class SaveInfoboxRequest {

    public int sessionId;
    public long timestamp;
    public String categories;
    public String pageName;
    public String infoboxCode;

    public SaveInfoboxRequest(){}

    public SaveInfoboxRequest(int sessionId, long timestamp, String categories, String pageName, String infoboxCode) {
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.categories = categories;
        this.pageName = pageName;
        this.infoboxCode = infoboxCode;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getInfoboxCode() {
        return infoboxCode;
    }

    public void setInfoboxCode(String infoboxCode) {
        this.infoboxCode = infoboxCode;
    }
}
