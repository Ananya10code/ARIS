package com.aris.incident.controller;

import com.aris.auth.User;
import com.aris.incident.Incident;
import com.aris.incident.repository.IncidentRepository;
import com.aris.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final SecurityUtils securityUtils;

    public IncidentController(IncidentRepository incidentRepository, SecurityUtils securityUtils) {
        this.incidentRepository = incidentRepository;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<Incident>> getAllIncidents() {
        User user = securityUtils.getCurrentUserOrFallback();
        List<Incident> userIncidents = incidentRepository.findAllByOrderByTimestampDesc().stream()
                .filter(i -> i.getService() == null || i.getService().getProject() == null
                        || i.getService().getProject().getOwner().getId().equals(user.getId()))
                .toList();
        return ResponseEntity.ok(userIncidents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncidentById(@PathVariable Long id) {
        return incidentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/monitor/{monitorId}")
    public ResponseEntity<List<Incident>> getIncidentsByMonitor(@PathVariable Long monitorId) {
        return ResponseEntity.ok(incidentRepository.findByMonitorIdOrderByTimestampDesc(monitorId));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<Incident>> getIncidentsByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(incidentRepository.findByServiceIdOrderByTimestampDesc(serviceId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Incident> updateIncidentStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return incidentRepository.findById(id)
                .map(incident -> {
                    incident.setStatus(status.toUpperCase());
                    return ResponseEntity.ok(incidentRepository.save(incident));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
