package dcapture.h2.service;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

public class H2ServiceServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(H2ServiceServlet.class);
    private static final String ENCODING = "UTF-8";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if(req.getPathInfo() == null) {
            sendResponse(resp, "Service not supported.");
        } else if("/start".equals(req.getPathInfo())) {
            String info1 = H2ContextListener.startDatabaseService(req.getServletContext());
            if(info1 == null) {
                info1 = "H2 database started.";
            }
            logger.info(info1);
            sendResponse(resp, info1);
        }  else if("/status".equals(req.getPathInfo())) {
            String[] info3 = H2ContextListener.statusDatabaseService(req.getServletContext());
            sendResponse(resp, info3[0] + "\n" + info3[1]);
        } else if("/stop".equals(req.getPathInfo())) {
            String info2 = H2ContextListener.stopDatabaseService(req.getServletContext());
            if(info2 == null) {
                info2 = "H2 database stopped.";
            }
            logger.info(info2);
            sendResponse(resp, info2);
        } else if("/create".equals(req.getPathInfo())) {
            String name = req.getParameter("name");
            String singleDatabase = req.getParameter("is_single_database");
            boolean isSingleDatabase = singleDatabase.equalsIgnoreCase("true");
            if (name == null || name.trim().isBlank()) {
                sendResponse(resp, "Database name should not be empty.");
            } else {
                String msg;
                if (isSingleDatabase) {
                    msg = H2ContextListener.createDatabase(name.trim(), "sa", "Teamwork");
                } else {
                    msg = H2ContextListener.createDatabaseByModule(name.trim(), "sa", "Teamwork");
                }
                sendResponse(resp, msg);
            }
        } else {
            sendResponse(resp,req.getPathInfo() + " : service not allowed.");
        }
    }

    private void sendResponse(HttpServletResponse response, String content) throws IOException {
        response.setCharacterEncoding(ENCODING);
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter printWriter = response.getWriter()) {
            if (content != null) {
                response.setContentLength(content.length());
                printWriter.write(content);
            }
            printWriter.flush();
        }
    }
}
