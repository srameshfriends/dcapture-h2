package dcapture.io;

import java.lang.reflect.Method;
import java.util.Objects;

public class Dispatcher {
    private final String path, httpMethod;
    private final Method method;
    private final boolean secured;

    Dispatcher(String path, String httpMethod, Method method, boolean secured) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.method = method;
        this.secured = secured;
    }

    public String getPath() {
        return path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isSecured() {
        return secured;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dispatcher that = (Dispatcher) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public String toString() {
        return path;
    }
}
