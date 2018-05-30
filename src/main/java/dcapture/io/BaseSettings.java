package dcapture.io;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BaseSettings {
    private File locale, webApp;
    private String version, language;
    private Set<String> languages;
    private Map<String, String[]> databases;
    private int port;

    @SuppressWarnings("unchecked")
    public void config(String name, Object value) {
        name = name.trim().toLowerCase();
        if (value instanceof String) {
            String text = (String) value;
            if ("version".equals(name)) {
                this.version = text;
            } else if ("language".equals(name)) {
                this.language = text;
            }
        } else if (value instanceof Integer) {
            int intValue = (Integer) value;
            if ("port".equals(name)) {
                this.port = intValue;
            }
        } else if (value instanceof Map) {
            if ("database".equals(name)) {
                this.databases = Collections.unmodifiableMap((Map<String, String[]>) value);
            }
        } else if (value instanceof File) {
            File file = (File) value;
            if ("locale".equals(name)) {
                this.locale = file;
            } else if ("webapp".equals(name)) {
                this.webApp = file;
            }
        } else if (value instanceof Set) {
            if ("languages".equals(name)) {
                this.languages = Collections.unmodifiableSet((Set<String>) value);
            }
        }
    }

    public String getVersion() {
        return version;
    }

    public String getLanguage() {
        return language;
    }

    public Set<String> getLanguages() {
        return new HashSet<>(languages);
    }

    public int getPort() {
        return port;
    }

    public String[] getDatabase(String name) {
        return databases.get(name);
    }

    public File getLocaleFolder() {
        return locale;
    }

    public File getWebAppFolder() {
        return webApp;
    }
}