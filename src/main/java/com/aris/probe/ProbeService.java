package com.aris.probe;

import com.aris.monitor.Monitor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
public class ProbeService {

    private final WebClient webClient;

    public ProbeService(WebClient webClient) {
        this.webClient = webClient;
    }

    public ProbeResult executeProbe(Monitor monitor) {
        String baseUrl = monitor.getService().getBaseUrl();
        String endpoint = monitor.getEndpoint();
        String url = (baseUrl.endsWith("/") || endpoint.startsWith("/"))
                ? baseUrl + endpoint
                : baseUrl + "/" + endpoint;

        ProbeResult result = new ProbeResult();
        result.monitorId = monitor.getId();
        result.ts = System.currentTimeMillis();

        long start = System.currentTimeMillis();
        try {
            HttpMethod method = HttpMethod.valueOf(monitor.getMethod().toUpperCase());

            Integer status = webClient
                    .method(method)
                    .uri(url)
                    .exchangeToMono(response -> response.releaseBody().thenReturn(response.statusCode().value()))
                    .timeout(Duration.ofSeconds(
                            monitor.getTimeoutSeconds() != null ? monitor.getTimeoutSeconds() : 10))
                    .block();

            result.latencyMs = System.currentTimeMillis() - start;
            result.statusCode = status != null ? status : 0;
            result.error = (result.statusCode >= 200 && result.statusCode < 400)
                    ? null
                    : "HTTP status: " + result.statusCode;
        } catch (Exception e) {
            result.latencyMs = System.currentTimeMillis() - start;
            result.statusCode = 0;
            result.error = e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500)
                    : e.getMessage();
        }

        return result;
    }
}
