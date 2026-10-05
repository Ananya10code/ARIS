package com.aris.probe;

import com.aris.auth.User;
import com.aris.auth.repository.UserRepository;
import com.aris.monitor.Monitor;
import com.aris.monitor.repository.MonitorRepository;
import com.aris.project.Project;
import com.aris.project.repository.ProjectRepository;
import com.aris.security.SecurityUtils;
import com.aris.service.Service;
import com.aris.service.repository.ServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Backs ARIS multi-project architecture with strict user-scoped project ownership:
 * - Overall system overview & health aggregation (scoped to authenticated user)
 * - Dynamic project creation and management (owner derived from JWT context)
 * - Dynamic endpoint addition & manual on-demand testing
 * - Source tree browsing & file reading (guarded by project ownership)
 * - Developer-approved code patching (guarded by project ownership)
 */
@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {
    record CreateProject(String name, String baseUrl, String sourcePath, String logPath, List<String> endpoints) {}
    record AddEndpoint(String name, String endpoint, String method, Integer intervalSeconds, Integer timeoutSeconds) {}
    record ApplyPatch(String file, String before, String after) {}

    private static final Set<String> SKIP = Set.of("target", ".git", "node_modules", ".idea", ".venv", "__pycache__", "build", "dist");
    private static final Pattern OK = Pattern.compile("\\.(java|py|js|ts|jsx|tsx|yml|yaml|properties|xml|md|json|html|css|sql)$");

    private final ProjectRepository projects;
    private final ServiceRepository services;
    private final MonitorRepository monitors;
    private final UserRepository users;
    private final ProbeResultRepository results;
    private final SecurityUtils securityUtils;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public WorkspaceController(ProjectRepository projects, ServiceRepository services,
                               MonitorRepository monitors, UserRepository users,
                               ProbeResultRepository results, SecurityUtils securityUtils) {
        this.projects = projects;
        this.services = services;
        this.monitors = monitors;
        this.users = users;
        this.results = results;
        this.securityUtils = securityUtils;
    }

    private Map<String, Object> dto(Project p) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("id", p.getId());
        o.put("name", p.getName());
        o.put("ownerId", p.getOwner() != null ? p.getOwner().getId() : null);
        o.put("ownerEmail", p.getOwner() != null ? p.getOwner().getEmail() : "");
        o.put("apiKey", p.getApiKey());
        o.put("sourcePath", p.getSourcePath());
        o.put("logPath", p.getLogPath());
        o.put("baseUrl", services.findByProjectId(p.getId()).stream().findFirst().map(Service::getBaseUrl).orElse(""));
        List<Monitor> pMons = monitors.findAll().stream()
                .filter(m -> m.getService().getProject().getId().equals(p.getId()))
                .toList();
        o.put("monitorCount", pMons.size());

        // Calculate dynamic project health score
        if (pMons.isEmpty()) {
            o.put("healthScore", 100);
            o.put("avgLatency", 0);
            o.put("status", "HEALTHY");
        } else {
            long totalLat = 0;
            int count = 0;
            int downCount = 0;
            for (Monitor m : pMons) {
                var history = results.findTop40ByMonitorIdOrderByTsDesc(m.getId());
                if (!history.isEmpty()) {
                    ProbeResult last = history.get(0);
                    totalLat += last.latencyMs;
                    count++;
                    if (last.statusCode == 0 || last.statusCode >= 500) {
                        downCount++;
                    }
                }
            }
            long avg = count > 0 ? (totalLat / count) : 0;
            int score = Math.max(10, Math.min(100, 100 - (downCount * 25) - (avg > 500 ? 20 : 0)));
            o.put("healthScore", score);
            o.put("avgLatency", avg);
            o.put("status", score > 80 ? "HEALTHY" : score > 50 ? "DEGRADED" : "CRITICAL");
        }
        return o;
    }

    private Project find(Long id) {
        return projects.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private Path root(Project p) {
        try {
            return Path.of(p.getSourcePath()).toRealPath();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source folder not found: " + p.getSourcePath());
        }
    }

    /**
     * Overall System Overview: aggregated ONLY for projects belonging to the authenticated user ID.
     */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        User user = securityUtils.getCurrentUserOrFallback();
        List<Project> userProjects = projects.findByOwnerId(user.getId());
        List<Monitor> userMonitors = monitors.findAll().stream()
                .filter(m -> m.getService().getProject().getOwner().getId().equals(user.getId()))
                .toList();

        long totalLat = 0;
        int count = 0;
        int downCount = 0;

        for (Monitor m : userMonitors) {
            var history = results.findTop40ByMonitorIdOrderByTsDesc(m.getId());
            if (!history.isEmpty()) {
                ProbeResult last = history.get(0);
                totalLat += last.latencyMs;
                count++;
                if (last.statusCode == 0 || last.statusCode >= 500) {
                    downCount++;
                }
            }
        }
        long avgLat = count > 0 ? (totalLat / count) : 0;
        int overallScore = userMonitors.isEmpty() ? 100 : Math.max(15, Math.min(100, 100 - (downCount * 20) - (avgLat > 600 ? 15 : 0)));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalProjects", userProjects.size());
        out.put("totalMonitors", userMonitors.size());
        out.put("avgLatency", avgLat);
        out.put("activeIncidents", downCount);
        out.put("systemHealth", overallScore);
        out.put("systemStatus", overallScore > 80 ? "HEALTHY" : overallScore > 50 ? "DEGRADED" : "CRITICAL");
        out.put("projects", userProjects.stream().map(this::dto).toList());
        out.put("authenticatedUser", Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        ));
        return out;
    }

    /** Returns only projects belonging to the authenticated user ID */
    @GetMapping("/projects")
    public List<Map<String, Object>> list() {
        User user = securityUtils.getCurrentUserOrFallback();
        return projects.findByOwnerId(user.getId()).stream().map(this::dto).toList();
    }

    /** Verifies that the requested project belongs to the authenticated user ID */
    @GetMapping("/projects/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        User user = securityUtils.getCurrentUserOrFallback();
        Project p = find(id);
        securityUtils.checkProjectOwnership(p, user);
        return dto(p);
    }

    /**
     * Creates a project and automatically associates it with the currently authenticated User ID.
     * Never trusts userId sent from the client/frontend.
     */
    @PostMapping("/projects")
    public Map<String, Object> create(@RequestBody CreateProject r) {
        if (r.name() == null || r.name().isBlank() || r.baseUrl() == null || !r.baseUrl().startsWith("http")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name and valid http(s) baseUrl are required");
        }
        User owner = securityUtils.getCurrentUserOrFallback();
        Project p = new Project(r.name(), "Created dynamically from ARIS workspace", owner);
        p.setSourcePath(r.sourcePath() != null && !r.sourcePath().isBlank() ? r.sourcePath() : Path.of("src/main/java").toAbsolutePath().toString());
        p.setLogPath(r.logPath() != null && !r.logPath().isBlank() ? r.logPath() : Path.of("logs/aris.log").toAbsolutePath().toString());
        p = projects.save(p);
        Service s = services.save(new Service(r.name() + " API", "", r.baseUrl(), p));
        for (String e : r.endpoints() == null ? List.<String>of() : r.endpoints()) {
            String ep = e.trim();
            if (!ep.isEmpty()) {
                monitors.save(new Monitor(ep, ep, "GET", 5, 5, true, s));
            }
        }
        return dto(p);
    }

    /** Dynamically add a new monitored API to a project (guarded by user project ownership) */
    @PostMapping("/projects/{id}/endpoints")
    public Map<String, Object> addEndpoint(@PathVariable Long id, @RequestBody AddEndpoint r) {
        User user = securityUtils.getCurrentUserOrFallback();
        Project p = find(id);
        securityUtils.checkProjectOwnership(p, user);

        Service s = services.findByProjectId(p.getId()).stream().findFirst()
                .orElseGet(() -> services.save(new Service(p.getName() + " API", "", "http://localhost:8080", p)));
        String name = (r.name() == null || r.name().isBlank()) ? r.endpoint() : r.name();
        String method = (r.method() == null || r.method().isBlank()) ? "GET" : r.method().toUpperCase();
        int interval = r.intervalSeconds() != null ? r.intervalSeconds() : 5;
        int timeout = r.timeoutSeconds() != null ? r.timeoutSeconds() : 5;
        Monitor m = monitors.save(new Monitor(name, r.endpoint(), method, interval, timeout, true, s));
        return Map.of("ok", true, "monitorId", m.getId(), "name", m.getName(), "endpoint", m.getEndpoint());
    }

    /** Trigger an immediate on-demand manual probe for an API (guarded by ownership) */
    @PostMapping("/monitors/{id}/test")
    public Map<String, Object> testMonitor(@PathVariable Long id) {
        User user = securityUtils.getCurrentUserOrFallback();
        Monitor m = monitors.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitor not found"));
        securityUtils.checkProjectOwnership(m.getService().getProject(), user);

        String url = m.getEndpoint().startsWith("http") ? m.getEndpoint() : m.getService().getBaseUrl() + m.getEndpoint();
        long t0 = System.nanoTime();
        ProbeResult r = new ProbeResult();
        r.monitorId = m.getId();
        r.ts = System.currentTimeMillis();
        String bodySnippet = "";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(m.getTimeoutSeconds()))
                    .method(m.getMethod().toUpperCase(), HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            r.statusCode = res.statusCode();
            r.responseSize = res.body().length();
            bodySnippet = res.body().length() > 200 ? res.body().substring(0, 200) + "..." : res.body();
        } catch (Exception e) {
            r.statusCode = 0;
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            r.error = msg.length() > 490 ? msg.substring(0, 490) : msg;
            bodySnippet = r.error;
        }
        r.latencyMs = (System.nanoTime() - t0) / 1_000_000;
        results.save(r);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", r.statusCode >= 200 && r.statusCode < 400);
        resp.put("monitorId", m.getId());
        resp.put("name", m.getName());
        resp.put("endpoint", url);
        resp.put("statusCode", r.statusCode);
        resp.put("latencyMs", r.latencyMs);
        resp.put("responseSnippet", bodySnippet);
        resp.put("status", r.statusCode == 0 ? "DOWN (Unreachable)" : r.statusCode >= 500 ? "DOWN (5xx Error)" : r.latencyMs > 500 ? "SLOW" : "UP (200 OK)");
        return resp;
    }

    /** Browse source code explorer files (guarded by project ownership) */
    @GetMapping("/projects/{id}/files")
    public List<String> files(@PathVariable Long id) throws IOException {
        User user = securityUtils.getCurrentUserOrFallback();
        Project p = find(id);
        securityUtils.checkProjectOwnership(p, user);

        Path root = root(p);
        List<String> out = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                return !d.equals(root) && SKIP.contains(d.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                if (OK.matcher(f.getFileName().toString()).find() && out.size() < 2000) {
                    out.add(root.relativize(f).toString().replace('\\', '/'));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(out);
        return out;
    }

    /** Read source file content (guarded by project ownership) */
    @GetMapping(value = "/projects/{id}/file", produces = "text/plain;charset=UTF-8")
    public String file(@PathVariable Long id, @RequestParam String path) {
        User user = securityUtils.getCurrentUserOrFallback();
        Project p = find(id);
        securityUtils.checkProjectOwnership(p, user);

        Path root = root(p);
        try {
            Path f = root.resolve(path).toRealPath();
            if (!f.startsWith(root) || !OK.matcher(f.getFileName().toString()).find() || Files.size(f) > 500_000) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return Files.readString(f);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + path);
        }
    }

    /** Apply developer-approved code patch (guarded by project ownership) */
    @PostMapping("/projects/{id}/patch")
    public Map<String, Object> applyPatch(@PathVariable Long id, @RequestBody ApplyPatch patch) {
        User user = securityUtils.getCurrentUserOrFallback();
        Project p = find(id);
        securityUtils.checkProjectOwnership(p, user);

        Path root = root(p);
        try {
            Path f = root.resolve(patch.file()).toRealPath();
            if (!f.startsWith(root) || !OK.matcher(f.getFileName().toString()).find()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid file access");
            }
            String content = Files.readString(f);
            String before = patch.before().trim();
            if (!content.contains(before)) {
                return Map.of("ok", false, "error", "Target code section not found in file (already patched or changed).");
            }
            String updated = content.replace(before, patch.after().trim());
            Files.writeString(f, updated);
            return Map.of("ok", true, "message", "Patch successfully applied to " + patch.file());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Patch failed: " + e.getMessage());
        }
    }
}
