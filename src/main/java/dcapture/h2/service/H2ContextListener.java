package dcapture.h2.service;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.h2.server.TcpServer;
import org.h2.server.web.WebServer;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

public class H2ContextListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(H2ContextListener.class);
    public static final int SERVICE_PORT = 8083, SHUTDOWN_PORT = 8084;

    private static String[] getModules() {
        return new String[]{"shared", "cashbook", "materials", "project", "inventory", "purchase", "sales"};
    }

    public static String startDatabaseService(ServletContext context) {
        String error = null;
        Server webServer = (Server)context.getAttribute(WebServer.class.getName());
        Server tcpServer = (Server)context.getAttribute(TcpServer.class.getName());
        try {
            if(tcpServer == null) {
                tcpServer = Server.createTcpServer("-tcpAllowOthers", "-tcpPassword", getAuthPassword(context));
                context.setAttribute(TcpServer.class.getName(), tcpServer);
                logger.info("H2 tcp server is created.");
            }
            if(webServer == null) {
                webServer = Server.createWebServer("-webAllowOthers", "-webAdminPassword", getAuthPassword(context));
                context.setAttribute(WebServer.class.getName(), webServer);
                logger.info("H2 web server is created.");
            }
            if(!tcpServer.isRunning(true)) {
                tcpServer.start();
                logger.info("H2 tcp server started.");
            }
            if(!webServer.isRunning(true)) {
                webServer.start();
                logger.info("H2 web server started.");
            }
        } catch (SQLException exc) {
            exc.printStackTrace();
            error = exc.getMessage();
        }
        return error;
    }

    public static String[] statusDatabaseService(ServletContext context) {
        String[] status = new String[2];
        Server tcpServer = (Server)context.getAttribute(TcpServer.class.getName());
        Server webServer = (Server)context.getAttribute(WebServer.class.getName());
        try {
            logger.info("H2 database status.");
            if(tcpServer == null || !tcpServer.isRunning(false)) {
                logger.info("H2 tcp server is offline.");
                status[0] = "H2 tcp server is offline.";
            } else {
                logger.info("H2 tcp server is running.");
                status[0] = "H2 tcp server is running.";
            }
            if(webServer == null || !webServer.isRunning(false)) {
                logger.info("H2 web server is offline.");
                status[1] = "H2 web server is offline.";
            }else {
                logger.info("H2 web server is running.");
                status[1] = "H2 web server is running.";
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            status[0] = exc.getMessage();
            status[1] = "";
        }
        return status;
    }

    public static String stopDatabaseService(ServletContext context) {
        String error = null;
        Server tcpServer = (Server)context.getAttribute(TcpServer.class.getName());
        Server webServer = (Server)context.getAttribute(WebServer.class.getName());
        try {
            logger.info("H2 web server is going to stop.");
            if(webServer != null && webServer.isRunning(true)) {
                webServer.shutdown();
                webServer.stop();
                logger.info("H2 web server is stopped.");
            }else {
                logger.info("H2 web server is not running.");
            }
            logger.info("H2 tcp server is going to stop.");
            if(tcpServer != null && tcpServer.isRunning(true)) {
                tcpServer.shutdown();
                tcpServer.stop();
                logger.info("H2 tcp server is stopped.");
            } else {
                logger.info("H2 tcp server is not running.");
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            error = exc.getMessage();
        }
        return error;
    }

    public static String createDatabaseByModule(String name, String user, String password) {
        StringBuilder builder = new StringBuilder();
        for (String moduleName : getModules()) {
            String info = createDatabaseByModule(name, moduleName, user, password);
            builder.append(info).append("\n");
        }
        return builder.toString();
    }

    public static String createDatabase(String name, String user, String password) {
        Connection conn = null;
        String info = null;
        try {
            conn = DriverManager.getConnection(getH2EmbeddedUrl(name), user, password);
            conn.close();
            info = "Database Created : " + getH2EmbeddedUrl(name);
        } catch (Exception se) {
            se.printStackTrace();
            info = "ERROR : Create database for (" + getH2EmbeddedUrl(name) + ") " + se.getMessage();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                logger.info(info);
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return info;
    }

    private static String createDatabaseByModule(String name, String module, String user, String password) {
        Connection conn = null;
        String info = null;
        try {
            conn = DriverManager.getConnection(getH2EmbeddedUrl(name, module), user, password);
            conn.close();
            info = "Database Created : " + getH2EmbeddedUrl(name, module);
        } catch (Exception se) {
            se.printStackTrace();
            info = "ERROR : Create database for (" + getH2EmbeddedUrl(name, module) + ") " + se.getMessage();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                logger.info(info);
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return info;
    }

    private static String getH2EmbeddedUrl(String name, String module) {
        return "jdbc:h2:~/data/" + name + "/" + module;
    }

    private static String getH2EmbeddedUrl(String name) {
        return "jdbc:h2:~/data/" + name;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        H2ContextListener.startDatabaseService(sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        H2ContextListener.stopDatabaseService(sce.getServletContext());
    }

    private static String getAuthPassword(ServletContext servletContext) {
        String password = servletContext.getInitParameter("password");
        return decryptPKCS5Padding(servletContext.getInitParameter("key"), password);
    }

    private static String encryptPKCS5Padding(String keyCode, String content) {
        if(keyCode != null && content != null) {
            try {
                SecretKey secKey = getSecretEncryptionKey(keyCode);
                Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
                return bytesToHex(aesCipher.doFinal(content.getBytes()));
            } catch (Exception ex) {
                // ignore Exception
            }
        }
        return null;
    }

    private static String decryptPKCS5Padding(String keyCode, String content) {
        if(keyCode != null && content != null) {
            try {
                byte[] contentBytes = hexToBytes(content);
                SecretKey secKey = getSecretEncryptionKey(keyCode);
                Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                aesCipher.init(Cipher.DECRYPT_MODE, secKey);
                byte[] bytePlainText = aesCipher.doFinal(contentBytes);
                return new String(bytePlainText, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                // ignore Exception
            }
        }
        return null;
    }

    private static SecretKey getSecretEncryptionKey(String keyText) {
        return new SecretKeySpec(getKey(keyText), "AES");
    }

    private static byte[] getKey(String keyStr) {
        byte[] key = null;
        try {
            key = (keyStr).getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = getMessageDigest256();
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return key;
    }

    private static byte[] hexToBytes(String content) {
        int len = content.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(content.charAt(i), 16) << 4)
                    + Character.digit(content.charAt(i+1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] byteArray) {
        StringBuilder builder = new StringBuilder();
        for (byte data : byteArray) {
            builder.append(String.format("%02x", data));
        }
        return builder.toString();
    }

    private static MessageDigest getMessageDigest256() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }
}
