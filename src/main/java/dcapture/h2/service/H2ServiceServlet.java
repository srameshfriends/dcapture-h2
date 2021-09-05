package dcapture.h2.service;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

import java.io.IOException;

public class H2ServiceServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(H2ServiceServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if(req.getPathInfo() == null) {
            resp.getWriter().write("Service not supported.");
            resp.getWriter().close();
        } else if("/start".equals(req.getPathInfo())) {
            String info1 = H2ContextListener.startDatabaseService(req.getServletContext());
            if(info1 == null) {
                info1 = "H2 database started.";
            }
            logger.info(info1);
            resp.getWriter().write(info1);
            resp.getWriter().close();
        } else if("/stop".equals(req.getPathInfo())) {
            String info2 = H2ContextListener.stopDatabaseService(req.getServletContext());
            if(info2 == null) {
                info2 = "H2 database stopped.";
            }
            logger.info(info2);
            resp.getWriter().write(info2);
            resp.getWriter().close();
        } else if("/status".equals(req.getPathInfo())) {
            String[] info3 = H2ContextListener.statusDatabaseService(req.getServletContext());
            resp.getWriter().write(info3[0]);
            resp.getWriter().write("\n");
            resp.getWriter().write(info3[1]);
            resp.getWriter().close();
        } else {
            resp.getWriter().write(req.getPathInfo() + " : service not allowed.");
            resp.getWriter().close();
        }
    }
}
