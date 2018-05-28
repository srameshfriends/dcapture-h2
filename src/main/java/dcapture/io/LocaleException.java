package dcapture.io;

public class LocaleException extends RuntimeException {
    private static final long serialVersionUID = 516271579863288L;
    private String language;

    public LocaleException(String name) {
        super(name);
    }

    public LocaleException(String name, Throwable cause) {
        super(name, cause);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
