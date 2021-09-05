package dcapture.h2.dev;

import dcapture.h2.service.H2ContextListener;
import dcapture.h2.service.H2ServiceServlet;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class EntryPoint {
    private static final Logger logger = Logger.getLogger(EntryPoint.class);

    private void addInitParam(ServletContextHandler context) {
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        context.setInitParameter("key", "f834227e-a3a8-4dd2-a5ce-8ebb84b9b1ee");
        context.setInitParameter("password", "3d3dd5957b8be3e36366431a0595c3ca");
    }

    private void start() throws Exception {
        Server server = new Server(H2ContextListener.SERVICE_PORT);
        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.addEventListener(new H2ContextListener());
        servletContext.setContextPath("/");
        //
        ServletHolder defaultHolder = new ServletHolder(new DefaultServlet());
        servletContext.addServlet(defaultHolder, "/*");
        ServletHolder refreshHolder = new ServletHolder(new H2ServiceServlet());
        servletContext.addServlet(refreshHolder, "/database/*");
        addInitParam(servletContext);
        server.setHandler(servletContext);
        servletContext.setAttribute(Server.class.getName(), server);
        logger.info("H2 jetty service port  :" + H2ContextListener.SERVICE_PORT);
        logger.info("H2 jetty shutdown port :" + H2ContextListener.SHUTDOWN_PORT);
        Thread monitor = new H2JettyStopService(server);
        monitor.start();
        server.start();
        server.join();
    }

    public static void main(String... args) {
        try {
            EntryPoint server = new EntryPoint();
            server.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class H2JettyStopService extends Thread {
        private final ServerSocket socket;
        private final Server server;

        public H2JettyStopService(Server server) {
            setDaemon(true);
            setName("H2JettyStopService");
            this.server = server;
            try {
                socket = new ServerSocket(H2ContextListener.SHUTDOWN_PORT, 1, InetAddress.getByName("127.0.0.1"));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            System.out.println("H2 database stop service port " + H2ContextListener.SHUTDOWN_PORT);
            Socket accept;
            try {
                accept = socket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
                reader.readLine();
                logger.info("Going to stop H2 jetty server.");
                server.stop();
                accept.close();
                socket.close();
                logger.info("H2 jetty server stopped.");
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
