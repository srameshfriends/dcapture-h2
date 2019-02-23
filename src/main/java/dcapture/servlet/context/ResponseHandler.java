package dcapture.servlet.context;

import dcapture.api.io.*;
import dcapture.api.sql.*;
import dcapture.api.support.Messages;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;

import javax.json.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ResponseHandler extends HttpServletResponseWrapper {
    private static final Logger logger = Logger.getLogger(ResponseHandler.class);
    private static final int SC_OK = 200;
    private static final int SC_BAD_REQUEST = 400;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm");
    private static final DateTimeFormatter attachmentHourFormat = DateTimeFormatter.ofPattern("yyyy MM dd hh");
    private DateTimeFormatter dateFormat, dateTimeFormat, timeFormat;
    private SqlContext sqlContext;
    private Messages messages;
    private final String pathInfo;
    private String attachment;

    ResponseHandler(HttpServletResponse res, String pathInfo) {
        super(res);
        this.pathInfo = pathInfo;
    }

    public void setDateFormat(DateTimeFormatter dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void setDateTimeFormat(DateTimeFormatter dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }

    public void setTimeFormat(DateTimeFormatter timeFormat) {
        this.timeFormat = timeFormat;
    }

    void setSqlContext(SqlContext sqlContext) {
        this.sqlContext = sqlContext;
    }

    void setMessages(Messages messages) {
        this.messages = messages;
    }

    private void sendJsonObject(JsonObject json) throws IOException {
        setCharacterEncoding("UTF-8");
        setContentType("application/json");
        setStatus(SC_OK);
        if (attachment != null) {
            setHeader("Content-disposition", attachment);
        }
        if (json != null) {
            JsonWriter writer = Json.createWriter(getWriter());
            writer.writeObject(json);
            writer.close();
        } else {
            getWriter().write("{}");
            getWriter().flush();
        }
    }

    private void sendJsonArray(JsonArray json) throws IOException {
        setCharacterEncoding("UTF-8");
        setContentType("application/json");
        setStatus(SC_OK);
        if (attachment != null) {
            setHeader("Content-disposition", attachment);
        }
        if (json != null) {
            JsonWriter writer = Json.createWriter(getWriter());
            writer.writeArray(json);
            writer.close();
        } else {
            getWriter().write("[]");
            getWriter().flush();
        }
    }

    private void sendCsv(SqlResult result, String[] header) throws IOException {
        setCharacterEncoding("UTF-8");
        setContentType("text/csv");
        setStatus(SC_OK);
        if (attachment != null) {
            setHeader("Content-disposition", attachment);
        }
        String[] headerArray;
        if (header == null) {
            headerArray = new String[result.getMetaDataList().size()];
            int index = 0;
            for (SqlMetaData smd : result.getMetaDataList()) {
                headerArray[index] = smd.getTable() + "." + smd.getColumn();
                index += 1;
            }
        } else {
            headerArray = header;
        }
        CSVPrinter printer = new CSVPrinter(getWriter(), CSVFormat.DEFAULT.withHeader(headerArray));
        for (Object[] values : result.getObjectsList()) {
            printer.printRecord(values);
        }
        printer.flush();
    }

    private void sendCsv(List<DataSet> dataSetList, String[] columns, String[] header) throws IOException {
        setCharacterEncoding("UTF-8");
        setContentType("text/csv");
        setStatus(SC_OK);
        if (attachment != null) {
            setHeader("Content-disposition", attachment);
        }
        String[] headerArray;
        if (header == null) {
            headerArray = columns;
        } else {
            headerArray = header;
        }
        CSVPrinter printer = new CSVPrinter(getWriter(), CSVFormat.DEFAULT.withHeader(headerArray));
        for (DataSet dataSet : dataSetList) {
            printer.printRecord(getCsvRecords(dataSet, columns));
        }
        printer.flush();
    }

    private JsonObjectBuilder getObjectBuilder(DataSet dataSet) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, Object> entry : dataSet.get().entrySet()) {
            addJsonValue(builder, entry.getKey(), entry.getValue());
        }
        return builder;
    }

    private JsonObject getJsonObject(DataSet dataSet, String tableName, String[] columns) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        SqlTable sqlTable = sqlContext.getSqlTable(tableName);
        if (sqlTable == null) {
            return getObjectBuilder(dataSet, columns).build();
        }
        if (columns == null || 0 == columns.length) {
            for (String column : sqlTable.getColumns()) {
                addJsonValue(builder, column, dataSet.get(column));
            }
        } else {
            for (String column : columns) {
                addJsonValue(builder, sqlTable.getColumn(column), dataSet.get(column));
            }
        }
        return builder.build();
    }

    private JsonObjectBuilder getObjectBuilder(DataSet dataSet, SqlTable sqlTable, String[] columns) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (sqlTable == null) {
            return getObjectBuilder(dataSet, columns);
        }
        if (columns == null || 0 == columns.length) {
            for (String column : sqlTable.getColumns()) {
                addJsonValue(builder, column, dataSet.get(column));
            }
        } else {
            for (String column : columns) {
                addJsonValue(builder, sqlTable.getColumn(column), dataSet.get(column));
            }
        }
        return builder;
    }

    private JsonObjectBuilder getObjectBuilder(DataSet dataSet, SqlTable sqlTable) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (SqlColumn sqlColumn : sqlTable.getSqlColumns()) {
            addJsonValue(builder, sqlColumn, dataSet.get(sqlColumn.getName()));
        }
        return builder;
    }

    private JsonObjectBuilder getObjectBuilder(DataSet dataSet, String[] columns) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (String field : columns) {
            addJsonValue(builder, field, dataSet.get(field));
        }
        return builder;
    }

    private void addJsonValue(JsonObjectBuilder builder, SqlColumn col, Object value) {
        if (value != null) {
            if (value instanceof DataSet) {
                SqlTable refTable = sqlContext.getSqlTable(col.getReference());
                if (refTable == null) {
                    throw new NullPointerException(col.toString() + " : Sql Column reference is table is null");
                }
                builder.add(col.getName(), getObjectBuilder((DataSet) value, refTable));
            } else if (value instanceof String) {
                builder.add(col.getName(), (String) value);
            } else if (value instanceof Integer) {
                builder.add(col.getName(), (Integer) value);
            } else if (value instanceof Long) {
                builder.add(col.getName(), (Long) value);
            } else if (value instanceof Double) {
                builder.add(col.getName(), (Double) value);
            } else if (value instanceof BigDecimal) {
                builder.add(col.getName(), (BigDecimal) value);
            } else if (value instanceof Boolean) {
                builder.add(col.getName(), (Boolean) value);
            } else if (value instanceof java.sql.Date) {
                String text = getString((java.sql.Date) value, col.getModel());
                builder.add(col.getName(), text);
            } else if (value instanceof LocalDate) {
                builder.add(col.getName(), getString((LocalDate) value));
            } else if (value instanceof LocalDateTime) {
                builder.add(col.getName(), getString((LocalDateTime) value));
            } else if (value instanceof LocalTime) {
                builder.add(col.getName(), getString((LocalTime) value));
            } else {
                throw new IllegalArgumentException(
                        col.toString() + " :  Entity to json format not yet implemented! " + value);
            }
        } else if (col.getReference() != null) {
            builder.add(col.getName(), Json.createObjectBuilder().build());
        } else {
            switch (col.getType()) {
                case Types.VARCHAR:
                    builder.add(col.getName(), "");
                    break;
                case Types.INTEGER:
                case Types.BIGINT:
                    builder.add(col.getName(), 0);
                    break;
                case Types.DOUBLE:
                case Types.DECIMAL:
                    builder.add(col.getName(), 0D);
                    break;
                case Types.BOOLEAN:
                    builder.add(col.getName(), false);
                    break;
                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    builder.add(col.getName(), "");
                    break;
                default:
                    throw new IllegalArgumentException(
                            col.toString() + " :  Entity to json format not yet implemented! " + col.getType());
            }
        }
    }

    private void addJsonValue(JsonObjectBuilder builder, String name, Object value) {
        if (value == null) {
            builder.add(name, JsonValue.NULL);
        } else if (value instanceof DataSet) {
            builder.add(name, getObjectBuilder((DataSet) value));
        } else if (value instanceof String) {
            builder.add(name, (String) value);
        } else if (value instanceof Integer) {
            builder.add(name, (Integer) value);
        } else if (value instanceof Long) {
            builder.add(name, (Long) value);
        } else if (value instanceof Double) {
            builder.add(name, (Double) value);
        } else if (value instanceof BigDecimal) {
            builder.add(name, (BigDecimal) value);
        } else if (value instanceof Boolean) {
            builder.add(name, (Boolean) value);
        } else if (value instanceof java.sql.Date) {
            String text = getString((java.sql.Date) value, LocalDateTime.class);
            builder.add(name, text);
        } else if (value instanceof LocalDate) {
            builder.add(name, getString((LocalDate) value));
        } else if (value instanceof LocalDateTime) {
            builder.add(name, getString((LocalDateTime) value));
        } else if (value instanceof LocalTime) {
            builder.add(name, getString((LocalTime) value));
        } else {
            throw new IllegalArgumentException(
                    name + " :  DataSet to json format not yet implemented! " + value);
        }
    }

    private String getString(java.sql.Date date, Class<?> model) {
        if (LocalDate.class.equals(model)) {
            return dateFormat == null ? DATE_FORMAT.format(date.toLocalDate()) : dateFormat.format(date.toLocalDate());
        } else if (LocalDateTime.class.equals(model)) {
            return dateTimeFormat == null ? DATETIME_FORMAT.format(date.toLocalDate().atStartOfDay()) :
                    dateTimeFormat.format(date.toLocalDate().atStartOfDay());
        } else if (LocalTime.class.equals(model)) {
            return timeFormat == null ? TIME_FORMAT.format(date.toLocalDate().atStartOfDay()) :
                    timeFormat.format(date.toLocalDate().atStartOfDay());
        }
        return "";
    }

    private String getString(LocalDate date) {
        return dateFormat == null ? DATE_FORMAT.format(date) : dateFormat.format(date);
    }

    private String getString(LocalDateTime date) {
        return dateTimeFormat == null ? DATETIME_FORMAT.format(date) : dateTimeFormat.format(date);
    }

    private String getString(LocalTime localTime) {
        return timeFormat == null ? TIME_FORMAT.format(localTime) : timeFormat.format(localTime);
    }

    private Object[] getCsvRecords(DataSet dataSet, String[] columns) {
        Object[] values = new Object[columns.length];
        int index = 0;
        for (String col : columns) {
            values[index] = dataSet.get(col);
        }
        return values;
    }

    private void sendJsonObject(Paging paging, String tableName, String[] columns) throws IOException {
        if (paging != null) {
            JsonArrayBuilder array = Json.createArrayBuilder();
            if (paging.getDataList() != null) {
                SqlTable sqlTable = sqlContext.getSqlTable(tableName);
                for (DataSet dataSet : paging.getDataList()) {
                    array.add(getObjectBuilder(dataSet, sqlTable, columns));
                }
            }
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("limit", paging.getLimit());
            builder.add("offset", paging.getOffset());
            builder.add("orderBy", paging.getOrderBy() == null ? "" : paging.getOrderBy());
            builder.add("totalRecords", paging.getTotalRecords());
            builder.add(paging.getName(), array);
            builder.add("size", paging.getDataList() == null ? 0 : paging.getDataList().size());
            sendJsonObject(builder.build());
        } else {
            sendJsonObject(null);
        }
    }

    private void sendJsonArray(List<DataSet> dataSetList, String tableName, String[] columns) throws IOException {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        if (dataSetList != null) {
            SqlTable sqlTable = sqlContext.getSqlTable(tableName);
            for (DataSet dataSet : dataSetList) {
                builder.add(getObjectBuilder(dataSet, sqlTable, columns));
            }
            sendJsonArray(builder.build());
        } else {
            sendJsonArray(null);
        }
    }

    private void send(int status, String text) {
        if (isCommitted()) {
            logger.debug(status + " : RESPONSE COMMITTED ERROR " + text);
            return;
        }
        try {
            setContentType("text/html");
            setCharacterEncoding("UTF-8");
            setStatus(status);
            if (text != null) {
                setContentLength(text.length());
                getWriter().write(text);
            } else {
                setContentLength(0);
                getWriter().write("");
            }
            getWriter().close();
        } catch (Exception ex) {
            logger.debug(status + " : ERROR " + text);
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
        }
    }

    private void send(ServletResult tmg) {
        if (tmg.getMessageCode() != null && messages != null) {
            send(tmg.getStatus(), messages.getMessage(tmg.getMessageCode(), tmg.getArguments()));
        } else {
            send(tmg.getStatus(), tmg.getMessage());
        }
    }

    private void setAttachment(JsonResult result) {
        if (result.getAttachmentName() != null) {
            String fileFormat = ".json", timeText = "-" + attachmentHourFormat.format(LocalDateTime.now());
            timeText = timeText.replaceAll("\\s", "");
            attachment = "attachment; filename=" + result.getAttachmentName() + timeText + fileFormat;
        }
    }

    private void setAttachment(CsvResult result) {
        if (result.getAttachmentName() != null) {
            String fileFormat = ".csv", timeText = "-" + attachmentHourFormat.format(LocalDateTime.now());
            timeText = timeText.replaceAll("\\s", "");
            attachment = "attachment; filename=" + result.getAttachmentName() + timeText + fileFormat;
        }
    }

    void send(Dispatcher dispatcher, Object invoked) throws IOException {
        if (invoked == null) {
            send(SC_OK, "Unknown error : " + dispatcher.getPath());
        } else if (invoked instanceof JsonResult) {
            JsonResult jsr = (JsonResult) invoked;
            setAttachment(jsr);
            if (String.class.equals(jsr.getSourceType())) {
                send(jsr);
            } else if (Paging.class.equals(jsr.getSourceType())) {
                sendJsonObject(jsr.getPaging(), jsr.getTableName(), jsr.getColumns());
            } else if (DataSet.class.equals(jsr.getSourceType())) {
                sendJsonObject(getJsonObject(jsr.getDataSet(), jsr.getTableName(), jsr.getColumns()));
            } else if (List.class.equals(jsr.getSourceType())) {
                sendJsonArray(jsr.getDataSetList(), jsr.getTableName(), jsr.getColumns());
            } else if (JsonObject.class.equals(jsr.getSourceType())) {
                sendJsonObject(jsr.getJsonObject());
            } else if (JsonArray.class.equals(jsr.getSourceType())) {
                sendJsonArray(jsr.getJsonArray());
            } else if (SqlResult.class.equals(jsr.getSourceType())) {
                sendJsonArray(jsr.getDataSetList(), jsr.getTableName(), jsr.getColumns());
            } else {
                error("application.response.type.error", pathInfo);
            }
        } else if (invoked instanceof CsvResult) {
            CsvResult cvr = (CsvResult) invoked;
            setAttachment(cvr);
            if (SqlResult.class.equals(cvr.getSourceType())) {
                sendCsv(cvr.getSqlResult(), cvr.getCsvHeader());
            } else if (List.class.equals(cvr.getSourceType())) {
                sendCsv(cvr.getDataSetList(), cvr.getColumns(), cvr.getCsvHeader());
            } else {
                error("application.response.type.error", pathInfo);
            }
        } else if (invoked instanceof ServletResult) {
            send((ServletResult) invoked);
        } else {
            if (!void.class.equals(dispatcher.getMethod().getReturnType())) {
                error("application.response.type.error", pathInfo);
            }
        }
    }

    void error(String messageCode) {
        send(SC_BAD_REQUEST, messages.getMessage(messageCode));
    }

    void error(String messageCode, Object... args) {
        send(SC_BAD_REQUEST, messages.getMessage(messageCode, args));
    }

    void errorMessage(String message) {
        send(SC_BAD_REQUEST, message);
    }
}