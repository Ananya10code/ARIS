package com.aris.probe;

import com.aris.monitor.Monitor;
import com.aris.monitor.repository.MonitorRepository;
import com.aris.probe.ProbeResultRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitorSchedulerService {

    private final MonitorRepository monitorRepository;
    private final ProbeService probeService;
    private final ProbeResultRepository probeResultRepository;

    private final Map<Long, Instant> lastExecutionMap = new ConcurrentHashMap<>();

    public MonitorSchedulerService(
            MonitorRepository monitorRepository,
            ProbeService probeService,
            ProbeResultRepository probeResultRepository) {
        this.monitorRepository = monitorRepository;
        this.probeService = probeService;
        this.probeResultRepository = probeResultRepository;
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduleProbes() {
        List<Monitor> allMonitors = monitorRepository.findAll();
        Instant now = Instant.now();

        for (Monitor monitor : allMonitors) {
            if (!monitor.isEnabled()) {
                continue;
            }

            Instant lastRun = lastExecutionMap.get(monitor.getId());
            long intervalSec = (monitor.getIntervalSeconds() != null) ? monitor.getIntervalSeconds() : 60;

            if (lastRun == null || now.isAfter(lastRun.plusSeconds(intervalSec))) {
                lastExecutionMap.put(monitor.getId(), now);
                executeAndLogProbe(monitor);
            }
        }
    }

    public ProbeResult executeAndLogProbe(Monitor monitor) {
        ProbeResult result = probeService.executeProbe(monitor);
        return probeResultRepository.save(result);
    }
}