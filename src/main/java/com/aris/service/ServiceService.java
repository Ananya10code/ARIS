package com.aris.service;
import com.aris.project.Project;
import com.aris.project.repository.ProjectRepository;
import com.aris.service.repository.ServiceRepository;
import java.util.List;
@org.springframework.stereotype.Service
public class ServiceService {
    private final ServiceRepository serviceRepository;
    private final ProjectRepository projectRepository;
    public ServiceService(
            ServiceRepository serviceRepository,
            ProjectRepository projectRepository) {
        this.serviceRepository = serviceRepository;
        this.projectRepository = projectRepository;
    }
    public Service createService(
            Long projectId,
            String name,
            String description,
            String baseUrl) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));
        Service service = new Service(
                name,
                description,
                baseUrl,
                project
        );
        return serviceRepository.save(service);
    }
    public List<Service> getProjectServices(Long projectId) {
        return serviceRepository.findByProjectId(projectId);
    }
}
