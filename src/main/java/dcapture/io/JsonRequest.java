package dcapture.io;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

public class JsonRequest extends HttpServletRequestWrapper {
    private static final Logger logger = LogManager.getLogger(JsonRequest.class);
    private JsonArray bodyArray;
    private JsonObject bodyObject;

    JsonRequest(HttpServletRequest request) {
        super(request);
        try {
            JsonReader reader = Json.createReader(request.getInputStream());
            JsonStructure body = reader.read();
            if (body instanceof JsonObject) {
                bodyObject = (JsonObject) body;
            } else if (body instanceof JsonArray) {
                bodyArray = (JsonArray) body;
            }
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
    }

    public JsonObject getJsonObject() {
        return bodyObject;
    }

    public JsonArray getJsonArray() {
        return bodyArray;
    }

    public String getContent() {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(getInputStream(), writer, Charset.forName("UTF-8"));
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return writer.toString();
    }

    public String getString(String key) {
        JsonValue value = getJsonObject() == null ? null : getJsonObject().get(key);
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        }
        return null;
    }

    public String getString(String key, String safeDefault) {
        String value = getString(key);
        return value == null ? safeDefault : value;
    }

    public JsonObject getObject(String key) {
        JsonValue value = getJsonObject() == null ? null : getJsonObject().get(key);
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        return null;
    }

    public long getLong(String key) {
        JsonValue value = getJsonObject() == null ? null : getJsonObject().get(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).longValue();
        }
        return 0L;
    }

    public int getInt(String key) {
        JsonValue value = getJsonObject() == null ? null : getJsonObject().get(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).intValue();
        }
        return 0;
    }
}
