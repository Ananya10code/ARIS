package com.aris.probe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProbeResultRepository extends JpaRepository<ProbeResult, Long> {
    List<ProbeResult> findByMonitorIdOrderByTsDesc(Long monitorId);

    Page<ProbeResult> findByMonitorIdOrderByTsDesc(Long monitorId, Pageable pageable);

    List<ProbeResult> findTop40ByMonitorIdOrderByTsDesc(Long monitorId);
}