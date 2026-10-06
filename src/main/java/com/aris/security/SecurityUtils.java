package com.aris.security;

import com.aris.auth.User;
import com.aris.auth.repository.UserRepository;
import com.aris.project.Project;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reusable security helper that extracts the authenticated user
 * strictly from the Spring Security / JWT context.
 *
 * Enforces project and resource ownership based on existing User ID.
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the current authenticated User entity from the active JWT context.
     * Never trusts any client-provided userId.
     */
    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required to access this resource");
        }
        String email = auth.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .or(() -> {
                    if ("rakshit".equalsIgnoreCase(email)) {
                        return userRepository.findByEmailIgnoreCase("rakshit@aris.dev");
                    }
                    if ("rakshit@aris.dev".equalsIgnoreCase(email)) {
                        return userRepository.findByEmailIgnoreCase("rakshit");
                    }
                    return java.util.Optional.empty();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found: " + email));
    }

    /**
     * Resolves the current authenticated user, or falls back to the default seeded admin
     * if unauthenticated (e.g. for background internal workers or ai-engine when no JWT is supplied).
     */
    public User getCurrentUserOrFallback() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            return userRepository.findByEmailIgnoreCase(email)
                    .or(() -> {
                        if ("rakshit".equalsIgnoreCase(email)) {
                            return userRepository.findByEmailIgnoreCase("rakshit@aris.dev");
                        }
                        if ("rakshit@aris.dev".equalsIgnoreCase(email)) {
                            return userRepository.findByEmailIgnoreCase("rakshit");
                        }
                        return java.util.Optional.empty();
                    })
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found: " + email));
        }
        return userRepository.findByEmailIgnoreCase("rakshit")
                .or(() -> userRepository.findByEmailIgnoreCase("rakshit@aris.dev"))
                .or(() -> userRepository.findByEmailIgnoreCase("demo@aris.dev"))
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No users registered")));
    }

    /**
     * Verifies that the given project belongs to the authenticated user ID.
     * Throws 403 FORBIDDEN if the user does not own the project.
     */
    public void checkProjectOwnership(Project project, User user) {
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        if (project.getOwner() == null || !project.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Project #" + project.getId() + " does not belong to authenticated user (ID: " + user.getId() + ")");
        }
    }
}
