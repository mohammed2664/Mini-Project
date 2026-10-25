package rentsphere.support;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-App Notification Center Service.
 * Tracks user events (reservation, escrow locks, pickup readiness, inspection results, dispute updates).
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
    }

    private final Map<String, List<NotificationItem>> userNotifications = new ConcurrentHashMap<>();

    public synchronized void pushNotification(String userId, String title, String message, String type) {
        String id = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8);
        NotificationItem item = new NotificationItem(id, userId, title, message, type);
        userNotifications.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>())).add(0, item);
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
        }
    }
}
