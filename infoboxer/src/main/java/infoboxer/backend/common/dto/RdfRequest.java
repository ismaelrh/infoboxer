package infoboxer.backend.common.dto;

/**
 * Created by ismaro3 on 14/11/15.
 */
public class RdfRequest {

    public int sessionId;
    public long timestamp;
    public String categories;
    public String pageName;
    public String rdfCode;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
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

    public String getRdfCode() {
        return rdfCode;
    }

    public void setRdfCode(String rdfCode) {
        this.rdfCode = rdfCode;
    }
}

