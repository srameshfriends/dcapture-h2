package dcapture.servlet.context;

import dcapture.api.io.*;
import dcapture.api.postgres.PgDatabase;
import dcapture.api.sql.SqlContext;
import dcapture.api.sql.SqlDatabase;
import dcapture.api.sql.SqlFactory;
import dcapture.api.support.MessageException;
import dcapture.api.support.Messages;
import dcapture.api.support.ObjectUtils;
import dcapture.api.support.WebResource;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;
import io.github.pustike.inject.bind.Binder;
import io.github.pustike.inject.bind.Module;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.*;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 5, maxRequestSize = 1024 * 1024 * 10)
public class DispatcherServlet extends GenericServlet {
    private static final Logger logger = Logger.getLogger(DispatcherServlet.class);
    private static final int SC_BAD_REQUEST = 400;
    private final Set<String> validContentTypes, validMethods;
    private DispatcherMap dispatcherMap;
    private Injector injector;
    private SqlContext sqlContext;
    private Messages messages;

    public DispatcherServlet() {
        Set<String> hashSet = new HashSet<>();
        Collections.addAll(hashSet, "multipart/form-data",
                "text/html", "text/plain", "text/csv", "application/json", "application/x-www-form-urlencoded");
        validContentTypes = Collections.unmodifiableSet(hashSet);
        Set<String> hashSet2 = new HashSet<>();
        Collections.addAll(hashSet2, "GET", "POST", "DELETE", "HEAD", "PUT", "CONNECT", "TRACE", "OPTIONS");
        validMethods = Collections.unmodifiableSet(hashSet2);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        injector = Injectors.create((Module) binder -> configureBinder(config.getServletContext(), binder));
        dispatcherMap = injector.getInstance(DispatcherMap.class);
        sqlContext = injector.getInstance(SqlContext.class);
        messages = injector.getInstance(Messages.class);
        config.getServletContext().setAttribute(Injector.class.getName(), injector);
        if (logger.isDebugEnabled()) {
            WebResource resource = injector.getInstance(WebResource.class);
            logger.info(resource.toString());
            logger.info(dispatcherMap.toString());
        }
    }

    @Override
    public void service(ServletRequest req, ServletResponse resp) {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        final String pathInfo = getValidPathInfo(request.getPathInfo());
        final String method = getValidMethod(request.getMethod());
        final String contentType = getValidContentType(request.getContentType());
        if (!dispatcherMap.isHttpService(pathInfo)) {
            ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.path.error", pathInfo));
            return;
        }
        Dispatcher dispatcher = dispatcherMap.getDispatcher(pathInfo);
        if (!method.equals(dispatcher.getHttpMethod())) {
            Object[] args = new String[]{pathInfo, method, dispatcher.getHttpMethod()};
            ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.httpMethod.error", args));
            return;
        }
        if (dispatcher.isSecured()) {
            HttpSession session = request.getSession(false);
            if (session == null || session.getId() == null) {
                ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.unauthorized.error", pathInfo));
                return;
            }
        }
        ContentReader contentReader;
        try {
            contentReader = new ContentReader(request, pathInfo, method, contentType);
        } catch (Exception ex) {
            contentReader = null;
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
                ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.content.error", pathInfo, ex.getMessage()));
            }
        }
        if (contentReader == null) {
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
                Object parameter = contentReader.getMethodParameter(sqlContext, paramTypes[0], response);
                result = serviceMethod.invoke(serviceBean, parameter);
            } else if (0 == paramTypes.length) {
                result = serviceMethod.invoke(serviceBean);
            } else if (2 == paramTypes.length) {
                Object paramFirst = contentReader.getMethodParameter(sqlContext, paramTypes[0], response);
                Object paramSecond = contentReader.getMethodParameter(sqlContext, paramTypes[1], response);
                result = serviceMethod.invoke(serviceBean, paramFirst, paramSecond);
            } else if (!void.class.equals(dispatcher.getMethod().getReturnType())) {
                ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.response.type.error", pathInfo));
                return;
            } else {
                logger.error("Unknown http service result type is received : " + dispatcher.toString());
            }
            if (result instanceof JsonResult) {
                new JsonHandler(response, pathInfo, messages).setSqlContext(sqlContext).send((JsonResult) result);
            } else if (result instanceof CsvResult) {
                new CsvHandler(response, pathInfo, messages).sendAsCsv((CsvResult) result);
            } else if (result instanceof ServletResult) {
                ServletResult svtResult = (ServletResult) result;
                if (svtResult.getMessageCode() != null) {
                    ResponseHandler.send(response, svtResult.getStatus(), getMessage(svtResult.getMessageCode(), svtResult.getArguments()));
                } else {
                    ResponseHandler.send(response, svtResult.getStatus(), svtResult.getMessage());
                }
            } else {
                ResponseHandler.send(response, SC_BAD_REQUEST, getMessage("application.method.parameter.error", pathInfo));
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

    private void configureBinder(ServletContext context, Binder binder) {
        WebResource webResource = WebResource.get(context);
        DispatcherMap dispatcherMap = new DispatcherMap();
        List<Class<?>> httpServiceList = new ArrayList<>();
        List<HttpModule> httpModules = getHttpModules(context.getInitParameter("http-modules"));
        logger.info("Http modules are configured (" + httpModules.size() + ")");
        for (HttpModule httpModule : httpModules) {
            logger.info(httpModule);
            List<Class<?>> httpServices = httpModule.getHttpServiceList();
            if (httpServices != null) {
                httpServiceList.addAll(httpServices);
            }
        }
        SqlDatabase sqlDatabase = getSqlDatabase(context, webResource);
        if (sqlDatabase == null) {
            logger.error("***** database error *****");
            return;
        }
        sqlContext = sqlDatabase.getContext();
        binder.bind(SqlContext.class).toInstance(sqlContext);
        binder.bind(SqlDatabase.class).toInstance(sqlDatabase);
        binder.bind(WebResource.class).toInstance(webResource);
        binder.bind(Messages.class).toInstance(getMessages(webResource));
        binder.bind(DispatcherMap.class).toInstance(dispatcherMap);
        for (Class<?> httpClass : httpServiceList) {
            binder.bind(httpClass);
            String errorMessage = dispatcherMap.addHttpService(httpClass);
            if (errorMessage != null) {
                logger.info("HTTP SERVICE ERROR : " + errorMessage);
            }
        }
    }

    private List<HttpModule> getHttpModules(String services) {
        if (services == null) {
            logger.error("Init parameter http modules not configured");
            return new ArrayList<>();
        }
        services = services.replaceAll("[\\r\\n\\t]+", "");
        logger.error("Init parameter http-modules is \n" + services);
        String[] arguments = services.split(",");
        if (0 == arguments.length) {
            logger.error("Init parameter http modules classes not configured");
            return new ArrayList<>();
        }
        List<HttpModule> httpModules = new ArrayList<>();
        for (String arg : arguments) {
            HttpModule module = getHttpModule(arg);
            if (module != null) {
                httpModules.add(module);
            }
        }
        return httpModules;
    }

    private Messages getMessages(WebResource resource) {
        Messages messages = new Messages();
        messages.setLanguage(resource.getSetting("language"));
        messages.loadPropertiesMap(resource.getMessages(), true);
        return messages;
    }

    private HttpModule getHttpModule(String name) {
        if (name != null) {
            try {
                Class<?> httpModuleClass = Class.forName(name);
                return (HttpModule) httpModuleClass.newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                logger.error(name + " >> HttpModule(s) would not be created : " + ex.getMessage());
                if (logger.isDebugEnabled()) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }

    private SqlDatabase getSqlDatabase(ServletContext context, WebResource resource) {
        final String url = resource.getSetting("database.url");
        if (url == null) {
            logger.error("Database url should not be null");
            return null;
        }
        Properties properties = new Properties();
        properties.setProperty("url", url);
        properties.setProperty("user", ObjectUtils.decodeBase64(resource.getSetting("database.user")));
        properties.setProperty("password", ObjectUtils.decodeBase64(resource.getSetting("database.password")));
        Class<?> driver = null;
        try {
            if (context.getClassLoader() == null) {
                driver = SqlFactory.loadJdbcDriver(properties, Thread.currentThread().getContextClassLoader());
            } else {
                driver = SqlFactory.loadJdbcDriver(properties, context.getClassLoader());
            }
        } catch (ClassNotFoundException ex) {
            logger.error(ex.getMessage());
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
        if (driver == null) {
            logger.error("Database driver not found at servlet context class loader");
            return null;
        }
        SqlContext sqlContext = SqlFactory.getSqlContext(resource.getSqlResource());
        if (url.toLowerCase().contains("postgres")) {
            return new PgDatabase(sqlContext, SqlFactory.getDataSource(properties));
        }
        return null;
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

    private String getMessage(String code, Object... args) {
        return messages.getMessage(code, args);
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
     * Http servlet request pathInfo null safe converted to lower case char
     */
    private String getValidPathInfo(String pathInfo) {
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
}
