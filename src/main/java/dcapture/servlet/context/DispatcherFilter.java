package dcapture.servlet.context;

import dcapture.api.io.ResponseHandler;
import dcapture.api.support.Messages;
import dcapture.api.support.SessionHandler;
import io.github.pustike.inject.Injector;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DispatcherFilter implements Filter {
    private static final Logger logger = Logger.getLogger(DispatcherFilter.class);
    private static final int SC_UNAUTHORIZED = 401, SC_BAD_REQUEST = 400;
    private final Set<String> validContentTypes, validMethods;
    private DispatcherMap dispatcherMap;
    private SessionHandler sessionHandler;

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
    public void init(FilterConfig config) {
        Injector injector = (Injector)config.getServletContext().getAttribute(Injector.class.getName());
        dispatcherMap = injector.getInstance(DispatcherMap.class);
        String handlerName = config.getServletContext().getInitParameter(SessionHandler.class.getName());
        try {
            Class<?> handlerClass = Class.forName(handlerName);
            sessionHandler = (SessionHandler) injector.getInstance(handlerClass);
        } catch (ClassNotFoundException ex) {
            logger.error("ERROR : To Create Session handler  : " + ex.getMessage());
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String path = getValidPath(request.getPathInfo());
        final String method = getValidMethod(request.getMethod());
        final String contentType = getValidContentType(request.getContentType());
        RequestInfo info = new RequestInfo(path, method, contentType);
        req.setAttribute(RequestInfo.class.getName(), info);
        Dispatcher dispatcher = dispatcherMap.getDispatcher(path);
        if (dispatcher == null) {
            ResponseHandler.send(response, SC_BAD_REQUEST,
                    Messages.getMessage("application.path.error", path));
        } else if (!method.equals(dispatcher.getHttpMethod())) {
            Object[] args = new String[]{path, method, dispatcher.getHttpMethod()};
            ResponseHandler.send(response, SC_BAD_REQUEST,
                    Messages.getMessage("application.httpMethod.error", args));
        } else if (dispatcher.isSecured() && !sessionHandler.isValidSession(request)) {
            ResponseHandler.send(response, SC_UNAUTHORIZED,
                    Messages.getMessage("application.unauthorized.error", path));
        } else {
            chain.doFilter(request, response);
        }
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
