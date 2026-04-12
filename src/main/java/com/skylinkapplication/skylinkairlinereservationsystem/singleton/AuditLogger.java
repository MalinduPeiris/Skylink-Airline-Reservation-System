package com.skylinkapplication.skylinkairlinereservationsystem.singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditLogger {
    private static AuditLogger instance;
    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);
    private final List<String> auditLog = new ArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditLogger() {
    }

    public static synchronized AuditLogger getInstance() {
        if (instance == null) {
            instance = new AuditLogger();
        }
        return instance;
    }

    public synchronized void logAction(String userId, String action, String resource, String details) {
        String logEntry = String.format("[%s] User: %s | Action: %s | Resource: %s | Details: %s",
                LocalDateTime.now().format(formatter), userId, action, resource, details);
        auditLog.add(logEntry);
        logger.info(logEntry);
    }

    public synchronized List<String> getAuditLog() {
        return new ArrayList<>(auditLog);
    }

    public synchronized void clearAuditLog() {
        auditLog.clear();
    }
}