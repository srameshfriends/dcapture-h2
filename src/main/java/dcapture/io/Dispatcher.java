package dcapture.io;

import java.lang.reflect.Method;
import java.util.Objects;

public class Dispatcher {
    private final String path, httpMethod;
    private final Method method;

    Dispatcher(String path, String httpMethod, Method method) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.method = method;
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
