package dcapture.io;

import javax.json.*;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

public class BaseSettings {
    private static Logger logger = Logger.getLogger("dcapture.io");
    private String version, id, name;
    private String database;
    private int port;

    private BaseSettings() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public int getPort() {
        return port;
    }

    public static BaseSettings load(Class<?> classPath) throws Exception {
        BaseSettings settings = new BaseSettings();
        settings.version = "1.0";
        settings.port = 9090;
        JsonReader settingsReader = Json.createReader(classPath.getResourceAsStream("/settings.json"));
        JsonObject settingsJson = settingsReader.readObject();
        for (Map.Entry<String, JsonValue> entry : settingsJson.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonValue jsonValue = entry.getValue();
            if (jsonValue instanceof JsonString) {
                String text = ((JsonString) jsonValue).getString().trim();
                if (notValid(text)) {
                    throw new IllegalArgumentException("Base settings key [" + key + "] is not valid data!");
                }
                if ("id".equals(key)) {
                    settings.id = text;
                } else if ("name".equals(key)) {
                    settings.name = text;
                } else if ("database".equals(key)) {
                    settings.database = text.trim();
                } else if ("version".equals(key)) {
                    settings.version = text;
                } else if ("port".equals(key)) {
                    settings.port = parsePort(text);
                }
            } else if (jsonValue instanceof JsonNumber) {
                if ("port".equals(key)) {
                    JsonNumber num = (JsonNumber) jsonValue;
                    settings.port = 1 > num.intValue() ? 9090 : num.intValue();
                }
            }
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

    private static boolean notValid(String value) {
        return value == null || value.trim().isEmpty();
    }
}