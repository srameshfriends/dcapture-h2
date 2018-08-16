package dcapture.io;

import java.io.File;

public interface FileMonitorListener {
    void onFileMonitor(String type, File file);
}
