package dcapture.io;

import javax.json.*;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

public class Localization {
    private static Logger logger = Logger.getLogger("dcapture.io");
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

    public String get(String lang, String name) {
        Properties prop = cache.get(lang == null ? language : lang);
        String value = prop == null ? null : prop.getProperty(name);
        return value == null ? name : value;
    }

    public String get(String name) {
        Properties prop = cache.get(language);
        String value = prop == null ? null : prop.getProperty(name);
        return value == null ? name : value;
    }

    public String getMessage(String name, Object... args) {
        Properties prop = cache.get(language);
        String value = prop == null ? null : prop.getProperty(name);
        return value != null ? MessageFormat.format(value, args) : name;
    }

    public static Localization development(Class<?> classPath) throws Exception {
        URL url = classPath.getResource("/locale");
        if (url == null) {
            throw new NullPointerException("locale folder not found at module class path : " + classPath.getName());
        }
        File localeFolder = Paths.get(url.toURI()).toFile();
        logger.severe("Loading locale properties from : " + localeFolder);
        Map<String, Properties> map = new HashMap<>();
        String lang = "en";
        JsonReader reader = Json.createReader(classPath.getResourceAsStream("/locale.json"));
        JsonObject json = reader.readObject();
        for (Map.Entry<String, JsonValue> entry : json.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonValue jsonValue = entry.getValue();
            if (jsonValue instanceof JsonString && "language".equals(key)) {
                lang = ((JsonString) jsonValue).getString().trim();
                if (notValid(lang)) {
                    lang = "en";
                }
            } else if (jsonValue instanceof JsonArray) {
                map.put(key, development(localeFolder, (JsonArray) jsonValue));
            }
        }
        Localization localization = new Localization();
        localization.language = lang;
        localization.cache = Collections.unmodifiableMap(map);
        return localization;
    }

    private static synchronized Properties development(File folder, JsonArray array) throws Exception {
        Properties properties = new Properties();
        for (JsonValue json : array) {
            if (json instanceof JsonString) {
                String name = ((JsonString) json).getString();
                File file = new File(folder, "/" + name);
                logger.severe("Locale File Loading : " + file);
                properties.load(new FileInputStream(file));
            }
        }
        return properties;
    }

    public static Localization load(Class<?> classPath) throws Exception {
        logger.severe("Localization configuration reading from " + classPath.getResource("/locale.json"));
        Map<String, Properties> map = new HashMap<>();
        String lang = "en";
        JsonReader reader = Json.createReader(classPath.getResourceAsStream("/locale.json"));
        JsonObject json = reader.readObject();
        for (Map.Entry<String, JsonValue> entry : json.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonValue jsonValue = entry.getValue();
            if (jsonValue instanceof JsonString && "language".equals(key)) {
                lang = ((JsonString) jsonValue).getString().trim();
                if (notValid(lang)) {
                    lang = "en";
                }
            } else if (jsonValue instanceof JsonArray) {
                map.put(key, loadProperties(classPath, (JsonArray) jsonValue));
            }
        }
        Localization localization = new Localization();
        localization.language = lang;
        localization.cache = Collections.unmodifiableMap(map);
        for (Map.Entry<String, Properties> entry : map.entrySet()) {
            logger.severe("Localization language for  [" + entry.getKey()
                    + "] properties count : " + entry.getValue().size());
        }
        return localization;
    }

    private static synchronized Properties loadProperties(Class<?> classPath, JsonArray array) throws Exception {
        Properties properties = new Properties();
        for (JsonValue json : array) {
            if (json instanceof JsonString) {
                String name = ((JsonString) json).getString();
                logger.severe(classPath.getName() + " >> " + classPath.getResource("/locale/" + name));
                properties.load(classPath.getResourceAsStream("/locale/" + name));
            }
        }
        return properties;
    }

    private static boolean notValid(String value) {
        return value == null || value.trim().isEmpty();
    }
}
