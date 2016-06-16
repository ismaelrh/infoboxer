package infoboxer.backend.common.dto;

/**
 * Created by ismaro3 on 10/02/16.
 */
public class StatusMessage {


    private String status;
    private String message;
    private Integer sessionId;
    public StatusMessage() {}

    public StatusMessage(String status, String message) {
        this.status = status;
        this.message = message;
    }



    public StatusMessage(String status) {
        this.status = status;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
