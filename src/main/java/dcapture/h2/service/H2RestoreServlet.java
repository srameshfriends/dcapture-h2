package dcapture.h2.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.h2.tools.Restore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class H2RestoreServlet extends MasterHttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(H2RestoreServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] pathInfoArray = getPathInfoArray(req.getPathInfo());
        if (2 != pathInfoArray.length) {
            sendResponse(resp, "Service not supported " + Arrays.toString(pathInfoArray));
            return;
        }
        String tempText = req.getParameter("is_single_database");
        boolean isSingleDatabase = "true".equalsIgnoreCase(tempText);
        String actionId = pathInfoArray[0], appsName = pathInfoArray[1];
        String backupRoot = req.getServletContext().getInitParameter("database.backup");
        if ("execute".equals(actionId)) {
            String databaseRoot = req.getServletContext().getInitParameter("database.data");
            databaseRoot = getDirectory(databaseRoot, appsName);
            String backUpRoot = getDirectory(backupRoot, appsName);
            String date2 = req.getParameter("date");
            if (isSingleDatabase) {
                performRestore(resp, appsName, Paths.get(backUpRoot), date2, Paths.get(databaseRoot));
            } else {
                performRestoreByModule(resp, appsName, Paths.get(backUpRoot), date2, Paths.get(databaseRoot));
            }
        } else {
            sendResponse(resp, "Service request not valid " + actionId);
        }
    }

    private void performRestoreByModule(HttpServletResponse resp, String appsName, Path backupFolder,
                                        String date, Path databaseRoot) throws IOException {
        String pathText = getDirectory(backupFolder.toString(), date);
        Path path = Paths.get(pathText);
        if (!Files.exists(path)) {
            sendResponse(resp, "Application backup not found on : " + date);
            return;
        }
        Path restoreDatePath = backupFolder.resolve(date);
        if (!Files.exists(restoreDatePath)) {
            sendResponse(resp, "Application backup not found at expected date : " + date);
            return;
        }
        Map<String, Path> databasePathMap = new HashMap<>();
        for (String database : getDatabaseNames()) {
            Path databasePath = restoreDatePath.resolve(database + ".zip");
            if (!Files.exists(databasePath)) {
                sendResponse(resp, "Application database (" + database + ") backup not found to restore.");
                return;
            }
            databasePathMap.put(database, databasePath);
        }
        try {
            StringBuilder builder = new StringBuilder(appsName);
            builder.append(" : Application databases restored at ").append(date).append("\n");
            for (Map.Entry<String, Path> entry : databasePathMap.entrySet()) {
                Restore.main("-dir", databaseRoot.toString(),
                        "-file", entry.getValue().toString(), "-db", entry.getKey());
                builder.append(entry.getKey()).append(", ");
            }
            if (0 > builder.lastIndexOf(", ")) {
                builder.replace(builder.length() - 2, builder.length(), "");
            }
            logger.info(builder.toString());
            sendResponse(resp, builder.toString());
        } catch (SQLException ex) {
            logger.info(ex.getMessage());
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
            sendError(resp, "Application restore error : " + ex.getMessage());
        }
    }

    private void performRestore(HttpServletResponse resp, String appsName, Path backupFolder,
                                String date, Path databaseRoot) throws IOException {
        String pathText = getDirectory(backupFolder.toString(), date);
        Path path = Paths.get(pathText);
        if (!Files.exists(path)) {
            sendResponse(resp, "Application backup not found on : " + date);
            return;
        }
        Path restoreDatePath = backupFolder.resolve(date);
        if (!Files.exists(restoreDatePath)) {
            sendResponse(resp, "Application backup not found at expected date : " + date);
            return;
        }
        Path databasePath = restoreDatePath.resolve(appsName + ".zip");
        if (!Files.exists(databasePath)) {
            sendResponse(resp, "Application database (" + appsName + ") backup not found to restore.");
            return;
        }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String msg = appsName + " : Application databases restored at " + simpleDateFormat.format(date);
            Restore.main("-dir", databaseRoot.toString(),
                    "-file", databasePath.toString(), "-db", appsName);
            sendResponse(resp, msg);
        } catch (SQLException ex) {
            logger.info(ex.getMessage());
            if (logger.isDebugEnabled()) {
                ex.printStackTrace();
            }
            sendError(resp, "Application restore error : " + ex.getMessage());
        }
    }
}
