package com.skylinkapplication.skylinkairlinereservationsystem.singleton;

import java.util.*;

public class DatabaseConnectionPool {
    private static DatabaseConnectionPool instance;
    private final int maxConnections = 20;
    private final Queue<String> availableConnections = new LinkedList<>();
    private final Set<String> usedConnections = new HashSet<>();

    private DatabaseConnectionPool() {
        // Initialize connection pool
        for (int i = 0; i < maxConnections; i++) {
            availableConnections.add("Connection-" + i);
        }
    }

    public static synchronized DatabaseConnectionPool getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionPool();
        }
        return instance;
    }

    public synchronized String getConnection() {
        if (availableConnections.isEmpty()) {
            throw new RuntimeException("No available database connections");
        }
        String connection = availableConnections.poll();
        usedConnections.add(connection);
        return connection;
    }

    public synchronized void releaseConnection(String connection) {
        if (usedConnections.remove(connection)) {
            availableConnections.add(connection);
        }
    }

    public synchronized int getAvailableConnectionCount() {
        return availableConnections.size();
    }

    public synchronized int getUsedConnectionCount() {
        return usedConnections.size();
    }
}