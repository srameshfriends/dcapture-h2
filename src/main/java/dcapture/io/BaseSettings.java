package dcapture.io;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class BaseSettings {
    private File root;
    private String version, language;
    private int port;
    private Set<String> languages;
    private Map<String, String[]> databases;

    void setRoot(File root) {
        this.root = root;
    }

    void setVersion(String version) {
        this.version = version;
    }

    void setLanguage(String language) {
        this.language = language;
    }

    void setPort(int port) {
        this.port = port;
    }

    void setDatabase(Map<String, String[]> databases) {
        this.databases = Collections.unmodifiableMap(databases);
    }

    void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public File getRoot() {
        return root;
    }

    public String getVersion() {
        return version;
    }

    public String getLanguage() {
        return language;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public int getPort() {
        return port;
    }

    public String[] getDatabase(String name) {
        return databases.get(name);
    }

    public File getConfigFolder() {
        return new File(root, "config");
    }

    public File getLocalFolder() {
        File cfg = new File(root, "config");
        return new File(cfg, "local");
    }

    public File getSysFolder() {
        return new File(root, "sys");
    }

    public File getWebAppFolder() {
        return new File(getSysFolder(), "webapp");
    }
}