package dcapture.io;

import dcapture.db.util.DataSetResult;
import dcapture.db.util.ServletResult;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;
import io.github.pustike.inject.bind.Binder;
import io.github.pustike.inject.bind.Module;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class DispatcherListener implements ServletContextListener, Module, Comparator<Dispatcher> {
    private static Logger logger = Logger.getLogger(DispatcherListener.class);
    private final Set<String> httpMethodSet;

    public DispatcherListener() {
        httpMethodSet = new HashSet<>();
        Collections.addAll(httpMethodSet, "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
    }

    @Override
    public final void configure(Binder binder) {
        configureBinder(binder);
        for (Class<?> pathClass : getHttpPathList()) {
            binder.bind(pathClass);
        }
    }

    @Override
    public final void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        context.setAttribute(ThreadPoolExecutor.class.getName(), Executors.newCachedThreadPool());
        context.setAttribute(Injector.class.getName(), Injectors.create(DispatcherListener.this));
        Map<String, Dispatcher> dispatcherMap = new HashMap<>();
        for (Class<?> pathClass : getHttpPathList()) {
            addPathService(dispatcherMap, pathClass);
        }
        context.setAttribute("DispatcherMap", dispatcherMap);
        StringBuilder builder = new StringBuilder(" --- PATH SERVICE --- ");
        List<Dispatcher> values = new ArrayList<>(dispatcherMap.values());
        values.sort(this);
        values.forEach(dis -> builder.append("\n").append(dis.getHttpMethod()).append(" : ")
                .append(dis.getPath().toLowerCase()));
        logger.debug(builder.toString());
        initialized(sce.getServletContext());
    }

    @Override
    public final void contextDestroyed(ServletContextEvent sce) {
        destroyed(sce.getServletContext());
    }

    protected void configureBinder(Binder binder) {
    }

    protected List<Class<?>> getHttpPathList() {
        return new ArrayList<>();
    }

    protected void initialized(ServletContext context) {
    }

    protected void destroyed(ServletContext context) {
    }

    @Override
    public final int compare(Dispatcher o1, Dispatcher o2) {
        return o1.getPath().compareTo(o2.getPath());
    }

    private void addPathService(Map<String, Dispatcher> serviceMap, Class<?> typeClass) {
        HttpPath classPath = typeClass.getAnnotation(HttpPath.class);
        if (classPath == null) {
            return;
        }
        validatePathAnnotation(typeClass, classPath);
        List<Method> pathMethodList = getPathAnnotatedMethods(typeClass);

        if (pathMethodList.isEmpty()) {
            throw new IllegalArgumentException(typeClass + " >> At least one method annotated with @Path");
        }
        final String pathPrefix = classPath.value();
        for (Method method : pathMethodList) {
            validateMethodDeclarations(typeClass, method);
            HttpPath httpPath = method.getAnnotation(HttpPath.class);
            HttpMethod httpMethod = method.getAnnotation(HttpMethod.class);
            String methodName = "POST";
            if (httpMethod != null) {
                if (!httpMethodSet.contains(httpMethod.value().toUpperCase())) {
                    throw new IllegalArgumentException(
                            typeClass + " >> " + method.getName() + " annotated http method is not valid : " + httpMethod.value());
                }
                methodName = httpMethod.value().toUpperCase();
            }
            boolean secured = httpPath.secured();
            if (!secured) {
                secured = httpPath.secured();
            }
            String path = pathPrefix + httpPath.value();
            serviceMap.put(path.toLowerCase(), new Dispatcher(path, methodName, method, secured));
        }
    }

    private void validatePathAnnotation(Class<?> typeClass, HttpPath pathAnn) {
        if (!pathAnn.value().startsWith("/")) {
            throw new IllegalArgumentException(typeClass + " >> Path should be started with '/' char");
        }
        if (pathAnn.value().endsWith("/")) {
            throw new IllegalArgumentException(typeClass + " >> Path should not be end with '/' char");
        }
    }

    private List<Method> getPathAnnotatedMethods(final Class<?> pathClass) {
        Method[] methodArray = pathClass.getDeclaredMethods();
        List<Method> methodList = new ArrayList<>();
        for (Method method : methodArray) {
            if (method.isAnnotationPresent(HttpPath.class)) {
                methodList.add(method);
            }
        }
        return methodList;
    }

    private void validateMethodDeclarations(Class<?> cls, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        if (2 < paramTypes.length) {
            throw new IllegalArgumentException(cls + " >> " + method + " >> more then 2 parameters not supported");
        }
        if (0 == paramTypes.length && void.class.equals(returnType)) {
            throw new IllegalArgumentException(cls + " >> " + method + " >> is not valid return type");
        }
        if (1 == paramTypes.length) {
            if (void.class.equals(returnType) && !isValidResponseParam(paramTypes[0])) {
                throw new IllegalArgumentException(cls + " >> " + method + " >> not a valid method response not processed");
            }
        }
        if (2 == paramTypes.length) {
            boolean isFirstResponse = isValidResponseParam(paramTypes[0]);
            boolean isSecondResponse = isValidResponseParam(paramTypes[1]);
            if (void.class.equals(returnType)) {
                if (!isFirstResponse && !isSecondResponse) {
                    throw new IllegalArgumentException(cls + " >> " + method + " >> response not processed");
                }
            } else {
                if (isFirstResponse || isSecondResponse) {
                    throw new IllegalArgumentException(cls + " >> " + method + " >> response logic error");
                }
            }
        }
    }

    private boolean isValidResponseParam(Class<?> source) {
        if (ServletResult.class.equals(source) || DataSetResult.class.equals(source)
                || HttpServletResponse.class.equals(source)) {
            return true;
        }
        Class superclass = source.getSuperclass();
        if (HttpServletResponse.class.equals(superclass) || HttpServletResponseWrapper.class.equals(superclass)) {
            return true;
        }
        while (superclass != null) {
            source = superclass;
            superclass = source.getSuperclass();
            if (HttpServletResponse.class.equals(superclass) || HttpServletResponseWrapper.class.equals(superclass)) {
                return true;
            }
        }
        return false;
    }
}
