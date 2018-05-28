package dcapture.io;

public enum ContentType {
    Json("application/json"), Html("text/html"), FormData("multipart/form-data");

    private final String type;

    ContentType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
