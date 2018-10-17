package dcapture.io;

import io.github.pustike.inject.Injector;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import javax.json.*;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

public class DispatcherServlet extends GenericServlet {
    private static Logger logger = Logger.getLogger("dcapture.io");
    private final Set<String> acceptedContentTypeSet;
    private Map<String, Dispatcher> dispatcherMap;
    private Injector injector;

    public DispatcherServlet() {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, Arrays.toString(ContentType.values()));
        acceptedContentTypeSet = Collections.unmodifiableSet(set);
    }

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
        final ContentType cType = getContentType(request);
        final String pathInfo = getPathInfo(request);
        if (!dispatcherMap.containsKey(pathInfo)) {
            error(cType, "Service not implemented : " + pathInfo, response);
            return;
        }
        Dispatcher dispatcher = dispatcherMap.get(pathInfo);
        /*final String httpMethod = getHttpMethod(request);
        if (!httpMethod.equals(dispatcher.getHttpMethod())) {
            error(cType, "Expected service method is " + dispatcher.getHttpMethod() +
                    ", Requested method is : " + httpMethod, response);
            return;
        }*/
        try {
            Method method = dispatcher.getMethod();
            Class<?> beanClass = method.getDeclaringClass();
            method.setAccessible(true);
            Object serviceBean = injector.getInstance(beanClass);
            Class<?>[] parameters = method.getParameterTypes();
            if (0 == parameters.length) {
                Object result = method.invoke(serviceBean);
                sendResponse(response, cType, result);
            } else if (1 == parameters.length) {
                Object instance = getMethodParameterInstance(pathInfo, parameters[0], request, response);
                if (instance instanceof HttpServletResponse) {
                    method.invoke(serviceBean, instance);
                } else {
                    sendResponse(response, cType, method.invoke(serviceBean, instance));
                }
            } else if (2 == parameters.length) {
                Object firstParameter = getMethodParameterInstance(pathInfo, parameters[0], request, response);
                Object secondParameter = getMethodParameterInstance(pathInfo, parameters[1], request, response);
                method.invoke(serviceBean, firstParameter, secondParameter);
            } else {
                error(cType, "Request or return class type not supported", response);
            }
        } catch (LocaleException ex) {
            Localization localization = injector.getInstance(Localization.class);
            String msg = localization.get(ex.getLanguage(), ex.getMessage());
            error(cType, msg, response);
            ex.printStackTrace();
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null) {
                message = ex.getCause() == null ? "Unknown Error" : ex.getCause().getMessage();
            }
            error(cType, message, response);
            ex.printStackTrace();
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
        } else if (JsonResponse.class.equals(pCls)) {
            return new JsonResponse(res);
        } else if (HtmlRequest.class.equals(pCls)) {
            return new HtmlRequest(req);
        } else if (HtmlResponse.class.equals(pCls)) {
            return new HtmlResponse(res);
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

    private void sendResponse(HttpServletResponse response, ContentType cType, Object result) throws IOException {
        if (ContentType.Json.equals(cType)) {
            sendAsJson(cType, result, response);
        } else {
            sendAsText(cType, result, response);
        }
    }

    private JsonObject getRequestJsonObject(String pathInfo, HttpServletRequest request) {
        try {
            JsonReader reader = Json.createReader(request.getInputStream());
            return reader.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Json.createObjectBuilder().build();
    }

    private JsonArray getRequestJsonArray(String pathInfo, HttpServletRequest request) {
        try {
            JsonReader reader = Json.createReader(request.getInputStream());
            return reader.readArray();
        } catch (Exception ex) {
            logger.severe(pathInfo + " \t Http Request JsonArray format error  : " + ex.getMessage());
        }
        return null;
    }

    private void errorAsJson(String description, HttpServletResponse response) throws IOException {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("title", "error");
        builder.add("description", description);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        writer.print(builder.build());
        response.getWriter().close();
    }

    private void error(ContentType cType, String text, HttpServletResponse response) throws IOException {
        if (ContentType.Json.equals(cType)) {
            errorAsJson(text, response);
        } else {
            response.setContentType(cType.toString());
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(text == null ? "Unknown Error" : text);
            response.getWriter().close();
        }
    }

    private void sendAsText(ContentType cType, Object result, HttpServletResponse response) throws IOException {
        try {
            response.setContentType(cType.toString());
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(result == null ? "" : result.toString());
            response.getWriter().close();
        } catch (Exception ex) {
            error(ContentType.Html, ex.getMessage(), response);
            ex.printStackTrace();
        }
    }

    private void sendAsJson(ContentType cType, Object result, HttpServletResponse response) throws IOException {
        try {
            response.setContentType(cType.toString());
            response.setCharacterEncoding("UTF-8");
            if (result == null) {
                response.setContentLength(0);
                response.getWriter().close();
            } else if (result instanceof JsonObject) {
                JsonWriter writer = Json.createWriter(response.getWriter());
                writer.write((JsonObject) result);
            } else if (result instanceof JsonArray) {
                JsonWriter writer = Json.createWriter(response.getWriter());
                writer.write((JsonArray) result);
            } else if (result instanceof JsonValue) {
                JsonWriter writer = Json.createWriter(response.getWriter());
                writer.write((JsonValue) result);
            } else {
                response.getWriter().write(result.toString());
                response.getWriter().close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            errorAsJson(ex.getMessage(), response);
        }
    }

    private ContentType getContentType(HttpServletRequest request) {
        String type = request.getContentType();
        type = type == null ? ContentType.Html.toString() : type.trim().toLowerCase();
        return acceptedContentTypeSet.contains(type) ? ContentType.valueOf(type) : ContentType.Html;
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
