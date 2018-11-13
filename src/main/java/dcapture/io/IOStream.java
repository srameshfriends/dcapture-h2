package dcapture.io;

import java.io.InputStream;
import java.io.OutputStream;

public interface IOStream {
    default InputStream getInputStream(String path) {
        return null;
    }

    default OutputStream getOutputStream(String path) {
        return null;
    }
}
