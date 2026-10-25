package rentsphere.support;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-App Notification Center Service.
 * Tracks user events (reservation, escrow locks, pickup readiness, inspection results, dispute updates).
 * Persists notifications to flat-file notifications_log.txt using atomic file operations.
 */
public class NotificationService {

    public static class NotificationItem {
        public String id;
        public String userId;
        public String title;
        public String message;
        public String type; // "INFO", "SUCCESS", "WARNING", "DANGER"
        public boolean read;
        public long timestamp;

        public NotificationItem(String id, String userId, String title, String message, String type) {
            this.id = id;
            this.userId = userId;
            this.title = title;
            this.message = message;
            this.type = type;
            this.read = false;
            this.timestamp = System.currentTimeMillis();
        }

        public NotificationItem(String id, String userId, String title, String message, String type, boolean read, long timestamp) {
            this.id = id;
            this.userId = userId;
            this.title = title;
            this.message = message;
            this.type = type;
            this.read = read;
            this.timestamp = timestamp;
        }

        public String serializeToPipe() {
            return String.format("%s|%s|%s|%s|%s|%b|%d",
                    id, userId,
                    title != null ? title.replace("|", "/") : "",
                    message != null ? message.replace("|", "/") : "",
                    type != null ? type : "INFO",
                    read,
                    timestamp);
        }
    }

    private final Map<String, List<NotificationItem>> userNotifications = new ConcurrentHashMap<>();
    private final Path notifsFile;
    private final Object fileLock = new Object();

    public NotificationService() {
        this("data");
    }

    public NotificationService(String baseDataDir) {
        Path dataDir = Paths.get(baseDataDir != null ? baseDataDir : "data");
        this.notifsFile = dataDir.resolve("notifications_log.txt");
        loadNotifications();
    }

    public synchronized void pushNotification(String userId, String title, String message, String type) {
        String id = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8);
        NotificationItem item = new NotificationItem(id, userId, title, message, type);
        userNotifications.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(0, item);
        saveNotifications();
    }

    public List<NotificationItem> getUserNotifications(String userId) {
        List<NotificationItem> list = userNotifications.get(userId);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public synchronized void markAllAsRead(String userId) {
        List<NotificationItem> list = userNotifications.get(userId);
        if (list != null) {
            synchronized (list) {
                for (NotificationItem n : list) {
                    n.read = true;
                }
            }
            saveNotifications();
        }
    }

    public int getTotalCount() {
        int count = 0;
        for (List<NotificationItem> list : userNotifications.values()) {
            synchronized (list) {
                count += list.size();
            }
        }
        return count;
    }

    private void loadNotifications() {
        synchronized (fileLock) {
            if (!Files.exists(notifsFile)) return;
            try (BufferedReader reader = Files.newBufferedReader(notifsFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 7) continue;

                    String id = parts[0];
                    String userId = parts[1];
                    String title = parts[2];
                    String message = parts[3];
                    String type = parts[4];
                    boolean read = Boolean.parseBoolean(parts[5]);
                    long timestamp;
                    try {
                        timestamp = Long.parseLong(parts[6]);
                    } catch (NumberFormatException e) {
                        timestamp = System.currentTimeMillis();
                    }

                    NotificationItem item = new NotificationItem(id, userId, title, message, type, read, timestamp);
                    userNotifications.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(item);
                }
            } catch (IOException e) {
                System.err.println("[NOTIF] Error loading notifications_log.txt: " + e.getMessage());
            }
        }
    }

    private void saveNotifications() {
        synchronized (fileLock) {
            try {
                if (notifsFile.getParent() != null && !Files.exists(notifsFile.getParent())) {
                    Files.createDirectories(notifsFile.getParent());
                }
                Path temp = notifsFile.resolveSibling(notifsFile.getFileName() + ".tmp");
                try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write("# RentSphere In-App Notification Center Ledger");
                    writer.newLine();
                    for (Map.Entry<String, List<NotificationItem>> entry : userNotifications.entrySet()) {
                        List<NotificationItem> list = entry.getValue();
                        synchronized (list) {
                            for (NotificationItem n : list) {
                                writer.write(n.serializeToPipe());
                                writer.newLine();
                            }
                        }
                    }
                }
                try {
                    Files.move(temp, notifsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, notifsFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.err.println("[NOTIF] Error saving notifications_log.txt: " + e.getMessage());
            }
        }
    }
}
