package com.aris.probe;
import com.aris.monitor.Monitor;
import com.aris.monitor.repository.MonitorRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Every 2s checks which enabled monitors are due (by intervalSeconds) and probes them concurrently. */
@Component
public class ProbeScheduler {
    private final MonitorRepository monitors;
    private final ProbeResultRepository results;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<Long, Long> lastRun = new ConcurrentHashMap<>();

    public ProbeScheduler(MonitorRepository monitors, ProbeResultRepository results) {
        this.monitors = monitors;
        this.results = results;
    }

    @Scheduled(fixedDelay = 2000)
    public void run() {
        long now = System.currentTimeMillis();
        for (Monitor m : monitors.findAll()) {
            if (!m.isEnabled()) continue;
            if (now < lastRun.getOrDefault(m.getId(), 0L) + m.getIntervalSeconds() * 1000L) continue;
            lastRun.put(m.getId(), now);
            Thread.startVirtualThread(() -> results.save(probe(m)));
        }
    }

    private ProbeResult probe(Monitor m) {
        ProbeResult r = new ProbeResult();
        r.monitorId = m.getId();
        r.ts = System.currentTimeMillis();
        String url = m.getEndpoint().startsWith("http") ? m.getEndpoint()
                : m.getService().getBaseUrl() + m.getEndpoint();
        long t0 = System.nanoTime();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(m.getTimeoutSeconds()))
                    .method(m.getMethod().toUpperCase(), HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            r.statusCode = res.statusCode();
            r.responseSize = res.body().length;
        } catch (Exception e) {
            r.statusCode = 0;
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            r.error = msg.length() > 490 ? msg.substring(0, 490) : msg;
        }
        r.latencyMs = (System.nanoTime() - t0) / 1_000_000;
        return r;
    }
}
