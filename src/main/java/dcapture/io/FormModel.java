package dcapture.io;

import javax.json.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FormModel {
    private DateFormat dateFormat;
    private DateTimeFormatter dateFormatter, timeFormatter;
    private Map<String, Object> values;

    public FormModel() {
        values = new HashMap<>();
        setDateFormat("yyyy-MM-dd");
    }

    public FormModel(JsonObject obj) {
        values = new HashMap<>();
        setDateFormat("yyyy-MM-dd");
        if (obj != null) {
            for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                values.put(entry.getKey(), getObject(entry.getValue()));
            }
        }
    }

    public Object getValue(String name) {
        return values.get(name);
    }

    public String getStringSafe(String name) {
        return isString(name) ? (String) values.get(name) : "";
    }

    public String getString(String name) {
        return (String) values.get(name);
    }

    public int getIntSafe(String name) {
        Object value = getValue(name);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return 0;
    }

    public int getInt(String name) {
        return (Integer) values.get(name);
    }

    public long getLongSafe(String name) {
        Object value = getValue(name);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            return ((Double) value).longValue();
        }
        return 0;
    }

    public long getLong(String name) {
        return (Long) values.get(name);
    }

    public void setValue(String name, Object value) {
        values.put(name, value);
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values == null ? new HashMap<>() : values;
    }

    private String format(Date date) {
        return dateFormat.format(date);
    }

    private String format(LocalDate localDate) {
        return dateFormatter.format(localDate);
    }

    public void setDateFormat(String pattern) {
        this.dateFormat = new SimpleDateFormat(pattern);
        this.dateFormatter = DateTimeFormatter.ofPattern(pattern);
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormatter = DateTimeFormatter.ofPattern(timeFormat);
    }

    public String formatTime(LocalDateTime dateTime) {
        if (timeFormatter == null) {
            timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
        }
        return timeFormatter.format(dateTime);
    }

    public Object getObject(JsonValue value) {
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

    private Map<String, Object> getObjectMap(JsonObject obj) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            map.put(entry.getKey(), getObject(entry.getValue()));
        }
        return map;
    }

    private List<Object> getObjectList(JsonArray array) {
        List<Object> list = new ArrayList<>();
        for (JsonValue value : array) {
            list.add(getObject(value));
        }
        return list;
    }

    public boolean isValue(String name) {
        return values.containsKey(name);
    }

    public boolean isString(String name) {
        Object value = getValue(name);
        return value instanceof String && !((String) value).trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormModel)) return false;
        FormModel formModel = (FormModel) o;
        return Objects.equals(toString(), formModel.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    private JsonValue getJsonValue(Object value) {
        if (value == null) {
            return JsonValue.NULL;
        } else if (value instanceof String) {
            return Json.createValue((String) value);
        } else if (value instanceof Number) {
            if (value instanceof Double) {
                return Json.createValue((Double) value);
            } else if (value instanceof Long) {
                return Json.createValue((Long) value);
            } else if (value instanceof Integer) {
                return Json.createValue((Integer) value);
            } else if (value instanceof BigDecimal) {
                return Json.createValue((BigDecimal) value);
            }
            return Json.createValue(((Number) value).doubleValue());
        } else if (value instanceof Date) {
            return Json.createValue(format((Date) value));
        } else if (value instanceof LocalDate) {
            return Json.createValue(format((LocalDate) value));
        } else if (value instanceof LocalDateTime) {
            return Json.createValue(formatTime((LocalDateTime) value));
        }
        throw new IllegalArgumentException("Form model unknown data type : " + value.getClass());
    }

    @Override
    public String toString() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            builder.add(entry.getKey(), getJsonValue(entry.getValue()));
        }
        return builder.toString();
    }
}
