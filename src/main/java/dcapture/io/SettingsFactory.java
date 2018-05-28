package dcapture.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SettingsFactory {
    private static final Logger logger = Logger.getLogger(SettingsFactory.class);

    public static BaseSettings loadBaseSettings(final File root) throws Exception {
        logging("Root File : " + root.getPath());
        File sourceSys = getClassPath("sys");
        File sourceConfig = getClassPath("config");
        File targetConfig = createDir(root, "config");
        File targetSys = createDir(root, "sys");
        FileUtils.copyDirectory(sourceConfig, targetConfig);
        FileUtils.copyDirectory(sourceSys, targetSys);
        createDir(root, "data");
        File local = createDir(targetConfig, "local");
        File settingsFile = new File(targetConfig, "settings.json");
        JsonObject settingsObj = JsonMapper.readObject(settingsFile);
        logging("Base Settings : " + settingsObj.toString());
        String language = JsonMapper.getString(settingsObj, "language", "en");
        String version = JsonMapper.getString(settingsObj, "version", "1.0");
        int port = JsonMapper.getInt(settingsObj, "port");
        port = 0 == port ? 9090 : port;
        JsonArray dbCfgAry = JsonMapper.getJsonArray(settingsObj, "database");
        Map<String, String[]> databases = new HashMap<>();
        List<JsonObject> dbObjList = dbCfgAry == null ? new ArrayList<>() : JsonMapper.getList(dbCfgAry);
        for (JsonObject db : dbObjList) {
            String dbName = JsonMapper.getString(db, "name");
            String dbUrl = JsonMapper.getString(db, "url");
            String dbUser = JsonMapper.getString(db, "user");
            String dbPass = JsonMapper.getString(db, "password");
            if (isValid(dbName) && isValid(dbUrl) && isValid(dbUser) && isValid(dbPass)) {
                databases.put(dbName, new String[]{dbUrl, dbUser, dbPass});
            }
        }
        BaseSettings base = new BaseSettings();
        base.setRoot(root);
        base.setLanguage(language);
        base.setLanguages(getSupportedLanguage(local));
        base.setPort(port);
        base.setDatabase(databases);
        base.setVersion(version);
        return base;
    }

    private static File createDir(File parent, String extend) throws IOException {
        File file = new File(parent, extend);
        if (!file.exists()) {
            Files.createDirectories(file.toPath());
        }
        return file;
    }

    private static File getClassPath(String path) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        URL url = classloader.getResource(path);
        File uri = null;
        try {
            String uriPath = url == null ? null : Paths.get(url.toURI()).toString();
            uri = uriPath == null ? null : new File(uriPath);
        } catch (URISyntaxException ex) {
            logging(ex);
        }
        if (uri == null) {
            throw new NullPointerException("Source not found at class path " + path);
        }
        return uri;
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
