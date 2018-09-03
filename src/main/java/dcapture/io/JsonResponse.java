package dcapture.io;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public class JsonResponse extends HttpServletResponseWrapper {

    public JsonResponse(HttpServletResponse response) {
        super(response);
        setContentType("application/json");
    }

    public void success(String title) {
        error(SC_OK, title);
    }

    public void accepted(String title) {
        error(SC_ACCEPTED, title);
    }

    public void unavailable(String title) {
        error(SC_SERVICE_UNAVAILABLE, title);
    }

    public void noContent(String title) {
        error(SC_NO_CONTENT, title);
    }

    public void forbidden(String title) {
        error(SC_FORBIDDEN, title);
    }

    public void error(String title) {
        error(SC_BAD_REQUEST, title);
    }

    public void error(int status, String title) {
        try {
            setContentType(getContentType());
            setStatus(status);
            setCharacterEncoding("UTF-8");
            getWriter().write(title);
            getWriter().close();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                sendError(SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void unauthorized(String title) {
        error(SC_UNAUTHORIZED, title);
    }

    public void sendObject(JsonObject object) {
        try {
            if (object == null) {
                object = Json.createObjectBuilder().build();
            }
            setContentType(getContentType());
            setStatus(SC_OK);
            setCharacterEncoding("UTF-8");
            Json.createWriter(getWriter()).writeObject(object);
            getWriter().close();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                sendError(SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendArray(JsonArray array) {
        try {
            if (array == null) {
                array = Json.createArrayBuilder().build();
            }
            setContentType(getContentType());
            setStatus(SC_OK);
            setCharacterEncoding("UTF-8");
            Json.createWriter(getWriter()).writeArray(array);
            getWriter().close();
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                sendError(SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}