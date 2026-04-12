package com.skylinkapplication.skylinkairlinereservationsystem.singleton;

import java.util.*;

public class NotificationService {
    private static NotificationService instance;
    private final Queue<Notification> notificationQueue = new LinkedList<>();
    private final List<NotificationListener> listeners = new ArrayList<>();

    private NotificationService() {
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public synchronized void addNotification(Notification notification) {
        notificationQueue.add(notification);
        notifyListeners(notification);
    }

    public synchronized Notification getNextNotification() {
        return notificationQueue.poll();
    }

    public synchronized int getNotificationCount() {
        return notificationQueue.size();
    }

    public synchronized void registerListener(NotificationListener listener) {
        listeners.add(listener);
    }

    private synchronized void notifyListeners(Notification notification) {
        for (NotificationListener listener : listeners) {
            listener.onNotification(notification);
        }
    }

    public static class Notification {
        private final String id;
        private final String type; // EMAIL, SMS, IN_APP
        private final String recipient;
        private final String subject;
        private final String message;
        private final Date createdAt;

        public Notification(String type, String recipient, String subject, String message) {
            this.id = UUID.randomUUID().toString();
            this.type = type;
            this.recipient = recipient;
            this.subject = subject;
            this.message = message;
            this.createdAt = new Date();
        }

        // Getters
        public String getId() { return id; }
        public String getType() { return type; }
        public String getRecipient() { return recipient; }
        public String getSubject() { return subject; }
        public String getMessage() { return message; }
        public Date getCreatedAt() { return createdAt; }
    }

    public interface NotificationListener {
        void onNotification(Notification notification);
    }
}