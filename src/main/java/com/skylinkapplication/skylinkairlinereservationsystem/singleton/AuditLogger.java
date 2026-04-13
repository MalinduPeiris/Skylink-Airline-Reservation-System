package com.skylinkapplication.skylinkairlinereservationsystem.singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuditLogger {

    private static volatile AuditLogger instance;
    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

    private final List<String> auditLog = new CopyOnWriteArrayList<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditLogger() {
    }

    public static AuditLogger getInstance() {
        if (instance == null) {
            synchronized (AuditLogger.class) {
                if (instance == null) {
                    instance = new AuditLogger();
                }
            }
        }
        return instance;
    }

    public void logAction(String userId, String action, String resource, String details) {
        String logEntry = String.format("[%s] User: %s | Action: %s | Resource: %s | Details: %s",
                LocalDateTime.now().format(formatter), userId, action, resource, details);
        auditLog.add(logEntry);
        logger.info(logEntry);
    }

    public List<String> getAuditLog() {
        return Collections.unmodifiableList(new ArrayList<>(auditLog));
    }

    public void clearAuditLog() {
        auditLog.clear();
    }
}