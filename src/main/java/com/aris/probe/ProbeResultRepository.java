package com.aris.probe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProbeResultRepository extends JpaRepository<ProbeResult, Long> {
    List<ProbeResult> findTop40ByMonitorIdOrderByTsDesc(Long monitorId);
}
