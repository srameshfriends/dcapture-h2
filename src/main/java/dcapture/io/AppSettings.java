package dcapture.io;

import javax.json.*;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

public class AppSettings {
    public static final String PATH = "/config/application-settings.json";
    private static Logger logger = Logger.getLogger("dcapture.io");
    private String version, id, name, language, database;
    private String[] databases, columnSets;
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

    public String[] getDatabases() {
        return databases;
    }

    public String[] getColumnSets() {
        return columnSets;
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
                if ("id".equals(key)) {
                    settings.id = text;
                } else if ("name".equals(key)) {
                    settings.name = text;
                } else if ("database".equals(key)) {
                    settings.database = text;
                } else if ("port".equals(key)) {
                    settings.port = parsePort(text);
                } else if ("language".equals(key)) {
                    settings.language = text.toLowerCase();
                } else if ("version".equals(key)) {
                    settings.version = text;
                }
            } else if (jsonValue instanceof JsonNumber) {
                if ("port".equals(key)) {
                    settings.port = ((JsonNumber) jsonValue).intValue();
                }
            } else if (jsonValue instanceof JsonArray) {
                if ("databases".equals(key)) {
                    settings.databases = toStringArray("/config/", (JsonArray) jsonValue);
                } else if ("columnsets".equals(key)) {
                    settings.columnSets = toStringArray("/config/", (JsonArray) jsonValue);
                }
            } else if (jsonValue instanceof JsonObject) {
                if ("languages".equals(key)) {
                    settings.languagesMap = Collections.unmodifiableMap(loadLanguages(jsonValue.asJsonObject()));
                } else if ("appdata".equals(key)) {
                    settings.appDataMap = Collections.unmodifiableMap(loadAppDataMap(jsonValue.asJsonObject()));
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

    public String getDatabase() {
        return database;
    }

    public static String decode(String value) {
        return new String(Base64.getDecoder().decode(value));
    }

    public static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private static int parsePort(String value) {
        try {
            int prt = Integer.parseInt(value);
            return 1 > prt ? 9090 : prt;
        } catch (NumberFormatException ex) {
            logger.severe("Base settings port not valid");
        }
        return 9090;
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

    private static String[] toStringArray(String prefix, JsonArray array) {
        LinkedHashSet<String> hashSet = new LinkedHashSet<>();
        for (JsonValue json : array) {
            if (json instanceof JsonString) {
                String suffix = ((JsonString) json).getString().trim();
                hashSet.add(prefix + suffix);
            }
        }
        return hashSet.isEmpty() ? null : hashSet.toArray(new String[0]);
    }
}