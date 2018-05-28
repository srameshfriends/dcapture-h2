package dcapture.io;

import javax.json.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class JsonMapper {

    private static Path initialize(File file) throws IOException {
        if (!file.exists()) {
            Files.createDirectories(file.toPath().getParent());
            return Files.createFile(file.toPath());
        }
        return file.toPath();
    }

    private static void initialize(File file, JsonValue value) throws IOException {
        if (!file.exists()) {
            Path path = initialize(file);
            JsonWriter writer = Json.createWriter(new FileOutputStream(path.toFile()));
            writer.write(value);
            writer.close();
        }
    }

    public static JsonStructure read(File file) throws IOException {
        initialize(file, Json.createObjectBuilder().build());
        JsonReader reader = Json.createReader(new FileInputStream(file));
        return reader.read();
    }

    public static JsonObject readObject(File file) throws IOException {
        initialize(file, Json.createObjectBuilder().build());
        JsonReader reader = Json.createReader(new FileInputStream(file));
        JsonObject result = reader.readObject();
        reader.close();
        return result;
    }

    public static JsonValue readFromObject(File sourceFile, String pointer) throws IOException {
        JsonPointer replacePointer = Json.createPointer(pointer);
        return replacePointer.getValue(readObject(sourceFile));
    }

    public static JsonArray readArray(File parent, String child) throws IOException {
        return readArray(new File(parent, child));
    }

    public static JsonArray readArray(File file) throws IOException {
        initialize(file, Json.createArrayBuilder().build());
        JsonReader reader = Json.createReader(new FileInputStream(file));
        JsonArray result = reader.readArray();
        reader.close();
        return result;
    }

    public static void write(File file, JsonArray array) throws IOException {
        initialize(file, Json.createArrayBuilder().build());
        if (array != null) {
            JsonWriter writer = Json.createWriter(new FileOutputStream(file));
            writer.write(array);
            writer.close();
        }
    }

    public static void write(File file, JsonObject object) throws IOException {
        initialize(file, Json.createObjectBuilder().build());
        if (object != null) {
            JsonWriter writer = Json.createWriter(new FileOutputStream(file));
            writer.write(object);
            writer.close();
        }
    }

    public static void replace(File sourceFile, String pointer, JsonValue value) throws IOException {
        initialize(sourceFile);
        JsonPointer replacePointer = Json.createPointer(pointer);
        JsonObject result = replacePointer.replace(readObject(sourceFile), value);
        write(sourceFile, result);
    }


    public static String getString(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        }
        return null;
    }

    public static String getString(JsonObject obj, String key, String safeDefault) {
        String value = getString(obj, key);
        return value == null ? safeDefault : value;
    }

    public static JsonObject getJsonObject(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        return null;
    }

    public static JsonArray getJsonArray(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        }
        return null;
    }

    public static <T> List<T> getList(JsonObject obj, String name) {
        JsonValue value = obj.get(name);
        if (!(value instanceof JsonArray)) {
            return new ArrayList<>();
        }
        return getList((JsonArray) value);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(JsonArray array) {
        List<T> resultList = new ArrayList<>();
        for (JsonValue value : array) {
            Object result;
            if (value instanceof JsonString) {
                result = ((JsonString) value).getString();
                resultList.add((T) result);
            } else if (value instanceof JsonNumber) {
                result = ((JsonNumber) value).doubleValue();
                resultList.add((T) result);
            } else if (value instanceof JsonObject || value instanceof JsonArray) {
                resultList.add((T) value);
            } else {
                throw new IllegalArgumentException("Type conversion not yet implemented");
            }
        }
        return resultList;
    }

    public static int getInt(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).intValue();
        }
        return 0;
    }

    public static long getLong(JsonObject obj, String key) {
        JsonValue value = obj.get(key);
        if (value instanceof JsonNumber) {
            return ((JsonNumber) value).longValue();
        }
        return 0L;
    }

    public static JsonObject getJsonObject(Properties prop) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (prop == null) {
            return builder.build();
        }
        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            builder.add(entry.getKey().toString(), entry.getValue().toString());
        }
        return builder.build();
    }

    public static Object getObject(JsonValue value) {
        if (value == null || JsonValue.ValueType.NULL.equals(value.getValueType())) {
            return null;
        } else if (value instanceof JsonString) {
            return ((JsonString) value).getString();
        } else if (value instanceof JsonNumber) {
            return ((JsonNumber) value).doubleValue();
        } else if (value instanceof JsonObject) {
            return getObjectMap((JsonObject) value);
        } else if (value instanceof JsonArray) {
            return getObjectList((JsonArray) value);
        } else if (JsonValue.ValueType.FALSE.equals(value.getValueType())) {
            return false;
        } else if (JsonValue.ValueType.TRUE.equals(value.getValueType())) {
            return true;
        }
        return null;
    }

    public static Map<String, Object> getObjectMap(JsonObject obj) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            map.put(entry.getKey(), getObject(entry.getValue()));
        }
        return map;
    }

    public static List<Object> getObjectList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (JsonValue value : array) {
            list.add(getObject(value));
        }
        return list;
    }
}
