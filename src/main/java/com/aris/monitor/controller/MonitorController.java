package com.aris.monitor.controller;
import com.aris.monitor.Monitor;
import com.aris.monitor.MonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/services/{serviceId}/monitors")
public class MonitorController {
    private final MonitorService monitorService;
    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }
    @PostMapping
    public ResponseEntity<Monitor> createMonitor(
            @PathVariable Long serviceId,
            @RequestParam String name,
            @RequestParam String endpoint,
            @RequestParam String method,
            @RequestParam Integer intervalSeconds,
            @RequestParam Integer timeoutSeconds) {
        Monitor monitor = monitorService.createMonitor(
                serviceId,
                name,
                endpoint,
                method,
                intervalSeconds,
                timeoutSeconds
        );
        return ResponseEntity.ok(monitor);
    }
    @GetMapping
    public ResponseEntity<List<Monitor>> getServiceMonitors(
            @PathVariable Long serviceId) {
        return ResponseEntity.ok(
                monitorService.getServiceMonitors(serviceId)
        );
    }
}
