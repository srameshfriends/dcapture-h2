package dcapture.servlet.context;

import dcapture.api.io.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.lang.reflect.Method;
import java.util.*;

public class DispatcherMap implements Comparator<Dispatcher> {
    private final Map<String, Dispatcher> dispatcherMap;

    DispatcherMap() {
        dispatcherMap = new HashMap<>();
    }

    boolean isHttpService(String path) {
        return dispatcherMap.containsKey(path);
    }

    Dispatcher getDispatcher(String path) {
        return dispatcherMap.get(path);
    }

    @Override
    public final int compare(Dispatcher o1, Dispatcher o2) {
        return o1.getPath().compareTo(o2.getPath());
    }

    String addHttpService(Class<?> typeClass) {
        HttpService httpService = typeClass.getAnnotation(HttpService.class);
        if (httpService == null) {
            return typeClass + " >> Class not annotated with @HttpService";
        }
        String errorMessage = isValidPath(typeClass, httpService.value());
        if (errorMessage != null) {
            return errorMessage;
        }
        List<Method> pathMethodList = getPathAnnotatedMethods(typeClass);
        if (pathMethodList.isEmpty()) {
            return "At least one method annotated with @POST, @GET or @DELETE >> " + typeClass;
        }
        final String servicePrefix = httpService.value();
        final boolean serviceSecured = httpService.secured();
        for (Method method : pathMethodList) {
            errorMessage = isValidMethodDeclaration(typeClass, method);
            if (errorMessage != null) {
                return errorMessage;
            }
            boolean methodSecured;
            String methodType, serviceSuffix;
            if (method.isAnnotationPresent(POST.class)) {
                POST post = method.getAnnotation(POST.class);
                methodType = "POST";
                serviceSuffix = post.value();
                methodSecured = post.secured();
            } else if (method.isAnnotationPresent(GET.class)) {
                GET get = method.getAnnotation(GET.class);
                methodType = "GET";
                serviceSuffix = get.value();
                methodSecured = get.secured();
            } else if (method.isAnnotationPresent(DELETE.class)) {
                DELETE delete = method.getAnnotation(DELETE.class);
                methodType = "DELETE";
                serviceSuffix = delete.value();
                methodSecured = delete.secured();
            } else {
                return typeClass + ", " + method.getName() + " http service method is not valid : " + httpService;
            }
            if (serviceSecured && !methodSecured) {
                methodSecured = true;
            }
            errorMessage = isValidPath(typeClass, method, serviceSuffix);
            if (errorMessage != null) {
                return errorMessage;
            }
            String path = servicePrefix + serviceSuffix;
            dispatcherMap.put(path.toLowerCase(), new Dispatcher(path, methodType, method, methodSecured));
        }
        return null;
    }

    private String isValidPath(Class<?> typeClass, String path) {
        if (!path.startsWith("/")) {
            return typeClass + " >> Path should be started with '/' char";
        }
        if (path.endsWith("/")) {
            return typeClass + " >> Path should not be end with '/' char";
        }
        return null;
    }

    private String isValidPath(Class<?> typeClass, Method method, String path) {
        if (!path.startsWith("/")) {
            return typeClass + "." + method.getName() + " >> Path should be started with '/' char";
        }
        if (path.endsWith("/")) {
            return typeClass + "." + method.getName() + " >> Path should not be end with '/' char";
        }
        return null;
    }

    private List<Method> getPathAnnotatedMethods(final Class<?> pathClass) {
        Method[] methodArray = pathClass.getDeclaredMethods();
        List<Method> methodList = new ArrayList<>();
        for (Method method : methodArray) {
            if (method.isAnnotationPresent(POST.class) || method.isAnnotationPresent(GET.class)
                    || method.isAnnotationPresent(DELETE.class)) {
                methodList.add(method);
            }
        }
        return methodList;
    }

    private String isValidMethodDeclaration(Class<?> cls, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        if (2 < paramTypes.length) {
            return cls + " >> " + method + " >> more then 2 parameters not supported";
        }
        if (0 == paramTypes.length && void.class.equals(returnType)) {
            return cls + " >> " + method + " >> is not valid return type";
        }
        if (1 == paramTypes.length) {
            if (void.class.equals(returnType) && !isValidResponseParam(paramTypes[0])) {
                return cls + " >> " + method + " >> not a valid method response not processed";
            }
        }
        if (2 == paramTypes.length) {
            boolean isFirstResponse = isValidResponseParam(paramTypes[0]);
            boolean isSecondResponse = isValidResponseParam(paramTypes[1]);
            if (void.class.equals(returnType)) {
                if (!isFirstResponse && !isSecondResponse) {
                    return cls + " >> " + method + " >> response not processed";
                }
            } else {
                if (isFirstResponse || isSecondResponse) {
                    return cls + " >> " + method + " >> response logic error";
                }
            }
        }
        return null;
    }

    private boolean isValidResponseParam(Class<?> source) {
        if (ServletResult.class.equals(source) || JsonResult.class.equals(source)
                || CsvResult.class.equals(source) || HttpServletResponse.class.equals(source)) {
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("\n **** Registered Http Services **** ");
        List<String> serviceList = new ArrayList<>(dispatcherMap.keySet());
        serviceList.sort(String::compareTo);
        for (String service : serviceList) {
            builder.append("\n").append(dispatcherMap.get(service));
        }
        return builder.toString();
    }
}
