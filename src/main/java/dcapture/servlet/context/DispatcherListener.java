package dcapture.servlet.context;

import dcapture.api.io.HttpModule;
import dcapture.api.postgres.PgDatabase;
import dcapture.api.sql.SqlContext;
import dcapture.api.sql.SqlDatabase;
import dcapture.api.sql.SqlFactory;
import dcapture.api.support.ContextResource;
import dcapture.api.support.Messages;
import dcapture.api.support.ObjectUtils;
import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;
import io.github.pustike.inject.bind.Binder;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class DispatcherListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(DispatcherListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Injector injector = Injectors.create(binder -> configureBinder(sce.getServletContext(), binder));
        sce.getServletContext().setAttribute(Injector.class.getName(), injector);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }

    private void configureBinder(ServletContext context, Binder binder) {
        ContextResource resource = ContextResource.get(context);
        String defaultLanguage = resource.getSetting("language");
        Messages messages = getMessages(context, resource.getMessagePaths(), defaultLanguage);
        List<HttpModule> httpModules = getHttpModules(context.getInitParameter("http-modules"));
        SqlDatabase sqlDatabase = getSqlDatabase(context, resource);
        if (sqlDatabase == null) {
            logger.error("***** database error *****");
            return;
        }
        SqlContext sqlContext = sqlDatabase.getContext();
        logger.info("Http modules are configured (" + httpModules.size() + ")");
        List<Class<?>> entityList = new ArrayList<>();
        List<Class<?>> httpServiceList = new ArrayList<>();
        for (HttpModule httpModule : httpModules) {
            logger.info(httpModule);
            List<Class<?>> entities = httpModule.getEntityList();
            List<Class<?>> httpServices = httpModule.getHttpServiceList();
            if (entities != null) {
                entityList.addAll(entities);
            }
            if (httpServices != null) {
                httpServiceList.addAll(httpServices);
            }
        }
        SqlFactory.setEntityList(sqlContext, entityList);
        binder.bind(SqlContext.class).toInstance(sqlContext);
        binder.bind(SqlDatabase.class).toInstance(sqlDatabase);
        binder.bind(ContextResource.class).toInstance(resource);
        binder.bind(Messages.class).toInstance(messages);
        try {
            httpServiceList.forEach(binder::bind);
            binder.bind(DispatcherMap.class).toInstance(DispatcherMap.create(httpServiceList));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
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

    private Messages getMessages(ServletContext context, Set<String> paths, String defaultLanguage) {
        Messages messages = new Messages();
        messages.setLanguage(defaultLanguage);
        try {
            messages.loadProperties(context, paths, true);
        } catch (IOException ex) {
            logger.error("Message sources loading error : " + ex.getMessage());
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
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

    private SqlDatabase getSqlDatabase(ServletContext context, ContextResource resource) {
        Properties properties = resource.getDatabaseConfig();
        properties.setProperty("user", ObjectUtils.decodeBase64(properties.getProperty("user")));
        properties.setProperty("password", ObjectUtils.decodeBase64(properties.getProperty("password")));
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
        SqlContext sqlContext = SqlFactory.getSqlContext(context, resource.getSqlPaths(), driver.getName());
        if (properties.getProperty("url").toLowerCase().contains("postgres")) {
            return new PgDatabase(sqlContext, SqlFactory.getDataSource(properties));
        }
        return null;
    }
}
