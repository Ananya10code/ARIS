package com.aris.monitor;
import com.aris.monitor.repository.MonitorRepository;
import com.aris.service.Service;
import com.aris.service.repository.ServiceRepository;
import java.util.List;
@org.springframework.stereotype.Service
public class MonitorService {
    private final MonitorRepository monitorRepository;
    private final ServiceRepository serviceRepository;
    public MonitorService(
            MonitorRepository monitorRepository,
            ServiceRepository serviceRepository) {
        this.monitorRepository = monitorRepository;
        this.serviceRepository = serviceRepository;
    }
    public Monitor createMonitor(
            Long serviceId,
            String name,
            String endpoint,
            String method,
            Integer intervalSeconds,
            Integer timeoutSeconds) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() ->
                        new RuntimeException("Service not found"));
        Monitor monitor = new Monitor(
                name,
                endpoint,
                method,
                intervalSeconds,
                timeoutSeconds,
                true,
                service
        );
        return monitorRepository.save(monitor);
    }
    public List<Monitor> getServiceMonitors(Long serviceId) {
        return monitorRepository.findByServiceId(serviceId);
    }
}