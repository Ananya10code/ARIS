package com.aris.project.controller;

import com.aris.auth.User;
import com.aris.project.Project;
import com.aris.project.ProjectResponse;
import com.aris.project.ProjectService;
import com.aris.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Project management API:
 * - Automatically associates new projects with currently authenticated User ID.
 * - GET /api/projects returns ONLY projects owned by authenticated user.
 * - GET /api/projects/{projectId} verifies ownership before returning.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    public record CreateProjectRequest(String name, String description, String sourcePath, String logPath) {}

    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    public ProjectController(ProjectService projectService, SecurityUtils securityUtils) {
        this.projectService = projectService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody(required = false) CreateProjectRequest body,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            Authentication authentication) {

        // Identity derived strictly from JWT authentication context
        String userEmail = authentication != null ? authentication.getName() : securityUtils.getCurrentAuthenticatedUser().getEmail();

        String projName = (body != null && body.name() != null) ? body.name() : name;
        String projDesc = (body != null && body.description() != null) ? body.description() : description;
        String srcPath = (body != null) ? body.sourcePath() : null;
        String logPath = (body != null) ? body.logPath() : null;

        Project project = projectService.createProject(projName, projDesc, srcPath, logPath, userEmail);
        return ResponseEntity.ok(toResponse(project));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : securityUtils.getCurrentAuthenticatedUser().getEmail();
        List<ProjectResponse> projects = projectService.getMyProjects(userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long projectId,
            Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : securityUtils.getCurrentAuthenticatedUser().getEmail();
        Project project = projectService.getProjectForUser(projectId, userEmail);
        return ResponseEntity.ok(toResponse(project));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId(),
                project.getOwner().getEmail(),
                project.getApiKey(),
                project.getSourcePath(),
                project.getLogPath()
        );
    }
}
