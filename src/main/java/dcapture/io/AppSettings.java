package dcapture.io;

import org.apache.log4j.Logger;

import javax.json.*;
import java.io.InputStream;
import java.util.*;

public class AppSettings {
    public static final String PATH = "/config/application-settings.json";
    private static Logger logger = Logger.getLogger(AppSettings.class);
    private String version, id, name, language;
    private Properties database;
    private Map<String, String[]> languagesMap;
    private Map<String, String> appDataMap;
    private int port;

    private AppSettings() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public int getPort() {
        return port;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String[]> getLanguages() {
        return languagesMap;
    }

    public String getAppData(String name) {
        return appDataMap.get(name.toLowerCase());
    }

    public static AppSettings load(InputStream stream) {
        AppSettings settings = new AppSettings();
        JsonReader reader = Json.createReader(stream);
        JsonObject jsonObject = reader.readObject();
        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonValue jsonValue = entry.getValue();
            if (jsonValue instanceof JsonString) {
                String text = ((JsonString) jsonValue).getString().trim();
                switch (key) {
                    case "id":
                        settings.id = text;
                        break;
                    case "name":
                        settings.name = text;
                        break;
                    case "port":
                        settings.port = parsePort(text);
                        break;
                    case "language":
                        settings.language = text.toLowerCase();
                        break;
                    case "version":
                        settings.version = text;
                        break;
                }
            } else if (jsonValue instanceof JsonNumber) {
                if ("port".equals(key)) {
                    settings.port = ((JsonNumber) jsonValue).intValue();
                }
            } else if (jsonValue instanceof JsonObject) {
                switch (key) {
                    case "languages":
                        settings.languagesMap = Collections.unmodifiableMap(loadLanguages(jsonValue.asJsonObject()));
                        break;
                    case "appdata":
                        settings.appDataMap = Collections.unmodifiableMap(loadAppDataMap(jsonValue.asJsonObject()));
                        break;
                    case "database":
                        settings.database = loadDatabaseProperties(jsonValue.asJsonObject());
                        break;
                }
            }
        }
        if (settings.version == null) {
            settings.version = "1.0";
        }
        if (0 == settings.port) {
            settings.port = 9090;
        }
        return settings;
    }

    private static int parsePort(String value) {
        try {
            int prt = Integer.parseInt(value);
            return 1 > prt ? 9090 : prt;
        } catch (NumberFormatException ex) {
            logger.error("Base settings port not valid");
        }
        return 9090;
    }

    public static String decode(String value) {
        return new String(Base64.getDecoder().decode(value));
    }

    public static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private static String toString(JsonArray array) {
        LinkedHashSet<String> hashSet = new LinkedHashSet<>();
        for (JsonValue json : array) {
            if (json instanceof JsonString) {
                String suffix = ((JsonString) json).getString().trim();
                hashSet.add(suffix);
            }
        }
        return String.join(",", hashSet);
    }

    private static Map<String, String[]> loadLanguages(JsonObject object) {
        Map<String, String[]> map = new Hashtable<>();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            if (entry.getValue() instanceof JsonArray) {
                map.put(entry.getKey().toLowerCase(), toStringArray("/locale/", (JsonArray) entry.getValue()));
            }
        }
        return map;
    }

    private static Map<String, String> loadAppDataMap(JsonObject object) {
        Map<String, String> map = new Hashtable<>();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            if (entry.getValue() instanceof JsonString) {
                String pathSuffix = ((JsonString) entry.getValue()).getString().trim();
                map.put(entry.getKey().toLowerCase(), "/appdata/" + pathSuffix);
            }
        }
        return map;
    }

    private static Properties loadDatabaseProperties(JsonObject object) {
        Properties prop = new Properties();
        String user = getString(object.get("user"));
        String password = getString(object.get("password"));
        prop.put("url", getString(object.get("url")));
        prop.put("user", AppSettings.decode(user));
        prop.put("password", AppSettings.decode(password));
        JsonValue metadata = object.get("metadata");
        if (metadata instanceof JsonArray) {
            prop.put("metadata", toString(metadata.asJsonArray()));
        }
        return prop;
    }

    private static String[] toStringArray(String prefix, JsonArray array) {
        LinkedHashSet<String> hashSet = new LinkedHashSet<>();
        for (JsonValue json : array) {
            if (json instanceof JsonString) {
                String suffix = ((JsonString) json).getString().trim();
                hashSet.add(prefix + suffix);
            }
        }
        return hashSet.isEmpty() ? new String[]{} : hashSet.toArray(new String[0]);
    }

    private static String getString(JsonValue value) {
        if (value instanceof JsonString) {
            return (((JsonString) value).getString());
        }
        return "";
    }

    public Properties getDatabase() {
        return database;
    }
}