package dcapture.io;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.bind.Binder;

import java.util.List;

public interface DispatcherRegistry {

    void inject(Binder binder);

    List<Class<?>> getPathServiceList();

    void destroyed(Injector injector);
}
