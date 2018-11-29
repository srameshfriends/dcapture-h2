package dcapture.io;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class TextMessage {
    static final int SC_OK = 200;
    private static final Logger logger = Logger.getLogger("dcapture.io");
    private static final int SC_ACCEPTED = 202;
    private static final int SC_SERVICE_UNAVAILABLE = 503;
    private static final int SC_NO_CONTENT = 204;
    private static final int SC_FORBIDDEN = 403;
    private static final int SC_BAD_REQUEST = 400;
    private static final int SC_UNAUTHORIZED = 401;
    private int status = SC_OK;
    private String message = "";

    public TextMessage() {
    }

    public TextMessage(String message) {
        this.message = message;
    }

    public TextMessage(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public static void onSuccess(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(SC_OK);
        response.setContentLength(message.length());
        response.getWriter().write(message);
        response.getWriter().close();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private void setResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public TextMessage success(String message) {
        setResponse(SC_OK, message);
        return TextMessage.this;
    }

    public TextMessage accepted(String message) {
        setResponse(SC_ACCEPTED, message);
        return TextMessage.this;
    }

    public TextMessage unavailable(String message) {
        setResponse(SC_SERVICE_UNAVAILABLE, message);
        return TextMessage.this;
    }

    public TextMessage noContent(String message) {
        setResponse(SC_NO_CONTENT, message);
        return TextMessage.this;
    }

    public TextMessage forbidden(String message) {
        setResponse(SC_FORBIDDEN, message);
        return TextMessage.this;
    }

    public TextMessage error(String message) {
        setResponse(SC_BAD_REQUEST, message);
        return TextMessage.this;
    }

    public TextMessage unauthorized(String message) {
        setResponse(SC_UNAUTHORIZED, message);
        return TextMessage.this;
    }
}