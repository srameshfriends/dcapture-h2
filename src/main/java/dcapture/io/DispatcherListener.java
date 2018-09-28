package dcapture.io;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;
import io.github.pustike.inject.bind.Binder;
import io.github.pustike.inject.bind.Module;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.io.FileCleaningTracker;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.ws.rs.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

public class DispatcherListener implements ServletContextListener {
    private static Logger logger = Logger.getLogger("dcapture.io");
    private DispatcherRegistry registry;

    public void setRegistry(DispatcherRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        context.setAttribute(ThreadPoolExecutor.class.getName(), threadPoolExecutor);
        Injector injector = Injectors.create((Module) this::bindInjector);
        context.setAttribute(Injector.class.getName(), injector);
        Map<String, Dispatcher> dispatcherMap = new HashMap<>();
        for (Class<?> pathClass : registry.getPathServiceList()) {
            addPathService(dispatcherMap, pathClass);
        }
        context.setAttribute("DispatcherMap", dispatcherMap);
        context.setAttribute(DiskFileItemFactory.class.getSimpleName(), getDiskFileItemFactory(sce.getServletContext()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Injector injector = (Injector) context.getAttribute(Injector.class.getName());
        registry.destroyed(injector);
        logger.severe("database stopped");
        Injectors.dispose(injector);
    }

    private DiskFileItemFactory getDiskFileItemFactory(ServletContext context) {
        File repository = (File) context.getAttribute("javax.servlet.context.tempdir");
        FileCleaningTracker cleaningTracker = FileCleanerCleanup.getFileCleaningTracker(context);
        DiskFileItemFactory factory = new DiskFileItemFactory(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, repository);
        factory.setFileCleaningTracker(cleaningTracker);
        return factory;
    }

    private void bindInjector(final Binder binder) {
        binder.setDefaultScope(Singleton.class);
        registry.inject(binder);
        for (Class<?> pathClass : registry.getPathServiceList()) {
            binder.bind(pathClass);
        }
    }

    private void addPathService(Map<String, Dispatcher> serviceMap, Class<?> typeClass) {
        Path classPath = typeClass.getAnnotation(Path.class);
        if (classPath == null) {
            return;
        }
        validatePathAnnotation(typeClass, classPath);
        List<Method> pathMethodList = getPathAnnotatedMethods(typeClass);
        if (pathMethodList.isEmpty()) {
            throw new IllegalArgumentException(typeClass + " >> At least one method annotated with @Path");
        }
        final String pathPrefix = classPath.value();
        String pathSuffix;
        for (Method method : pathMethodList) {
            validateMethodDeclarations(typeClass, method);
            Path methodPath = method.getAnnotation(Path.class);
            pathSuffix = methodPath.value();
            String path = pathPrefix + pathSuffix;
            String httpMethod = findHttpMethod(method);
            serviceMap.put(path.toLowerCase(), new Dispatcher(path, httpMethod, method));
            logger.severe("Path Service : " + path.toLowerCase());
        }
    }

    private String findHttpMethod(Method method) {
        if (method.isAnnotationPresent(GET.class)) {
            return HttpMethod.GET;
        } else if (method.isAnnotationPresent(POST.class)) {
            return HttpMethod.POST;
        } else if (method.isAnnotationPresent(PUT.class)) {
            return HttpMethod.PUT;
        } else if (method.isAnnotationPresent(DELETE.class)) {
            return HttpMethod.DELETE;
        } else if (method.isAnnotationPresent(PATCH.class)) {
            return HttpMethod.PATCH;
        } else if (method.isAnnotationPresent(HEAD.class)) {
            return HttpMethod.HEAD;
        } else if (method.isAnnotationPresent(OPTIONS.class)) {
            return HttpMethod.OPTIONS;
        }
        return HttpMethod.GET;
    }

    private void validatePathAnnotation(Class<?> typeClass, Path pathAnn) {
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
            if (method.isAnnotationPresent(Path.class)) {
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
        if (JsonResponse.class.equals(source) || HtmlResponse.class.equals(source)) {
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
