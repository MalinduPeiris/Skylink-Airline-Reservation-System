package com.skylinkapplication.skylinkairlinereservationsystem.service;

import org.springframework.stereotype.Service;

@Service
public class MonitoringService {
	public String getSystemMetrics() {
		return "uptime=ok, db=ok, version=0.0.1";
	}
}
