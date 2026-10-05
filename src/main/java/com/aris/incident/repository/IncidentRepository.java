package com.aris.incident.repository;

import com.aris.incident.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByOrderByTimestampDesc();
    List<Incident> findByMonitorIdOrderByTimestampDesc(Long monitorId);
    List<Incident> findByServiceIdOrderByTimestampDesc(Long serviceId);
    List<Incident> findByStatus(String status);
}
