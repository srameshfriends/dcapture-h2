package dcapture.h2.embedded;

import dcapture.h2.service.H2ContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ExitPoint {
    private static final Logger logger = LoggerFactory.getLogger(ExitPoint.class);

    public static void main(String... args) {
        Socket socket = null;
        try{
            logger.info("DCapture H2 database service is stopping now.");
            socket = new Socket(InetAddress.getByName("127.0.0.1"), H2ContextListener.SHUTDOWN_PORT);
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("shutdown");
            outputStream.flush();
            logger.info(inputStream.readUTF());
            close(outputStream);
            close(inputStream);
        } catch(Exception err) {
            err.printStackTrace();
        } finally {
            if(socket != null) {
                try {
                    socket.close();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
    }

    private static void close(DataOutputStream inputStream) {
        if(inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ex2) {
                // ignore
            }
        }
    }

    private static void close(DataInputStream outputStream) {
        if(outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception ex2) {
                // ignore
            }
        }
    }
}
