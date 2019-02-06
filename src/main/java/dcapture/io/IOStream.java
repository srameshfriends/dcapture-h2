package dcapture.io;

import java.io.InputStream;

public interface IOStream {
    InputStream getResourceAsStream(String file);
}
