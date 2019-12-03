package dcapture.servlet.context;

public class RequestInfo {
    private final String path, method, contentType;

    public RequestInfo(String path, String method, String contentType) {
        this.path = path;
        this.method = method;
        this.contentType = contentType;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getContentType() {
        return contentType;
    }
}
