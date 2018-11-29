package dcapture.io;

import io.github.pustike.inject.Injector;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import javax.json.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DispatcherServlet extends GenericServlet {
    private static final Logger logger = Logger.getLogger("dcapture.io");
    private static final int SC_OK = 200;
    private static final int SC_BAD_REQUEST = 400;
    private Map<String, Dispatcher> dispatcherMap;
    private Injector injector;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        injector = (Injector) context.getAttribute(Injector.class.getName());
        HashMap<String, Dispatcher> map = (HashMap<String, Dispatcher>) context.getAttribute("DispatcherMap");
        dispatcherMap = Collections.unmodifiableMap(map);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        response.setContentType(request.getContentType() == null ? "text/html" : request.getContentType());
        final String pathInfo = getPathInfo(request);
        if (!dispatcherMap.containsKey(pathInfo)) {
            sendMessage(response, SC_BAD_REQUEST, "Service not implemented : " + pathInfo);
            return;
        }
        Dispatcher dispatcher = dispatcherMap.get(pathInfo);
        final String httpMethod = request.getMethod() == null ? "POST" : request.getMethod().toUpperCase().trim();
        if (!httpMethod.equals(dispatcher.getHttpMethod())) {
            sendMessage(response, SC_BAD_REQUEST, "Http " + dispatcher.getHttpMethod() + " : " + pathInfo + ". "
                    + httpMethod + " is not valid");
            return;
        }
        if (dispatcher.isSecured() && request.getSession(false) == null) {
            sendMessage(response, SC_BAD_REQUEST, "Unauthenticated Access : " + pathInfo);
            return;
        }
        try {
            Method method = dispatcher.getMethod();
            Class<?> beanClass = method.getDeclaringClass();
            method.setAccessible(true);
            Object serviceBean = injector.getInstance(beanClass);
            Class<?>[] parameters = method.getParameterTypes();
            if (0 == parameters.length) {
                send(response, method.getReturnType(), method.invoke(serviceBean));
            } else if (1 == parameters.length) {
                Object instance = getMethodParameterInstance(pathInfo, parameters[0], request, response);
                send(response, method.getReturnType(), method.invoke(serviceBean, instance));
            } else if (2 == parameters.length) {
                Object firstParam = getMethodParameterInstance(pathInfo, parameters[0], request, response);
                Object secondParam = getMethodParameterInstance(pathInfo, parameters[1], request, response);
                send(response, method.getReturnType(), method.invoke(serviceBean, firstParam, secondParam));
            } else {
                sendMessage(response, SC_BAD_REQUEST, "Request or return class type not supported");
            }
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null) {
                message = ex.getCause() == null ? "Unknown Error" : ex.getCause().getMessage();
            }
            sendMessage(response, SC_BAD_REQUEST, message);
            if (logger.isLoggable(Level.FINE)) {
                ex.printStackTrace();
            }
        }
    }

    private Object getMethodParameterInstance(String url, Class<?> pCls, HttpServletRequest req, HttpServletResponse res)
            throws Exception {
        if (JsonRequest.class.equals(pCls)) {
            return new JsonRequest(req);
        } else if (JsonObject.class.equals(pCls)) {
            return getRequestJsonObject(url, req);
        } else if (JsonArray.class.equals(pCls)) {
            return getRequestJsonArray(url, req);
        } else if (MultiPartRequest.class.equals(pCls)) {
            DiskFileItemFactory factory = (DiskFileItemFactory) req.getServletContext()
                    .getAttribute(DiskFileItemFactory.class.getSimpleName());
            return new MultiPartRequest(factory, req);
        } else if (HttpServletRequest.class.equals(pCls)) {
            return req;
        } else if (HttpServletResponse.class.equals(pCls)) {
            return res;
        }
        Constructor[] constructors = pCls.getDeclaredConstructors();
        for (Constructor ctr : constructors) {
            Class<?>[] types = ctr.getParameterTypes();
            if (1 == types.length) {
                if (HttpServletRequest.class.equals(types[0])) {
                    return ctr.newInstance(req);
                } else if (HttpServletResponse.class.equals(types[0])) {
                    return ctr.newInstance(res);
                }
            }
        }
        throw new IllegalArgumentException("Path service method requested parameter not yet implemented!");
    }

    private JsonObject getRequestJsonObject(String pathInfo, HttpServletRequest request) {
        try {
            JsonReader reader = Json.createReader(request.getInputStream());
            return reader.readObject();
        } catch (Exception ex) {
            if (logger.isLoggable(Level.FINE)) {
                ex.printStackTrace();
            } else {
                logger.severe(pathInfo + " \t Http Request JsonObject format error  : " + ex.getMessage());
            }
        }
        return Json.createObjectBuilder().build();
    }

    private JsonArray getRequestJsonArray(String pathInfo, HttpServletRequest request) {
        try {
            JsonReader reader = Json.createReader(request.getInputStream());
            return reader.readArray();
        } catch (Exception ex) {
            if (logger.isLoggable(Level.FINE)) {
                ex.printStackTrace();
            } else {
                logger.severe(pathInfo + " \t Http Request JsonArray format error  : " + ex.getMessage());
            }
        }
        return null;
    }

    private void sendMessage(HttpServletResponse response, TextMessage tmg) throws IOException {
        sendMessage(response, tmg.getStatus(), tmg.getMessage());
    }

    private void sendMessage(HttpServletResponse response, int status, String text) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        response.setContentLength(text.length());
        response.getWriter().write(text);
        response.getWriter().close();
    }

    private void send(HttpServletResponse response, Class<?> type, Object result) throws IOException {
        if (JsonStructure.class.equals(type) || JsonResult.class.equals(type)) {
            sendAsJson(response, type, result);
        } else if (TextMessage.class.equals(type)) {
            if (result == null) {
                sendMessage(response, SC_OK, "");
            } else {
                sendMessage(response, (TextMessage) result);
            }
        } else if (!Void.class.equals(type)) {
            sendMessage(response, SC_OK, result == null ? "" : result.toString());
        } else {
            sendMessage(response, SC_BAD_REQUEST, "Response type is not implemented : " + type);
            logger.fine("Response type is not implemented : " + type);
        }
    }

    private void sendAsJson(HttpServletResponse response, Class<?> type, Object result) throws IOException {
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            if (result == null) {
                if (JsonObject.class.equals(type)) {
                    response.setContentLength(2);
                    response.getWriter().write("{}");
                    response.getWriter().close();
                } else if (JsonArray.class.equals(type)) {
                    response.setContentLength(2);
                    response.getWriter().write("[]");
                    response.getWriter().close();
                } else {
                    response.getWriter().write("");
                    response.getWriter().close();
                }
            } else if (result instanceof JsonStructure) {
                JsonWriter writer = Json.createWriter(response.getWriter());
                writer.write((JsonStructure) result);
                writer.close();
            } else if (result instanceof JsonResult) {
                JsonResult json = (JsonResult) result;
                if (json.getStructure() != null) {
                    JsonWriter writer = Json.createWriter(response.getWriter());
                    writer.write(json.getStructure());
                    writer.close();
                } else {
                    sendMessage(response, json.getStatus(), json.getMessage());
                }
            } else if (result instanceof JsonValue) {
                JsonWriter writer = Json.createWriter(response.getWriter());
                writer.write((JsonValue) result);
                writer.close();
            } else {
                response.getWriter().write(result.toString());
                response.getWriter().close();
            }
        } catch (Exception ex) {
            if (logger.isLoggable(Level.FINE)) {
                ex.printStackTrace();
            } else {
                sendMessage(response, SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    private String getPathInfo(HttpServletRequest request) {
        String path = request.getPathInfo();
        path = path == null ? "" : path.trim().toLowerCase();
        if (path.equals("") || path.equals("/")) {
            return "";
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.lastIndexOf("/"));
        }
        if (!path.startsWith("/")) {
            path = "/".concat(path);
        }
        return path;
    }
}
