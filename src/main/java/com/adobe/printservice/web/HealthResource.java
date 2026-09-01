package com.adobe.printservice.web;

import com.adobe.printservice.web.dto.HealthStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthResource {

    private final DataSource dataSource;

    public HealthResource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 1. Liveness Probe
    // Returns 200 OK if the application process is alive and JVM is responding
    @GetMapping("/liveness")
    public ResponseEntity<HealthStatusResponse> liveness() {
        return ResponseEntity.ok(new HealthStatusResponse("UP"));
    }

    // 2. Readiness Probe
    // Always returns 200 OK, but details which components are UP or DOWN
    @GetMapping("/readiness")
    public ResponseEntity<HealthStatusResponse> readiness() {
        Map<String, Object> checks = new HashMap<>();
        boolean isReady = true;

        // Check 1: Database connectivity
        boolean dbHealthy = checkDatabaseConnection();
        checks.put("database", dbHealthy ? "UP" : "DOWN");
        if (!dbHealthy) {
            isReady = false;
        }

        // Check 2: Storage accessibility (Crucial for print/rendering services)
        boolean storageHealthy = checkStorageAccess();
        checks.put("storage", storageHealthy ? "UP" : "DOWN");
        if (!storageHealthy) {
            isReady = false;
        }

        String globalStatus = isReady ? "UP" : "DOWN";

        // Always returns the breakdown
        return ResponseEntity.ok(new HealthStatusResponse(globalStatus, isReady, checks));
    }

    private boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2); // 2-second timeout check
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkStorageAccess() {
        // Logic to verify if temporary directories or remote storage (e.g., S3) are writable/accessible
        return true;
    }
}