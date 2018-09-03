package dcapture.io;

import javax.json.*;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

public class BaseSettings {
    private static Logger logger = Logger.getLogger("dcapture.io");
    private String version, id, name;
    private String databaseName, databaseUrl, databaseUser, databasePassword;
    private JsonArray databaseConfig;
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

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public JsonArray getDatabaseConfig() {
        return databaseConfig;
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
                    String[] dbs = getDatabaseSettings(text);
                    settings.databaseName = dbs[0];
                    settings.databaseUrl = dbs[1];
                    settings.databaseUser = dbs[2];
                    settings.databasePassword = dbs[3];
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
        if (settings.databaseName != null) {
            String dbCfgPath = "/" + settings.databaseName + ".json";
            logger.severe("Database configuration reading from " + classPath.getResource(dbCfgPath));
            JsonReader dbCfgReader = Json.createReader(classPath.getResourceAsStream(dbCfgPath));
            settings.databaseConfig = dbCfgReader.readArray();
        }
        return settings;
    }

    private static String[] getDatabaseSettings(String compact) {
        String[] values = compact.trim().split(" ");
        if (4 > values.length) {
            throw new IllegalArgumentException("Database user and password should be encrypted, and format is : " +
                    "name [space] url [space] userName [space] password");
        }
        if (notValid(values[0])) {
            throw new IllegalArgumentException("Database name not valid");
        }
        if (notValid(values[1])) {
            throw new IllegalArgumentException("Database url not valid");
        }
        if (notValid(values[2])) {
            throw new IllegalArgumentException("Database user not valid");
        }
        if (notValid(values[3])) {
            throw new IllegalArgumentException("Database password not valid");
        }
        return new String[]{values[0].trim(), values[1].trim(), values[2].trim(), values[3].trim()};
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