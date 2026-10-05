package com.aris.service.controller;

import com.aris.auth.User;
import com.aris.project.Project;
import com.aris.project.repository.ProjectRepository;
import com.aris.security.SecurityUtils;
import com.aris.service.Service;
import com.aris.service.ServiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/services")
public class ServiceController {
    private final ServiceService serviceService;
    private final ProjectRepository projectRepository;
    private final SecurityUtils securityUtils;

    public ServiceController(ServiceService serviceService,
                             ProjectRepository projectRepository,
                             SecurityUtils securityUtils) {
        this.serviceService = serviceService;
        this.projectRepository = projectRepository;
        this.securityUtils = securityUtils;
    }

    private void checkOwnership(Long projectId) {
        User user = securityUtils.getCurrentAuthenticatedUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        securityUtils.checkProjectOwnership(project, user);
    }

    @PostMapping
    public ResponseEntity<Service> createService(
            @PathVariable Long projectId,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String baseUrl) {
        checkOwnership(projectId);
        Service service = serviceService.createService(
                projectId,
                name,
                description,
                baseUrl
        );
        return ResponseEntity.ok(service);
    }

    @GetMapping
    public ResponseEntity<List<Service>> getProjectServices(
            @PathVariable Long projectId) {
        checkOwnership(projectId);
        return ResponseEntity.ok(
                serviceService.getProjectServices(projectId)
        );
    }
}
