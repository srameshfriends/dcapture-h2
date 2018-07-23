package dcapture.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public class JsonResponse extends HttpServletResponseWrapper {
    private final Logger logger = LogManager.getLogger(JsonResponse.class);

    public JsonResponse(HttpServletResponse response) {
        super(response);
        setContentType("application/json");
    }

    public void success(String title) {
        send(SC_OK, title);
    }

    public void accepted(String title) {
        send(SC_ACCEPTED, title);
    }

    public void unavailable(String title) {
        send(SC_SERVICE_UNAVAILABLE, title);
    }

    public void noContent(String title) {
        send(SC_NO_CONTENT, title);
    }

    public void forbidden(String title) {
        send(SC_FORBIDDEN, title);
    }

    public void error(String title) {
        send(SC_BAD_REQUEST, title);
    }

    public void unauthorized(String title) {
        send(SC_UNAUTHORIZED, title);
    }

    public void send(JsonValue value) {
        if (value == null) {
            send(SC_BAD_REQUEST, JsonValue.NULL);
        } else {
            send(SC_OK, value);
        }
    }

    private void send(int status, String text) {
        send(status, Json.createValue(text));
    }

    private void send(int status, JsonValue value) {
        try {
            if (value == null) {
                value = JsonValue.NULL;
            }
            setContentType(getContentType());
            setStatus(status);
            setCharacterEncoding("UTF-8");
            getWriter().write(value.toString());
            getWriter().close();
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
            try {
                sendError(SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}