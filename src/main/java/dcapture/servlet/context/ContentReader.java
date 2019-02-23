package dcapture.servlet.context;

import dcapture.api.io.CsvRequest;
import dcapture.api.io.FormRequest;
import dcapture.api.io.HtmlRequest;
import dcapture.api.io.JsonRequest;
import dcapture.api.sql.SqlContext;
import dcapture.api.support.MessageException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.json.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class ContentReader {
    private final HttpServletRequest request;
    private Map<String, String[]> parameters;
    private JsonObject jsonObject;
    private JsonArray jsonArray;
    private List<CSVRecord> csvRecords;
    private Map<String, Integer> csvHeaders;
    private String text;
    private final String pathInfo;

    ContentReader(HttpServletRequest request, String pathInfo, String method, String contentType) throws Exception {
        this.request = request;
        this.pathInfo = pathInfo;
        if ("GET".equals(method)) {
            parameters = request.getParameterMap();
        } else if ("POST".equals(method) || "DELETE".equals(method)) {
            if (contentType.contains("json")) {
                JsonStructure json = Json.createReader(request.getInputStream()).read();
                if (json instanceof JsonObject) {
                    jsonObject = (JsonObject) json;
                } else if (json instanceof JsonArray) {
                    jsonArray = (JsonArray) json;
                } else {
                    throw new MessageException("application.json.error", new Object[]{pathInfo});
                }
            } else if (contentType.contains("csv")) {
                CSVParser parser = CSVParser.parse(request.getInputStream(), StandardCharsets.UTF_8,
                        CSVFormat.DEFAULT.withHeader());
                csvRecords = parser.getRecords();
                csvHeaders = parser.getHeaderMap();
            } else if ("application/x-www-form-urlencoded".equals(contentType)) {
                parameters = request.getParameterMap();
            } else if (contentType.contains("text")) {
                BufferedReader reader = request.getReader();
                final char[] buffer = new char[4 * 1024];
                int len;
                final StringBuilder builder = new StringBuilder();
                while ((len = reader.read(buffer, 0, buffer.length)) != -1) {
                    builder.append(buffer, 0, len);
                }
                text = builder.toString();
            } else {
                throw new MessageException("application.content.error", new Object[]{pathInfo});
            }
        } else {
            throw new MessageException("application.httpMethod.error", new Object[]{pathInfo});
        }
    }

    Object getMethodParameter(SqlContext sqlContext, Class<?> pCls, HttpServletResponse response) throws MessageException {
        if (JsonRequest.class.equals(pCls)) {
            if (jsonObject != null) {
                return new JsonRequest(request, sqlContext, jsonObject);
            } else if (jsonArray != null) {
                return new JsonRequest(request, sqlContext, jsonArray);
            } else {
                throw new MessageException("application.json.type.error", new Object[]{pathInfo});
            }
        } else if (CsvRequest.class.equals(pCls)) {
            return new CsvRequest(request, sqlContext, csvRecords, csvHeaders);
        } else if (FormRequest.class.equals(pCls)) {
            return new FormRequest(request, sqlContext, parameters);
        } else if (HtmlRequest.class.equals(pCls)) {
            return new HtmlRequest(request, text);
        } else if (HttpServletResponse.class.equals(pCls) || HttpServletResponseWrapper.class.equals(pCls)) {
            return response;
        } else if (HttpServletRequest.class.equals(pCls) || HttpServletRequestWrapper.class.equals(pCls)) {
            return request;
        }
        throw new MessageException("application.parameter.error", new Object[]{pathInfo});
    }
}
