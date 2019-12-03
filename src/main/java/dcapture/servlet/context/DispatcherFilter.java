package dcapture.servlet.context;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DispatcherFilter implements Filter {
    private final Set<String> validContentTypes, validMethods;

    public DispatcherFilter() {
        Set<String> hashSet = new HashSet<>();
        Collections.addAll(hashSet, "multipart/form-data",
                "text/html", "text/plain", "text/csv", "application/json", "application/x-www-form-urlencoded");
        validContentTypes = Collections.unmodifiableSet(hashSet);
        Set<String> hashSet2 = new HashSet<>();
        Collections.addAll(hashSet2, "GET", "POST", "DELETE", "HEAD", "PUT", "CONNECT", "TRACE", "OPTIONS");
        validMethods = Collections.unmodifiableSet(hashSet2);
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        System.out.println("AUTHENTICATION FILTER : " + LocalDateTime.now());

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String path = getValidPath(request.getPathInfo());
        final String method = getValidMethod(request.getMethod());
        final String contentType = getValidContentType(request.getContentType());
        req.setAttribute(RequestInfo.class.getName(), new RequestInfo(path, method, contentType));
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    /**
     * Default method is @GET
     * Http servlet request method supported only @GET, @POST and @DELETE
     */
    private String getValidMethod(String method) {
        method = method == null ? "GET" : method.toUpperCase();
        return validMethods.contains(method) ? method : "GET";
    }

    /**
     * Http servlet request path null safe converted to lower case char
     */
    private String getValidPath(String pathInfo) {
        pathInfo = pathInfo == null ? "" : pathInfo.trim().toLowerCase();
        if (pathInfo.equals("") || pathInfo.equals("/")) {
            return "";
        }
        if (pathInfo.endsWith("/")) {
            pathInfo = pathInfo.substring(0, pathInfo.lastIndexOf("/"));
        }
        if (!pathInfo.startsWith("/")) {
            pathInfo = "/".concat(pathInfo);
        }
        return pathInfo;
    }

    /**
     * Default content type is [text/plain]
     * Http servlet request supported content type is [text/plain, application/json, text/html, text/csv, application/x-www-form-urlencoded, multipart/form-data]
     */
    private String getValidContentType(String contentType) {
        contentType = contentType == null ? "text/plain" : contentType.toLowerCase();
        for (String type : validContentTypes) {
            if (contentType.contains(type)) {
                return type;
            }
        }
        return contentType;
    }
}
