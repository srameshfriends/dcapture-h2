package dcapture.io;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class Localization {
    private final Logger logger = Logger.getLogger(Localization.class);
    private Map<String, Properties> localMap;
    private Set<String> languages;
    private String language;
    private File localeFolder;

    public void setLanguage(String lang) {
        this.language = lang;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = Collections.unmodifiableSet(languages);
    }

    public void setLocaleFolder(File localeFolder) {
        this.localeFolder = localeFolder;
    }

    public String getLanguage(String lang) {
        return lang == null || !languages.contains(lang) ? "en" : lang;
    }

    public String getLanguage() {
        return language;
    }

    private Properties loadProperties(File localFile) {
        if (!localFile.exists()) {
            throw new NullPointerException(localFile.getAbsolutePath() + " file not found");
        }
        if (logger.isDebugEnabled()) {
            logger.info("Loading Message : " + localFile);
        }
        Properties local = new Properties();
        try {
            FileInputStream stream = new FileInputStream(localFile);
            local.load(stream);
            stream.close();
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
        return local;
    }

    private String findLanguage(File file) {
        String name = file.getPath().replace(".properties", "");
        int idx = name.lastIndexOf("-");
        return name.substring(idx + 1, idx + 3);
    }

    private synchronized Map<String, Properties> getLocaleMap() {
        if (localMap == null) {
            Map<String, Properties> cache = new HashMap<>();
            String[] extensions = new String[]{"properties"};
            List<File> fileList = (List<File>) FileUtils.listFiles(localeFolder, extensions, true);
            for (File file : fileList) {
                String lang = findLanguage(file);
                Properties properties = cache.get(lang);
                if (properties == null) {
                    properties = new Properties();
                    cache.put(lang, properties);
                }
                properties.putAll(loadProperties(file));
            }
            localMap = Collections.unmodifiableMap(cache);
        }
        return localMap;
    }

    public Properties getProperties(String lang) {
        String language = getLanguage(lang);
        return getLocaleMap().get(language);
    }

    public String get(String lang, String name) {
        lang = getLanguage(lang);
        Properties prop = getLocaleMap().get(lang);
        String value = prop == null ? null : prop.getProperty(name);
        return value == null ? name : value;
    }

    public String get(String name) {
        return get(language, name);
    }
}
