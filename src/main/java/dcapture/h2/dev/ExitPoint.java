package dcapture.h2.dev;

import dcapture.h2.service.H2ContextListener;
import org.apache.log4j.Logger;

import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ExitPoint {
    private static final Logger logger = Logger.getLogger(ExitPoint.class);
    public static void main(String... args) {
        try{
            logger.info("DCapture H2 database service is stopping now.");
            Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), H2ContextListener.SHUTDOWN_PORT);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("shutdown");
            outputStream.flush();
            outputStream.close();
            socket.close();
        } catch(Exception err) {
            err.printStackTrace();
        }
    }
}
