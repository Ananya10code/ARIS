package com.aris.service.repository;
import com.aris.service.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByProjectId(Long projectId);
}
