package com.aris.probe;

import com.aris.monitor.repository.MonitorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors/{monitorId}/history")
public class ProbeHistoryController {

    private final ProbeResultRepository probeResultRepository;
    private final MonitorRepository monitorRepository;

    public ProbeHistoryController(
            ProbeResultRepository probeResultRepository,
            MonitorRepository monitorRepository) {
        this.probeResultRepository = probeResultRepository;
        this.monitorRepository = monitorRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProbeResult>> getMonitorHistory(
            @PathVariable Long monitorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (!monitorRepository.existsById(monitorId)) {
            return ResponseEntity.notFound().build();
        }

        List<ProbeResult> history = probeResultRepository
                .findByMonitorIdOrderByTsDesc(monitorId, PageRequest.of(page, size))
                .getContent();

        return ResponseEntity.ok(history);
    }
}