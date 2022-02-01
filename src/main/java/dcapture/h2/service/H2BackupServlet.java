package dcapture.h2.service;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.h2.tools.Backup;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class H2BackupServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(H2BackupServlet.class);
    private static final String ENCODING = "UTF-8";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] pathInfoArray = getPathInfoArray(req.getPathInfo());
        if (2 != pathInfoArray.length) {
            sendResponse(resp, "Service not supported " + Arrays.toString(pathInfoArray));
            return;
        }
        String actionId = pathInfoArray[0], appsName = pathInfoArray[1];
        String backupRoot = req.getServletContext().getInitParameter("backup_root");
        if ("load-backup".equals(actionId)) {
            String date1 = req.getParameter("date");
            showBackupList(resp, backupRoot, appsName, date1);
        } else if ("download".equals(actionId)) {
            String path = getDirectory(backupRoot, appsName);
            String date2 = req.getParameter("date");
            String database = req.getParameter("db");
            performDownload(resp, Paths.get(path), appsName, date2, database);
        } else if ("create".equals(actionId)) {
            String databaseRoot = req.getServletContext().getInitParameter("database_root");
            String type = req.getParameter("type");
            boolean isOffLine = !"online".equals(type);
            performCreateDatabase(resp, databaseRoot, backupRoot, appsName, isOffLine);
        } else {
            sendResponse(resp, "Service request not valid " + actionId);
        }
    }

    private void performCreateDatabase(HttpServletResponse resp, String databaseRoot, String backupRoot,
                                       String appsName, boolean isOffLine) throws IOException {
        if (isOffLine) {
            Map<String, String> databaseMap = getDatabaseUrls(appsName);
            try {
                for (String db : databaseMap.keySet()) {
                    String fileName = getBackupFileName(backupRoot, appsName, db);
                    logger.info("Create database backup for (" + appsName + "/" + db + ") to " + fileName);
                    Backup.execute(fileName, getDirectory(databaseRoot, appsName), db, false);
                }
                sendResponse(resp, "Database back up is created for " + appsName);
            } catch (SQLException ex1) {
                ex1.printStackTrace();
                sendResponse(resp, "Database back up error : " + appsName + "\n" + ex1.getMessage());
            }
        } else {
            sendResponse(resp, "Online database back up not yet implemented.");
        }
    }

    private void showBackupList(HttpServletResponse resp, String backupRoot, String appsName, String dateText) throws IOException {
        String backupDirectory = getDirectory(backupRoot, appsName);
        Path path = Paths.get(backupDirectory, dateText);
        if (!Files.exists(path)) {
            sendError(resp, "Database back up not created on " + dateText + " for " + appsName);
            return;
        }
        List<String> backupList = getBackupFileList(path.toString());
        StringBuilder builder = new StringBuilder();
        backupList.forEach(str -> builder.append(str).append(", "));
        sendResponse(resp, "text/plain", builder.toString());
    }

    private void performDownload(HttpServletResponse resp, Path backupFolder, String appsName, String date, String database)
            throws IOException {
        String pathText = getDirectory(backupFolder.toString(), date);
        Path path = Paths.get(pathText);
        if (!Files.exists(path)) {
            sendResponse(resp, "Database file not found at : " + date + "/" + database);
            return;
        }
        File downloadFile = new File(path.toString(), database + ".zip");
        if (!downloadFile.exists()) {
            sendResponse(resp, "Database backup not found at : " + date + "/" + database);
            return;
        }
        resp.setContentType("application/octet-stream");
        resp.setContentLength((int) downloadFile.length());
        resp.setHeader("Content-Disposition",
                String.format("attachment; filename=\"%s\"", appsName + "-" + database + "-" + date + ".zip"));
        try (OutputStream outputStream = resp.getOutputStream()) {
            try (FileInputStream inputStream = new FileInputStream(downloadFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            outputStream.flush();
        }
    }

    private void sendResponse(HttpServletResponse response, String bodyText) throws IOException {
        sendResponse(response, "text/html", bodyText);
    }

    private void sendResponse(HttpServletResponse response, String type, String bodyText) throws IOException {
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

    private void sendError(HttpServletResponse response, String bodyText) throws IOException {
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

    private Map<String, String> getDatabaseUrls(String appsName) {
        String prefix = "jdbc:h2:tcp://localhost/~/data/";
        Map<String, String> resultsMap = new HashMap<>();
        for (String db : getDatabaseNames()) {
            resultsMap.put(db, prefix + appsName + "/" + db);
        }
        return resultsMap;
    }

    private String[] getDatabaseNames() {
        return new String[]{"shared", "cashbook", "materials", "project", "inventory", "purchase", "sales"};
    }

    private String getDirectory(String root, String appsName) {
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

    private String getBackupFileName(String backupFolder, String appsName, String dbName) {
        String dir = getDirectory(backupFolder, appsName);
        dir = getDirectory(dir, dateFormat.format(new Date()));
        return Paths.get(dir, dbName + ".zip").toString();
    }

    private String[] getPathInfoArray(String pathInfo) {
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

    private List<String> getBackupFileList(String dir) throws IOException {
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
