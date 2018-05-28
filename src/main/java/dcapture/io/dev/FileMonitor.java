package dcapture.io.dev;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileMonitor implements Runnable {
    private final File folder;
    private final FileMonitorListener listener;
    private Map<String, FileMonitorModel> fileServiceMap;
    private boolean runningService, ignoreListener;
    private Set<String> ignoredFolderSet;

    public interface FileMonitorListener {
        void onFileMonitorEvent(String type, File file);
    }

    private class FileMonitorModel {
        private final File file;
        private long lastModified;

        FileMonitorModel(File file) {
            this.file = file;
            updateLastModified();
        }

        void updateLastModified() {
            lastModified = file.lastModified();
        }

        boolean isModified() {
            return lastModified < file.lastModified();
        }

        boolean exists() {
            return file.exists();
        }

        File getFile() {
            return file;
        }
    }

    public FileMonitor(File folder, FileMonitorListener listener) {
        this.folder = folder;
        this.listener = listener;
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            throw new NullPointerException("Folder should not be empty");
        }
        if (listener == null) {
            throw new NullPointerException("Listener should not be empty");
        }
    }

    @Override
    public void run() {
        File ideaIgnoreFile = new File(folder.getAbsolutePath() + File.separator + ".idea");
        ignoredFolderSet = new HashSet<>();
        ignoredFolderSet.add(ideaIgnoreFile.getAbsolutePath());
        fileServiceMap = new HashMap<>();
        ignoreListener = true;
        monitor();
        ignoreListener = false;
        runningService = true;
        while (runningService) {
            monitor();
            try {
                Thread.sleep(1600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    boolean isRunning() {
        return runningService;
    }

    void stop() {
        runningService = false;
    }

    private void monitor() {
        Set<String> monitoredItemsSet = new HashSet<>();
        if (folder.exists() && folder.isDirectory()) {
            File[] fileArray = folder.listFiles();
            if (fileArray != null && 0 < fileArray.length) {
                monitorFiles(fileArray, monitoredItemsSet);
                for (File file : fileArray) {
                    if (file.isDirectory() && !ignoredFolderSet.contains(file.getAbsolutePath())) {
                        monitorFolders(file, monitoredItemsSet);
                    }
                }
            }
            monitorExitingItems(monitoredItemsSet);
        }
    }

    private void monitorExitingItems(Set<String> monitoredItemsSet) {
        if (monitoredItemsSet.size() != fileServiceMap.size()) {
            Set<String> deletedSet = new HashSet<>();
            fileServiceMap.entrySet().stream().filter(entry -> !monitoredItemsSet.contains( //
                    entry.getKey()) && !entry.getValue().exists()).forEach(entry -> {
                if (!ignoreListener) {
                    listener.onFileMonitorEvent("Deleted", entry.getValue().getFile());
                }
                deletedSet.add(entry.getKey());
            });
            if (0 < deletedSet.size()) {
                deletedSet.forEach(fileServiceMap::remove);
            }
        }
    }

    private void monitorFolders(File folder, Set<String> monitoredItemsSet) {
        File[] fileArray = folder.listFiles();
        if (fileArray != null && 0 < fileArray.length) {
            monitorFiles(fileArray, monitoredItemsSet);
            for (File file : fileArray) {
                if (file.isDirectory()) {
                    monitorFolders(file, monitoredItemsSet);
                }
            }
        }
    }

    private void monitorFiles(File[] fileArray, Set<String> monitoredItemsSet) {
        for (File file : fileArray) {
            if (file.isDirectory()) {
                continue;
            }
            FileMonitorModel existing = fileServiceMap.get(file.getAbsolutePath());
            if (existing == null) {
                fileServiceMap.put(file.getAbsolutePath(), new FileMonitorModel(file));
                if (!ignoreListener) {
                    listener.onFileMonitorEvent("Created", file);
                }
            } else if (existing.isModified()) {
                existing.updateLastModified();
                if (!ignoreListener) {
                    listener.onFileMonitorEvent("Modified", file);
                }
            }
            monitoredItemsSet.add(file.getAbsolutePath());
        }
    }
}
