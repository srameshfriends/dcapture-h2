package dcapture.servlet.context;

import dcapture.api.io.*;
import dcapture.api.sql.SqlContext;
import dcapture.api.support.ContextResource;
import dcapture.api.support.MessageException;
import dcapture.api.support.Messages;
import io.github.pustike.inject.Injector;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 10)
public class DispatcherServlet extends GenericServlet {
    private static final Logger logger = Logger.getLogger(DispatcherServlet.class);
    private static final int SC_BAD_REQUEST = 400, SC_UNAUTHORIZED = 401;
    private DispatcherMap dispatcherMap;
    private Injector injector;
    private SqlContext sqlContext;
    private Messages messages;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        injector = (Injector)config.getServletContext().getAttribute(Injector.class.getName());
        dispatcherMap = injector.getInstance(DispatcherMap.class);
        sqlContext = injector.getInstance(SqlContext.class);
        messages = injector.getInstance(Messages.class);
        if (logger.isDebugEnabled()) {
            ContextResource resource = injector.getInstance(ContextResource.class);
            logger.info(resource.toString());
            logger.info(dispatcherMap.toString());
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse resp) {
        RequestInfo info = (RequestInfo) req.getAttribute(RequestInfo.class.getName());
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        Dispatcher dispatcher = dispatcherMap.getDispatcher(info.getPath());
        if (dispatcher == null) {
            ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.path.error", info.getPath()));
            return;
        }
        if (!info.getMethod().equals(dispatcher.getHttpMethod())) {
            Object[] args = new String[]{info.getPath(), info.getMethod(), dispatcher.getHttpMethod()};
            ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.httpMethod.error", args));
            return;
        }
        if (dispatcher.isSecured()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getId() == null) {
                ResponseHandler.send(response, SC_UNAUTHORIZED, getMessage("application.unauthorized.error", info.getPath()));
                return;
            }
        }
        RequestReader reader;
        try {
            reader = new RequestReader(request, dispatcher.getPath());
        } catch (Exception ex) {
            reader = null;
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
            if (ex instanceof MessageException) {
                MessageException msgEx = (MessageException) ex;
                if (msgEx.getErrorCode() == null) {
                    ResponseHandler.send(response, SC_BAD_REQUEST, msgEx.getMessage());
                } else {
                    ResponseHandler.send(response, SC_BAD_REQUEST, getMessage(msgEx.getErrorCode(), msgEx.getArguments()));
                }
            } else {
                ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.content.error", info.getPath(), ex.getMessage()));
            }
        }
        if (reader == null) {
            return;
        }
        try {
            Method serviceMethod = dispatcher.getMethod();
            Class<?> beanClass = serviceMethod.getDeclaringClass();
            serviceMethod.setAccessible(true);
            Object serviceBean = injector.getInstance(beanClass);
            Class<?>[] paramTypes = serviceMethod.getParameterTypes();
            Object result = null;
            if (1 == paramTypes.length) {
                Object parameter = reader.getMethodParameter(sqlContext, paramTypes[0], response);
                result = serviceMethod.invoke(serviceBean, parameter);
            } else if (0 == paramTypes.length) {
                result = serviceMethod.invoke(serviceBean);
            } else if (2 == paramTypes.length) {
                Object paramFirst = reader.getMethodParameter(sqlContext, paramTypes[0], response);
                Object paramSecond = reader.getMethodParameter(sqlContext, paramTypes[1], response);
                result = serviceMethod.invoke(serviceBean, paramFirst, paramSecond);
            } else if (dispatcher.isPattern() && 3 == paramTypes.length) {
                Object paramFirst = reader.getMethodParameter(sqlContext, paramTypes[0], response);
                Object paramSecond = reader.getMethodParameter(sqlContext, paramTypes[1], response);
                Object paramThree = reader.getMethodParameter(sqlContext, paramTypes[2], response);
                result = serviceMethod.invoke(serviceBean, paramFirst, paramSecond, paramThree);
            } else if (!void.class.equals(dispatcher.getMethod().getReturnType())) {
                ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.response.type.error", info.getPath()));
                return;
            } else {
                logger.error("Unknown http service result type is received : " + dispatcher.toString());
            }
            if (result instanceof JsonResult) {
                new JsonHandler(response, info.getPath(), messages).setSqlContext(sqlContext).send((JsonResult) result);
            } else if (result instanceof CsvResult) {
                new CsvHandler(response, info.getPath(), messages).sendAsCsv((CsvResult) result);
            } else if (result instanceof ServletResult) {
                ServletResult svtResult = (ServletResult) result;
                if (svtResult.getMessageCode() != null) {
                    ResponseHandler.send(response, svtResult.getStatus(), getMessage(svtResult.getMessageCode(), svtResult.getArguments()));
                } else {
                    ResponseHandler.send(response, svtResult.getStatus(), svtResult.getMessage());
                }
            } else {
                if (!response.isCommitted()) {
                    if (result == null) {
                        ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.method.parameter.error", info.getPath()));
                    } else if (!void.class.equals(result.getClass())) {
                        ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.method.parameter.error", info.getPath()));
                    }
                }
            }
        } catch (Exception ex) {
            String msg = getRootCause(ex);
            ResponseHandler.send(response, SC_BAD_REQUEST, msg);
            logger.error(msg);
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
    }

    private String getMessage(String code, Object... args) {
        return messages.getMessage(code, args);
    }

    private String getRootCause(Throwable throwable) {
        if (throwable.getCause() != null) {
            return getRootCause(throwable.getCause());
        } else {
            if (throwable instanceof MessageException) {
                String messageCode = ((MessageException) throwable).getErrorCode();
                if (messageCode != null) {
                    return getMessage(messageCode, ((MessageException) throwable).getArguments());
                }
            }
            return throwable.getMessage();
        }
    }

    private static class RequestReader {
        private final HttpServletRequest request;
        private final String pathInfo, pattern;
        private Map<String, String[]> parameters;
        private JsonObject jsonObject;
        private JsonArray jsonArray;
        private List<CSVRecord> csvRecords;
        private Map<String, Integer> csvHeaders;
        private String text;

        RequestReader(HttpServletRequest request, String pattern)
                throws Exception {
            RequestInfo info = (RequestInfo)request.getAttribute(RequestInfo.class.getName());
            this.request = request;
            this.pathInfo = info.getPath();
            this.pattern = pattern;
            if ("GET".equals(info.getMethod())) {
                parameters = request.getParameterMap();
            } else if ("POST".equals(info.getMethod()) || "DELETE".equals(info.getMethod())) {
                if (info.getContentType().contains("json")) {
                    JsonStructure json = Json.createReader(request.getInputStream()).read();
                    if (json instanceof JsonObject) {
                        jsonObject = (JsonObject) json;
                    } else if (json instanceof JsonArray) {
                        jsonArray = (JsonArray) json;
                    } else {
                        throw new MessageException("application.json.error", new Object[]{pathInfo});
                    }
                } else if (info.getContentType().contains("csv")) {
                    CSVParser parser = CSVParser.parse(request.getInputStream(), StandardCharsets.UTF_8,
                            CSVFormat.DEFAULT.withHeader());
                    csvRecords = parser.getRecords();
                    csvHeaders = parser.getHeaderMap();
                } else if ("application/x-www-form-urlencoded".equals(info.getContentType())) {
                    parameters = request.getParameterMap();
                } else if (info.getContentType().contains("text")) {
                    BufferedReader reader = request.getReader();
                    final char[] buffer = new char[4 * 1024];
                    int len;
                    final StringBuilder builder = new StringBuilder();
                    while ((len = reader.read(buffer, 0, buffer.length)) != -1) {
                        builder.append(buffer, 0, len);
                    }
                    text = builder.toString();
                }
            } else {
                throw new MessageException("application.httpMethod.error", new Object[]{pathInfo});
            }
        }

        Object getMethodParameter(SqlContext sqlContext, Class<?> pCls, HttpServletResponse response) throws MessageException {
            if (JsonRequest.class.equals(pCls)) {
                if (jsonObject != null) {
                    return new JsonRequest(request, sqlContext, jsonObject);
                } else if (jsonArray != null) {
                    return new JsonRequest(request, sqlContext, jsonArray);
                } else {
                    throw new MessageException("application.json.type.error", new Object[]{pathInfo});
                }
            } else if (CsvRequest.class.equals(pCls)) {
                return new CsvRequest(request, sqlContext, csvRecords, csvHeaders);
            } else if (FormRequest.class.equals(pCls)) {
                return new FormRequest(request, sqlContext, parameters);
            } else if (HtmlRequest.class.equals(pCls)) {
                return new HtmlRequest(request, text);
            } else if (HttpServletResponse.class.equals(pCls) || HttpServletResponseWrapper.class.equals(pCls)) {
                return response;
            } else if (HttpServletRequest.class.equals(pCls) || HttpServletRequestWrapper.class.equals(pCls)) {
                return request;
            } else if(pattern != null && String.class.equals(pCls)) {
                String info = pathInfo.replace(pattern, "");
                return info.startsWith("/") ? info.substring(1) : info;
            }
            throw new MessageException("application.parameter.error", new Object[]{pathInfo});
        }
    }
}
