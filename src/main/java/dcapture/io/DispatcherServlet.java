package dcapture.io;

import dcapture.db.core.DataSet;
import dcapture.db.core.SqlContext;
import dcapture.db.core.SqlResult;
import dcapture.db.util.*;
import io.github.pustike.inject.Injector;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.log4j.Logger;

import javax.json.*;
import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 10)
public class DispatcherServlet extends SqlDispatcher {
    private static final Logger logger = Logger.getLogger(DispatcherServlet.class);
    private static final int SC_OK = 200;
    private static final int SC_BAD_REQUEST = 400;
    private static final DateTimeFormatter attachmentHourFormat = DateTimeFormatter.ofPattern("yyyy MM dd hh");
    private Map<String, Dispatcher> dispatcherMap;
    private Injector injector;
    private SqlContext sqlContext;
    private Localization locale;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        injector = (Injector) context.getAttribute(Injector.class.getName());
        sqlContext = injector.getInstance(SqlContext.class);
        locale = injector.getInstance(Localization.class);
        setSqlContext(sqlContext);
        HashMap<String, Dispatcher> map = (HashMap<String, Dispatcher>) context.getAttribute("DispatcherMap");
        dispatcherMap = Collections.unmodifiableMap(map);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        final String contentType = getContentType(request);
        final String httpMethod = getHttpMethod(request);
        final String pathInfo = getPathInfo(request);
        response.setContentType(contentType);
        if (!dispatcherMap.containsKey(pathInfo)) {
            sendMessage(response, SC_BAD_REQUEST, "Path Info : " + pathInfo + ", Content Type : "
                    + contentType + ", Http Method : " + httpMethod + " >> Service not recognized.");
            return;
        }
        Dispatcher dispatcher = dispatcherMap.get(pathInfo);
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
            Class<?>[] paramTypes = method.getParameterTypes();
            if (0 == paramTypes.length) {
                send(response, method.getReturnType(), method.invoke(serviceBean), pathInfo);
            } else if (1 == paramTypes.length) {
                Object instance = getMethodParameterInstance(request, response, paramTypes[0], contentType, pathInfo);
                send(response, method.getReturnType(), method.invoke(serviceBean, instance), pathInfo);
            } else if (2 == paramTypes.length) {
                Object firstParam = getMethodParameterInstance(request, response, paramTypes[0], contentType, pathInfo);
                Object secondParam = getMethodParameterInstance(request, response, paramTypes[1], contentType, pathInfo);
                send(response, method.getReturnType(), method.invoke(serviceBean, firstParam, secondParam), pathInfo);
            } else {
                sendMessage(response, SC_BAD_REQUEST, "Request or return class type not supported");
            }
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null) {
                message = ex.getCause() == null ? "Unknown Error" : ex.getCause().getMessage();
            }
            sendMessage(response, SC_BAD_REQUEST, message);
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
    }

    private String getContentType(HttpServletRequest request) {
        if (request.getContentType() != null) {
            String type = request.getContentType().toLowerCase();
            if (type.contains("json")) {
                return "application/json";
            } else if (type.contains("csv")) {
                return "text/csv";
            }
        }
        return "html/text";
    }

    private String getHttpMethod(HttpServletRequest request) {
        if (request.getMethod() != null) {
            String type = request.getMethod().toLowerCase();
            if (type.contains("post")) {
                return "POST";
            } else if (type.contains("get")) {
                return "GET";
            } else if (type.contains("put")) {
                return "PUT";
            } else if (type.contains("delete")) {
                return "DELETE";
            }
        }
        return "POST";
    }

    private DataSetRequest getDataSetRequest(HttpServletRequest req, String contentType) {
        try {
            if (contentType.contains("json")) {
                JsonReader reader = Json.createReader(req.getInputStream());
                return new DataSetRequest(req, sqlContext, reader.readValue());
            } else if (contentType.contains("csv")) {
                CSVParser parser = CSVParser.parse(req.getInputStream(), StandardCharsets.UTF_8,
                        CSVFormat.DEFAULT.withHeader());
                return new DataSetRequest(req, sqlContext, parser.getRecords(), parser.getHeaderMap());
            } else {
                BufferedReader reader = req.getReader();
                final char[] buffer = new char[4 * 1024];
                int len;
                final StringBuilder sb = new StringBuilder();
                while ((len = reader.read(buffer, 0, buffer.length)) != -1) {
                    sb.append(buffer, 0, len);
                }
                return new DataSetRequest(req, sqlContext, sb.toString());
            }
        } catch (Exception ex) {
            logger.debug("Path Info : " + req.getPathInfo() + ", Content Type :  " + req.getContentType()
                    + " >> request format error : " + ex.getMessage());
            throw new RuntimeException("Path Info : " + req.getPathInfo() + ", Content Type :  " + req.getContentType()
                    + " >> request format error : " + ex.getMessage());
        }
    }

    private Object getMethodParameterInstance(HttpServletRequest req, HttpServletResponse res, Class<?> pCls, String contentType, String pathInfo)
            throws Exception {
        if (DataSetRequest.class.equals(pCls)) {
            return getDataSetRequest(req, contentType);
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
        throw new IllegalArgumentException("Path Info : " + pathInfo + ", Content Type : " + contentType
                + ", Parameter Type :  " + pCls + ", Service request parameter invalid");
    }

    private void sendMessage(HttpServletResponse response, ServletResult tmg) throws IOException {
        if (tmg.getMessageCode() != null && locale != null) {
            sendMessage(response, tmg.getStatus(), locale.getMessage(tmg.getMessageCode(), tmg.getArguments()));
        } else {
            sendMessage(response, tmg.getStatus(), tmg.getMessage());
        }
    }

    private void sendMessage(HttpServletResponse response, int status, String text) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        if (text != null) {
            response.setContentLength(text.length());
            response.getWriter().write(text);
        } else {
            response.setContentLength(0);
            response.getWriter().write("");
        }
        response.getWriter().close();
    }

    private void send(HttpServletResponse response, Class<?> type, Object result, String pathInfo)
            throws IOException {
        if (result == null) {
            sendMessage(response, SC_OK, "");
        } else if (result instanceof DataSetResult) {
            send(response, (DataSetResult) result, pathInfo);
        } else if (result instanceof ServletResult) {
            sendMessage(response, (ServletResult) result);
        } else {
            if (!void.class.equals(type)) {
                sendMessage(response, SC_BAD_REQUEST, pathInfo + " >> Response type is not implemented : " + type);
                logger.warn(pathInfo + " >> Response type is not implemented : " + type);
            }
        }
    }

    private void send(HttpServletResponse hsr, DataSetResult result, String pathInfo) throws IOException {
        if (String.class.equals(result.getSourceType())) {
            sendMessage(hsr, result);
        } else {
            hsr.setContentType(result.getContentType());
            hsr.setCharacterEncoding("UTF-8");
            if (result.getAttachmentName() != null) {
                hsr.setHeader("Content-disposition", getAttachmentName(result));
            }
            if (result.getContentType().contains("json")) {
                JsonWriter jwr = Json.createWriter(hsr.getWriter());
                if (Paging.class.equals(result.getSourceType())) {
                    jwr.writeObject(getJsonObject(result.getPaging(), result.getTableName(), result.getColumns()));
                    jwr.close();
                } else if (DataSet.class.equals(result.getSourceType())) {
                    jwr.writeObject(getJsonObject(result.getDataSet(), result.getTableName(), result.getColumns()));
                    jwr.close();
                } else if (List.class.equals(result.getSourceType())) {
                    jwr.writeArray(getJsonArray(result.getDataSetList(), result.getTableName(), result.getColumns()));
                    jwr.close();
                } else if (JsonObject.class.equals(result.getSourceType())) {
                    jwr.writeObject(result.getJsonObject() == null ? Json.createObjectBuilder().build() : result.getJsonObject());
                    jwr.close();
                } else if (JsonArray.class.equals(result.getSourceType())) {
                    jwr.writeArray(result.getJsonArray() == null ? Json.createArrayBuilder().build() : result.getJsonArray());
                    jwr.close();
                } else if (SqlResult.class.equals(result.getSourceType())) {
                    jwr.writeArray(getJsonArray(result.getDataSetList(), result.getTableName(), result.getColumns()));
                    jwr.close();
                } else {
                    sendMessage(hsr, SC_BAD_REQUEST, "RESPONSE ERROR :  " + pathInfo);
                    logger.debug("RESPONSE ERROR (DataSetResult) :  " + pathInfo);
                }
            } else if (result.getContentType().contains("csv")) {
                if (SqlResult.class.equals(result.getSourceType())) {
                    writeCsv(hsr.getWriter(), result.getSqlResult(), result.getCsvHeader());
                    hsr.getWriter().close();
                } else if (Paging.class.equals(result.getSourceType()) || List.class.equals(result.getSourceType())) {
                    writeCsv(hsr.getWriter(), result.getDataSetList(), result.getColumns(), result.getCsvHeader());
                    hsr.getWriter().close();
                } else if (DataSet.class.equals(result.getSourceType())) {
                    writeCsv(hsr.getWriter(), Collections.singletonList(
                            result.getDataSet()), result.getColumns(), result.getCsvHeader());
                    hsr.getWriter().close();
                } else {
                    sendMessage(hsr, SC_BAD_REQUEST, "RESPONSE ERROR :  " + pathInfo);
                    logger.debug("RESPONSE ERROR (DataSetResult) :  " + pathInfo);
                }
            } else {
                sendMessage(hsr, result);
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

    private String getAttachmentName(DataSetResult result) {
        String fileFormat = ".csv", timeText = "-" + attachmentHourFormat.format(LocalDateTime.now());
        if (result.getContentType().contains("json")) {
            fileFormat = ".json";
        }
        timeText = timeText.replaceAll("\\s", "");
        return "attachment; filename=" + result.getAttachmentName() + timeText + fileFormat;
    }
}
