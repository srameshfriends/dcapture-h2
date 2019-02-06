package dcapture.io;

import org.apache.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileMonitor implements Runnable, FileMonitorListener {
    private static final Logger logger = Logger.getLogger(FileMonitor.class);
    private final File source;
    private final String sourcePrefix, targetPrefix;
    private FileMonitorListener listener;
    private Map<String, FileMonitorModel> fileServiceMap;
    private boolean runningService, ignoreListener;
    private Set<String> ignoredFolderSet;

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

    public FileMonitor(File source, File target) {
        if (source == null || !source.exists() || !source.isDirectory()) {
            throw new NullPointerException("Source directory should not be empty");
        }
        if (target == null || !target.exists() || !target.isDirectory()) {
            throw new NullPointerException("Target directory should not be empty");
        }
        this.source = source;
        sourcePrefix = source.getAbsolutePath();
        targetPrefix = target.getAbsolutePath();
        this.listener = this;
    }

    public void setListener(FileMonitorListener listener) {
        this.listener = listener == null ? this : listener;
    }

    @Override
    public void run() {
        File ideaIgnoreFile = new File(source.getAbsolutePath() + File.separator + ".idea");
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

    public boolean isRunning() {
        return runningService;
    }

    public void stop() {
        runningService = false;
    }

    private void monitor() {
        Set<String> monitoredItemsSet = new HashSet<>();
        if (source.exists() && source.isDirectory()) {
            File[] fileArray = source.listFiles();
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
                    listener.onFileMonitor("Deleted", entry.getValue().getFile());
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
                    listener.onFileMonitor("Created", file);
                }
            } else if (existing.isModified()) {
                existing.updateLastModified();
                if (!ignoreListener) {
                    listener.onFileMonitor("Modified", file);
                }
            }
            monitoredItemsSet.add(file.getAbsolutePath());
        }
    }

    public static void main(String... args) {
        if (2 > args.length) {
            throw new IllegalArgumentException("Source and target directory path not configured!");
        }
        logger.info("File Monitor Source : " + args[0]);
        logger.info("File Monitor Target : " + args[1]);
        FileMonitor fileMonitor = new FileMonitor(new File(args[0]), new File(args[1]));
        fileMonitor.run();
    }

    @Override
    public void onFileMonitor(String type, File file) {
        logger.info("File has " + type + " \t " + file);
        try {
            String target = file.getAbsolutePath().replace(sourcePrefix, targetPrefix);
            if ("Modified".equals(type)) {
                Files.copy(file.toPath(), Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
            } else if("Created".equals(type)) {
                Files.copy(file.toPath(), Paths.get(target));
            } else if ("Deleted".equals(type)) {
                Files.delete(Paths.get(target));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
