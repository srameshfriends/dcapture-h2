package dcapture.io;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class HtmlRequest extends HttpServletRequestWrapper {

    public HtmlRequest(HttpServletRequest request) {
        super(request);
    }

    public String getHttpPath() {
        int serverPort = getServerPort();
        StringBuilder builder = new StringBuilder();
        builder.append(getScheme()).append("://").append(getServerName());
        if (serverPort != 80 && serverPort != 443) {
            builder.append(":").append(serverPort);
        }
        builder.append(getContextPath()).append(getServletPath());
        return builder.toString();
    }
}