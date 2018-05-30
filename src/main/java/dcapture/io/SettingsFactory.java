package dcapture.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

import javax.json.*;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class SettingsFactory {
    private static final Logger logger = Logger.getLogger(SettingsFactory.class);

    public static BaseSettings loadBaseSetting(Class<?> classToGetPath, File jsonFile) throws Exception {
        logging("Base Setting loading : " + jsonFile.getPath());
        JsonObject obj = JsonMapper.readObject(jsonFile);
        return loadBaseSetting(classToGetPath, obj);
    }

    public static BaseSettings loadBaseSetting(Class<?> classToGetPath, JsonObject obj) throws Exception {
        BaseSettings settings = new BaseSettings();
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            String key = entry.getKey().toLowerCase();
            JsonValue jsonValue = entry.getValue();
            if (jsonValue instanceof JsonString) {
                String text = ((JsonString) jsonValue).getString();
                if ("locale".equals(key)) {
                    settings.config(key, getFile(classToGetPath, text));
                } else if ("webapp".equals(key)) {
                    settings.config(key, getFile(classToGetPath, text));
                } else if ("config".equals(key)) {
                    settings.config(key, getFile(classToGetPath, text));
                } else {
                    settings.config(key, text);
                }
            } else if (jsonValue instanceof JsonNumber) {
                JsonNumber num = (JsonNumber) jsonValue;
                settings.config(key, num.intValue());
            } else if (jsonValue instanceof JsonArray) {
                JsonArray array = (JsonArray) jsonValue;
                if ("database".equals(key)) {
                    settings.config(key, getDatabaseMap(array));
                }
            }
        }
        if (settings.getLocaleFolder() != null) {
            settings.config("languages", getSupportedLanguage(settings.getLocaleFolder()));
        }
        if (settings.getLanguage() == null) {
            settings.config("language", "en");
        }
        if (1 > settings.getPort()) {
            settings.config("port", 9090);
        }
        return settings;
    }

    private static Map<String, String[]> getDatabaseMap(JsonArray array) {
        Map<String, String[]> databases = new HashMap<>();
        List<JsonObject> dbObjList = array == null ? new ArrayList<>() : JsonMapper.getList(array);
        for (JsonObject db : dbObjList) {
            String dbName = JsonMapper.getString(db, "name");
            String dbUrl = JsonMapper.getString(db, "url");
            String dbUser = JsonMapper.getString(db, "user");
            String dbPass = JsonMapper.getString(db, "password");
            if (isValid(dbName) && isValid(dbUrl) && isValid(dbUser) && isValid(dbPass)) {
                databases.put(dbName, new String[]{dbUrl, dbUser, dbPass});
            }
        }
        return databases;
    }

    private static File getFile(Class<?> classToGetPath, String configPath) throws URISyntaxException {
        if (configPath.startsWith("classpath:")) {
            String classPath = configPath.replaceFirst("classpath:", "");
            return getClassPath(classToGetPath, classPath);
        } else if (configPath.startsWith("absolute:")) {
            String absolutePath = configPath.replaceFirst("absolute:", "");
            return new File(absolutePath);
        }
        return new File(configPath);
    }

    public static File getClassPath(Class<?> classToGetPath, String path) {
        ClassLoader classLoader = classToGetPath.getClassLoader();
        URL url = classLoader.getResource(path);
        if (url == null) {
            throw new NullPointerException("Classpath file is missing : " + path);
        }
        return new File(url.getFile());
    }

    private static Set<String> getSupportedLanguage(File localFolder) {
        Set<String> languageSet = new HashSet<>();
        List<File> files = (List<File>) FileUtils.listFiles(localFolder,
                TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files) {
            String language = findLanguage(file);
            if (language != null && 1 < language.length()) {
                languageSet.add(language);
            }
        }
        if (languageSet.isEmpty()) {
            languageSet.add("en");
        }
        return languageSet;
    }

    public static Localization getLocalization(String defaultLanguage, File localeFolder) {
        Localization localization = new Localization();
        localization.setLanguage(defaultLanguage);
        localization.setLocaleFolder(localeFolder);
        localization.setLanguages(getSupportedLanguage(localeFolder));
        return localization;
    }

    public static String decode(String value) {
        return new String(Base64.getDecoder().decode(value));
    }

    public static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private static String findLanguage(File file) {
        String name = file.getPath().replace(".properties", "");
        if (5 > name.length()) {
            return null;
        }
        int idx = name.lastIndexOf("-");
        if ((idx + 3) > name.length()) {
            return null;
        }
        return name.substring(idx + 1, idx + 3);
    }

    private static void logging(Exception ex) {
        if (logger.isDebugEnabled()) {
            ex.printStackTrace();
        } else {
            logger.error(ex.getMessage());
        }
    }

    private static void logging(String msg) {
        if (logger.isDebugEnabled()) {
            logger.info(msg);
        }
    }

    private static boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
