package dcapture.h2.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.h2.tools.Backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class H2BackupServlet extends MasterHttpServlet {
    private static final Logger logger = Logger.getLogger(H2BackupServlet.class.getSimpleName());

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
                    String fileName = getDBFileName(backupRoot, appsName, db);
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
        List<String> backupList = getDBFileList(path.toString());
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
}
