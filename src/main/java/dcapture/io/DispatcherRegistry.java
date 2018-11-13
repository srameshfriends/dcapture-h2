package dcapture.io;

import io.github.pustike.inject.bind.Binder;

import javax.servlet.ServletContext;
import java.util.List;

public interface DispatcherRegistry {

    void bind(Binder binder);

    List<Class<?>> getPathServiceList();

    void contextInitialized(ServletContext context);

    void contextDestroyed(ServletContext context);
}
