package com.aris.probe;
import com.aris.monitor.Monitor;
import com.aris.monitor.repository.MonitorRepository;
import com.aris.auth.User;
import com.aris.project.Project;
import com.aris.project.repository.ProjectRepository;
import com.aris.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    record Point(long t, long latency, int status) {}

    private final MonitorRepository monitors;
    private final ProbeResultRepository results;
    private final ProjectRepository projects;
    private final SecurityUtils securityUtils;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DashboardController(MonitorRepository monitors, ProbeResultRepository results,
                               ProjectRepository projects, SecurityUtils securityUtils) {
        this.monitors = monitors;
        this.results = results;
        this.projects = projects;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/monitors")
    public List<Map<String, Object>> monitors(@RequestParam(required = false) Long projectId) {
        User user = securityUtils.getCurrentUserOrFallback();
        if (projectId != null) {
            Project p = projects.findById(projectId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
            securityUtils.checkProjectOwnership(p, user);
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Monitor m : monitors.findAll()) {
            if (projectId != null) {
                if (!m.getService().getProject().getId().equals(projectId)) continue;
            } else {
                // Global monitors list: only include monitors from projects owned by current user
                if (!m.getService().getProject().getOwner().getId().equals(user.getId())) continue;
            }
            List<ProbeResult> h = new ArrayList<>(results.findTop40ByMonitorIdOrderByTsDesc(m.getId()));
            Collections.reverse(h);
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", m.getId());
            o.put("name", m.getName());
            o.put("endpoint", m.getEndpoint());
            if (h.isEmpty()) {
                o.put("status", "PENDING");
                o.put("history", List.of());
                out.add(o);
                continue;
            }
            ProbeResult last = h.get(h.size() - 1);
            double avg = h.stream().mapToLong(p -> p.latencyMs).average().orElse(0);
            double prevAvg = h.size() > 1
                    ? h.subList(0, h.size() - 1).stream().mapToLong(p -> p.latencyMs).average().orElse(0) : avg;
            long ok = h.stream().filter(p -> p.statusCode >= 200 && p.statusCode < 400).count();
            // Rule-based incident detection (the Isolation Forest engine replaces this later)
            String status = "UP", incident = null;
            if (last.statusCode == 0 || last.statusCode >= 500) {
                status = "DOWN";
                incident = last.statusCode == 0 ? "No response: " + last.error : "HTTP " + last.statusCode + " server error";
            } else if (last.latencyMs > 500 && last.latencyMs > 3 * prevAvg) {
                status = "SLOW";
                incident = "Latency " + last.latencyMs + " ms is over 3x the recent average (" + Math.round(prevAvg) + " ms)";
            }
            o.put("status", status);
            o.put("incident", incident);
            o.put("lastLatency", last.latencyMs);
            o.put("avgLatency", Math.round(avg));
            o.put("uptime", Math.round(100.0 * ok / h.size()));
            o.put("history", h.stream().map(p -> new Point(p.ts, p.latencyMs, p.statusCode)).toList());
            out.add(o);
        }
        return out;
    }

    /** Resources of the machine the backend runs on. */
    @GetMapping("/host")
    public Map<String, Object> host() {
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long total = os.getTotalMemorySize(), free = os.getFreeMemorySize();
        File disk = new File(".");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cpuPercent", Math.max(0L, Math.round(os.getCpuLoad() * 100)));
        m.put("ramUsedMb", (total - free) / 1048576);
        m.put("ramTotalMb", total / 1048576);
        m.put("diskUsedGb", (disk.getTotalSpace() - disk.getFreeSpace()) / 1073741824L);
        m.put("diskTotalGb", disk.getTotalSpace() / 1073741824L);
        return m;
    }

    /** System-wide overview aggregated across all projects. */
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        var allMonitors = monitors.findAll();
        long totalMonitors = allMonitors.size();
        long totalProjects = projects.count();

        long upCount = 0, slowCount = 0, downCount = 0;
        long totalLatency = 0, latencySamples = 0, totalUptimePercent = 0;

        for (Monitor m : allMonitors) {
            List<ProbeResult> h = results.findTop40ByMonitorIdOrderByTsDesc(m.getId());
            if (h.isEmpty()) continue;
            ProbeResult last = h.get(0);
            double avg = h.stream().mapToLong(p -> p.latencyMs).average().orElse(0);
            long ok = h.stream().filter(p -> p.statusCode >= 200 && p.statusCode < 400).count();
            long uptime = Math.round(100.0 * ok / h.size());
            totalUptimePercent += uptime;

            totalLatency += (long) avg;
            latencySamples++;

            if (last.statusCode == 0 || last.statusCode >= 500) downCount++;
            else if (last.latencyMs > 500) slowCount++;
            else upCount++;
        }

        long avgLatency = latencySamples > 0 ? totalLatency / latencySamples : 0;
        long avgUptime = latencySamples > 0 ? totalUptimePercent / latencySamples : 100;
        long overallHealth = Math.max(10, Math.min(100, avgUptime - (downCount * 15) - (slowCount * 8)));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalProjects", totalProjects);
        out.put("totalMonitors", totalMonitors);
        out.put("overallHealth", overallHealth);
        out.put("avgLatency", avgLatency);
        out.put("upCount", upCount);
        out.put("slowCount", slowCount);
        out.put("downCount", downCount);
        out.put("activeIncidents", downCount + slowCount);
        return out;
    }

    /** Manual on-demand test of any endpoint. */
    @PostMapping("/test")
    public Map<String, Object> testEndpoint(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || !url.startsWith("http")) {
            return Map.of("ok", false, "error", "Invalid URL: Must start with http:// or https://");
        }
        long t0 = System.nanoTime();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            long latency = (System.nanoTime() - t0) / 1_000_000;
            String snippet = res.body();
            if (snippet.length() > 500) snippet = snippet.substring(0, 500) + "...";
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("statusCode", res.statusCode());
            out.put("latencyMs", latency);
            out.put("status", res.statusCode() >= 200 && res.statusCode() < 400 ? "UP" : "DOWN");
            out.put("body", snippet);
            return out;
        } catch (Exception e) {
            long latency = (System.nanoTime() - t0) / 1_000_000;
            return Map.of("ok", false, "statusCode", 0, "latencyMs", latency, "status", "DOWN", "error", e.getMessage());
        }
    }
}
