package com.aris.monitor.repository;
import com.aris.monitor.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MonitorRepository extends JpaRepository<Monitor, Long> {
    List<Monitor> findByServiceId(Long serviceId);
}
