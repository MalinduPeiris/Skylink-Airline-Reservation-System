package com.skylinkapplication.skylinkairlinereservationsystem.singleton;

public class SingletonTest {
    public static void main(String[] args) {
        ConfigurationManager config1 = ConfigurationManager.getInstance();
        ConfigurationManager config2 = ConfigurationManager.getInstance();
        System.out.println("ConfigurationManager same instance: " + (config1 == config2));

        AuditLogger logger1 = AuditLogger.getInstance();
        AuditLogger logger2 = AuditLogger.getInstance();
        System.out.println("AuditLogger same instance: " + (logger1 == logger2));

        CacheManager cache1 = CacheManager.getInstance();
        CacheManager cache2 = CacheManager.getInstance();
        System.out.println("CacheManager same instance: " + (cache1 == cache2));
    }
}