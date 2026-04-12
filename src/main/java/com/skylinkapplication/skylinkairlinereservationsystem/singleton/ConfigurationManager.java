package com.skylinkapplication.skylinkairlinereservationsystem.singleton;

public class ConfigurationManager {
    private static ConfigurationManager instance;
    private String appName = "SkyLink Airline Reservation System";
    private String appVersion = "1.0.0";
    private int maxSessionTimeout = 3600; // seconds
    private int maxBookingPassengers = 5;
    private double taxPercentage = 10.0; // Fixed tax percentage
    private boolean maintenanceMode = false;

    // Private constructor to prevent instantiation
    private ConfigurationManager() {
    }

    // Synchronized method to get singleton instance (thread-safe)
    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    // Getters and setters
    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout;
    }

    public int getMaxBookingPassengers() {
        return maxBookingPassengers;
    }

    public double getTaxPercentage() {
        return taxPercentage;
    }

    public void setTaxPercentage(double taxPercentage) {
        this.taxPercentage = taxPercentage;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }
}