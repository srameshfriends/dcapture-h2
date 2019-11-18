package dcapture.servlet.context;

import dcapture.api.io.*;
import dcapture.api.support.MessageException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.lang.reflect.Method;
import java.util.*;

public class DispatcherMap implements Comparator<Dispatcher> {
    private final Map<String, Dispatcher> dispatcherMap;
    private final Map<String, Dispatcher> patternDispatcherMap;

    private DispatcherMap(Map<String, Dispatcher> disMap, Map<String, Dispatcher> patternDisMap) {
        dispatcherMap = Collections.unmodifiableMap(disMap);
        patternDispatcherMap = Collections.unmodifiableMap(patternDisMap);
    }

    static DispatcherMap create(List<Class<?>> httpServiceList) {
        Map<String, Dispatcher> pathMap = new HashMap<>();
        Map<String, Dispatcher> patternMap = new HashMap<>();
        for (Class<?> httpClass : httpServiceList) {
            addHttpService(pathMap, patternMap, httpClass);
        }
        return new DispatcherMap(pathMap, patternMap);
    }

    Dispatcher getDispatcher(String path) {
        if(path != null) {
            if(dispatcherMap.containsKey(path)) {
                return dispatcherMap.get(path);
            } else {
                int lastIndex = path.lastIndexOf("/");
                if (-1 < lastIndex) {
                    String pattern = path.substring(0, lastIndex);
                    if(patternDispatcherMap.containsKey(pattern)) {
                        return patternDispatcherMap.get(pattern);
                    } else {
                        return getDispatcher(pattern);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public final int compare(Dispatcher o1, Dispatcher o2) {
        return o1.getPath().compareTo(o2.getPath());
    }

    private static void addHttpService(Map<String, Dispatcher> disMap, Map<String, Dispatcher> patMap, Class<?> type) {
        HttpService httpService = type.getAnnotation(HttpService.class);
        if (httpService == null) {
            throw new MessageException(type + " >> Class not annotated with @HttpService");
        }
        String errorMessage = isValidPath(type, httpService.value());
        if (errorMessage != null) {
            throw new MessageException(errorMessage);
        }
        List<Method> pathMethodList = getPathAnnotatedMethods(type);
        if (pathMethodList.isEmpty()) {
            throw new MessageException("At least one method annotated with @POST, @GET or @DELETE >> " + type);
        }
        final String servicePrefix = httpService.value();
        final boolean serviceSecured = httpService.secured();
        for (Method method : pathMethodList) {
            errorMessage = isValidMethodDeclaration(type, method);
            if (errorMessage != null) {
                throw new MessageException(errorMessage);
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
                throw new MessageException(type + ", " + method.getName() + " http service method is not valid : " + httpService);
            }
            if (serviceSecured && !methodSecured) {
                methodSecured = true;
            }
            errorMessage = isValidPath(type, method, serviceSuffix);
            if (errorMessage != null) {
                throw new MessageException(errorMessage);
            }
            if (serviceSuffix.endsWith("/*")) {
                int lastIndex = serviceSuffix.lastIndexOf("/*");
                serviceSuffix = serviceSuffix.substring(0, lastIndex);
                String pathPtn = servicePrefix + serviceSuffix;
                patMap.put(pathPtn.toLowerCase(), new Dispatcher(pathPtn, true, methodType, method, methodSecured));
            } else {
                String path = servicePrefix + serviceSuffix;
                disMap.put(path.toLowerCase(), new Dispatcher(path, false, methodType, method, methodSecured));
            }
        }
    }

    private static String isValidPath(Class<?> typeClass, String path) {
        if (!path.startsWith("/")) {
            return typeClass + " >> Path should be started with '/' char";
        }
        if (path.endsWith("/")) {
            return typeClass + " >> Path should not be end with '/' char";
        }
        return null;
    }

    private static String isValidPath(Class<?> typeClass, Method method, String path) {
        if (!path.startsWith("/")) {
            return typeClass + "." + method.getName() + " >> Path should be started with '/' char";
        }
        if (path.endsWith("/")) {
            return typeClass + "." + method.getName() + " >> Path should not be end with '/' char";
        }
        return null;
    }

    private static List<Method> getPathAnnotatedMethods(final Class<?> pathClass) {
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

    private static String isValidMethodDeclaration(Class<?> cls, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        boolean isPattern = isPatternClass(paramTypes);
        if (3 < paramTypes.length) {
            return cls + " >> " + method + " >> more then 3 parameters not supported";
        }
        if (3 == paramTypes.length && !isPattern) {
            return cls + " >> " + method + " >> more then 2 parameters not supported, unless pattern is used at url";
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
        if (3 == paramTypes.length) {
            boolean isFirstResponse = isValidResponseParam(paramTypes[0]);
            boolean isSecondResponse = isValidResponseParam(paramTypes[1]);
            boolean isThreeResponse = isValidResponseParam(paramTypes[2]);
            if (void.class.equals(returnType)) {
                if (!isFirstResponse && !isSecondResponse && !isThreeResponse) {
                    return cls + " >> " + method + " >> response not processed";
                }
            } else {
                if (isFirstResponse || isSecondResponse || isThreeResponse) {
                    return cls + " >> " + method + " >> response logic error";
                }
            }
        }
        return null;
    }

    private static boolean isPatternClass(Class<?>[] sourceArray) {
        for (Class<?> source : sourceArray) {
            if (String.class.equals(source)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidResponseParam(Class<?> source) {
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
