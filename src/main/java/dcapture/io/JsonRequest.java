package dcapture.io;

import org.apache.commons.io.IOUtils;

import javax.json.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class JsonRequest extends HttpServletRequestWrapper {
    private static Logger logger = Logger.getLogger("dcapture.io");
    private JsonArray bodyArray;
    private JsonObject bodyObject;
    private JsonValue bodyValue;
    private Map<String, Object> valueMap;

    JsonRequest(HttpServletRequest request) {
        super(request);
        try {
            JsonReader reader = Json.createReader(request.getInputStream());
            JsonValue value = reader.readValue();
            if (value instanceof JsonObject) {
                bodyObject = (JsonObject) value;
            } else if (value instanceof JsonArray) {
                bodyArray = (JsonArray) value;
            } else {
                bodyValue = value;
            }
        } catch (Exception ex) {
            logger.severe(request.getPathInfo() + " >> json parser error : " + ex.getMessage());
        }
    }

    public String getHttpPath() {
        int serverPort = getServerPort();
        StringBuilder builder = new StringBuilder();
        builder.append(getScheme()).append("://").append(getServerName());
        if (serverPort != 80 && serverPort != 443) {
            builder.append(":").append(serverPort);
        }
        builder.append(getContextPath()).append(getServletPath());
        return builder.toString();
    }

    public JsonObject getJsonObject() {
        return bodyObject;
    }

    public JsonValue getJsonValue(String name) {
        return bodyObject == null ? null : bodyObject.get(name);
    }

    public JsonArray getJsonArray() {
        return bodyArray;
    }

    public JsonValue getJsonValue() {
        return bodyValue;
    }

    public String getContent() {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(getInputStream(), writer, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            logger.severe(getPathInfo() + " >> json parser error : " + ex.getMessage());
        }
        return writer.toString();
    }

    private long copyLarge(Reader input, Writer output, char[] buffer) throws IOException {
        long count;
        int n;
        for (count = 0L; -1 != (n = input.read(buffer)); count += (long) n) {
            output.write(buffer, 0, n);
        }
        return count;
    }

    public String getString(String key) {
        JsonValue value = getJsonValue(key);
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        }
        return null;
    }

    public String getString(String key, String defaultValue) {
        JsonValue value = getJsonValue(key);
        if (value instanceof JsonString) {
            String text = ((JsonString) value).getString().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return defaultValue;
    }

    public JsonObject getObject(String key) {
        JsonValue value = getJsonValue(key);
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        return null;
    }

    public long getLong(String key) {
        JsonValue value = getJsonValue(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).longValue();
        }
        return 0L;
    }

    public long getLong(String key, long defaultValue) {
        JsonValue value = getJsonValue(key);
        if (value instanceof JsonNumber) {
            long longValue = ((JsonNumber) value).intValue();
            if (0 != longValue) {
                return longValue;
            }
        }
        return defaultValue;
    }

    public int getInt(String key) {
        JsonValue value = getJsonValue(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).intValue();
        }
        return 0;
    }

    public int getInt(String key, int defaultValue) {
        JsonValue value = getJsonValue(key);
        if (value instanceof JsonNumber) {
            int intValue = ((JsonNumber) value).intValue();
            if (0 != intValue) {
                return intValue;
            }
        }
        return defaultValue;
    }

    public Object getSessionAttribute(String name) {
        return getSession(false).getAttribute(name);
    }

    public Map<String, Object> getValueMap() {
        if (bodyObject == null) {
            valueMap = new HashMap<>();
        }
        if (valueMap != null) {
            return valueMap;
        }
        valueMap = new HashMap<>();
        for (Map.Entry<String, JsonValue> entry : bodyObject.entrySet()) {
            valueMap.put(entry.getKey(), toObject(entry.getValue()));
        }
        return valueMap;
    }

    private Object toObject(JsonValue value) {
        if (value == null || JsonValue.ValueType.NULL.equals(value.getValueType())) {
            return null;
        } else if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        } else if (value instanceof JsonNumber) {
            return ((JsonNumber) value).doubleValue();
        } else if (value instanceof JsonObject) {
            return toObjectMap((JsonObject) value);
        } else if (value instanceof JsonArray) {
            return toObjectList((JsonArray) value);
        } else if (JsonValue.ValueType.FALSE.equals(value.getValueType())) {
            return false;
        } else if (JsonValue.ValueType.TRUE.equals(value.getValueType())) {
            return true;
        }
        return null;
    }

    private Map<String, Object> toObjectMap(JsonObject obj) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            map.put(entry.getKey(), toObject(entry.getValue()));
        }
        return map;
    }

    private List<Object> toObjectList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (JsonValue value : array) {
            list.add(toObject(value));
        }
        return list;
    }
}
