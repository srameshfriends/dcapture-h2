package dcapture.io;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

public class Localization {
    private static Logger logger = Logger.getLogger(Localization.class);
    private Map<String, Properties> cache;
    private String language;

    private Localization() {
    }

    public String getLanguage() {
        return language;
    }

    public Set<String> getLanguages() {
        return cache.keySet();
    }

    public Properties getProperties(String lang) {
        return cache.get(lang == null ? language : lang);
    }

    public static synchronized Localization getInstance(Map<String, Properties> map, String defaultLanguage) {
        Localization localization = new Localization();
        localization.language = defaultLanguage == null ? "en" : defaultLanguage;
        localization.cache = Collections.unmodifiableMap(map);
        return localization;
    }

    public String get(String name) {
        Properties prop = cache.get(language);
        String value = prop == null ? null : prop.getProperty(name);
        return value == null ? name : value;
    }

    private static synchronized Properties loadProperties(IOStream ioStream, String lang, String[] pathArray)
            throws IOException {
        Properties properties = new Properties();
        for (String path : pathArray) {
            logger.debug("Loading language properties : " + path);
            properties.load(ioStream.getResourceAsStream(path));
        }
        logger.debug(lang + " > properties : " + properties.size());
        return properties;
    }

    public String getMessage(String name, Object... args) {
        return args == null ? get(name) : MessageFormat.format(get(name), args);
    }

    public String get(String name, String lang) {
        Properties prop = cache.get(lang == null ? language : lang);
        String value = prop == null ? null : prop.getProperty(name);
        return value == null ? name : value;
    }

    public static synchronized Localization load(AppSettings settings, IOStream ioStream) throws Exception {
        Map<String, Properties> map = new HashMap<>();
        for (Map.Entry<String, String[]> entry : settings.getLanguages().entrySet()) {
            map.put(entry.getKey(), loadProperties(ioStream, entry.getKey(), entry.getValue()));
        }
        String lang = settings.getLanguage();
        Localization localization = new Localization();
        localization.language = lang == null ? "en" : lang;
        localization.cache = Collections.unmodifiableMap(map);
        return localization;
    }

    public Map<String, Properties> getPropertiesMap() {
        return cache;
    }
}
