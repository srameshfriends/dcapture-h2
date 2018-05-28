package dcapture.io;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class HtmlResponse extends HttpServletResponseWrapper {
    private final Logger logger = Logger.getLogger(HtmlResponse.class);

    public HtmlResponse(HttpServletResponse response) {
        super(response);
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

    public void send(String text) {
        send(SC_OK, text);
    }

    private void send(int status, String text) {
        try {
            if (text == null) {
                text = "";
            }
            setContentType(getContentType());
            setStatus(status);
            setContentLength(text.length());
            setCharacterEncoding("UTF-8");
            getWriter().write(text);
            getWriter().close();
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
    }
}