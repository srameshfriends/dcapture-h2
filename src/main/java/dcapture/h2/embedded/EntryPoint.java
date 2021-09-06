package dcapture.h2.embedded;

import dcapture.h2.service.H2ContextListener;
import dcapture.h2.service.H2ServiceServlet;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

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
            try {
                Socket accept = socket.accept();
                DataInputStream inputStream = new DataInputStream(accept.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(accept.getOutputStream());
                String command = "H2 service " + inputStream.readUTF() + "  command received.";
                logger.info(command);
                outputStream.writeUTF(command);
                outputStream.flush();
                server.stop();
                close(outputStream);
                close(inputStream);
                close(accept);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private String getString(byte[] allBytes) {
            return allBytes == null ? "Unknown" : new String(allBytes, StandardCharsets.UTF_8);
        }

        private void close(DataInputStream inputStream) {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        private void close(DataOutputStream outputStream) {
            if(outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        private void close(Socket accept) {
            try {
                if(accept != null) {
                    accept.close();
                }
            } catch (Exception ex) {
                // ignore
            }
        }
    }
}
