package dcapture.h2.service;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class MasterHttpServlet extends HttpServlet {
    protected static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    protected static final String ENCODING = "UTF-8";

    protected String[] getPathInfoArray(String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty()) {
            return new String[]{};
        }
        if (pathInfo.endsWith("/")) {
            pathInfo = pathInfo.substring(0, pathInfo.lastIndexOf("/"));
        }
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        return pathInfo.split("/");
    }

    protected void sendResponse(HttpServletResponse response, String bodyText) throws IOException {
        sendResponse(response, "text/html", bodyText);
    }

    protected void sendResponse(HttpServletResponse response, String type, String bodyText) throws IOException {
        response.setCharacterEncoding(ENCODING);
        response.setContentType(type);
        response.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter printWriter = response.getWriter()) {
            if (bodyText != null) {
                response.setContentLength(bodyText.length());
                printWriter.write(bodyText);
            }
            printWriter.flush();
        }
    }

    protected void sendError(HttpServletResponse response, String bodyText) throws IOException {
        response.setCharacterEncoding(ENCODING);
        response.setContentType("text/plan");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        try (PrintWriter printWriter = response.getWriter()) {
            if (bodyText != null) {
                response.setContentLength(bodyText.length());
                printWriter.write(bodyText);
            }
            printWriter.flush();
        }
    }

    protected Map<String, String> getDatabaseUrlByModules(String appsName) {
        String prefix = "jdbc:h2:tcp://localhost/~/data/";
        Map<String, String> resultsMap = new HashMap<>();
        for (String db : getDatabaseNames()) {
            resultsMap.put(db, prefix + appsName + "/" + db);
        }
        return resultsMap;
    }

    protected String getDatabaseUrl(String appsName) {
        return "jdbc:h2:tcp://localhost/~/data/" + appsName;
    }

    protected String[] getDatabaseNames() {
        return new String[]{"shared", "cashbook", "materials", "project", "inventory", "purchase", "sales"};
    }

    protected String getDirectory(String root, String appsName) {
        Path path = Paths.get(root, appsName);
        String msg;
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            msg = ex.getMessage();
        }
        throw new NullPointerException(msg);
    }

    protected String getDBFileNameByModule(String backupFolder, String appsName, String dbName) {
        String dir = getDirectory(backupFolder, appsName);
        dir = getDirectory(dir, dateFormat.format(new Date()));
        return Paths.get(dir, dbName + ".zip").toString();
    }

    protected String getDBFileName(String backupFolder, String appsName) {
        String dir = getDirectory(backupFolder, appsName);
        dir = getDirectory(dir, dateFormat.format(new Date()));
        return Paths.get(dir, appsName + ".zip").toString();
    }

    protected List<String> getDBFileList(String dir) throws IOException {
        List<String> fileList = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    fileList.add(path.toString().replace(dir, "").replace(File.separator, ""));
                }
            }
        }
        return fileList;
    }
}
