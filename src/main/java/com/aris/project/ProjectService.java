package com.aris.project;

import com.aris.auth.User;
import com.aris.auth.repository.UserRepository;
import com.aris.project.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Project createProject(String name, String description, String email) {
        return createProject(name, description, null, null, email);
    }

    public Project createProject(String name, String description, String sourcePath, String logPath, String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found: " + email));

        Project project = new Project(name, description, owner);
        project.setSourcePath(sourcePath != null && !sourcePath.isBlank()
                ? sourcePath
                : Path.of("src/main/java").toAbsolutePath().toString());
        project.setLogPath(logPath != null && !logPath.isBlank()
                ? logPath
                : Path.of("logs/aris.log").toAbsolutePath().toString());

        return projectRepository.save(project);
    }

    /**
     * Returns ONLY the projects belonging to the authenticated user ID.
     */
    public List<Project> getMyProjects(String email) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found: " + email));
        return projectRepository.findByOwnerId(owner.getId());
    }

    /**
     * Retrieves a project and verifies that it belongs to the authenticated user.
     * Throws 403 FORBIDDEN if the user does not own the project.
     */
    public Project getProjectForUser(Long projectId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found: " + email));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project #" + projectId + " not found"));

        if (project.getOwner() == null || !project.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Project #" + projectId + " does not belong to authenticated user (ID: " + user.getId() + ")");
        }
        return project;
    }

    /**
     * Checks if the given project belongs to the given user ID.
     */
    public Project verifyProjectOwnership(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project #" + projectId + " not found"));
        if (project.getOwner() == null || !project.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Project #" + projectId + " does not belong to user ID #" + userId);
        }
        return project;
    }
}
