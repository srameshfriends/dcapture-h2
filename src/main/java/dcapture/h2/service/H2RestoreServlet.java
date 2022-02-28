package dcapture.h2.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.h2.tools.Restore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class H2RestoreServlet extends MasterHttpServlet {
    private static final Logger logger = Logger.getLogger(H2RestoreServlet.class.getSimpleName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] pathInfoArray = getPathInfoArray(req.getPathInfo());
        if (2 != pathInfoArray.length) {
            sendResponse(resp, "Service not supported " + Arrays.toString(pathInfoArray));
            return;
        }
        String actionId = pathInfoArray[0], appsName = pathInfoArray[1];
        String backupRoot = req.getServletContext().getInitParameter("backup_root");
        if ("execute".equals(actionId)) {
            String databaseRoot = req.getServletContext().getInitParameter("database_root");
            databaseRoot = getDirectory(databaseRoot, appsName);
            String backUpRoot = getDirectory(backupRoot, appsName);
            String date2 = req.getParameter("date");
            performRestore(resp, appsName, Paths.get(backUpRoot), date2, Paths.get(databaseRoot));
        } else {
            sendResponse(resp, "Service request not valid " + actionId);
        }
    }

    private void performRestore(HttpServletResponse resp, String appsName, Path backupFolder, String date, Path databaseRoot)
            throws IOException {
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
            if (logger.isLoggable(Level.ALL)) {
                ex.printStackTrace();
            }
            sendError(resp, "Application restore error : " + ex.getMessage());
        }
    }
}
