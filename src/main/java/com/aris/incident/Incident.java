package com.aris.incident;

import com.aris.monitor.Monitor;
import com.aris.service.Service;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "monitor_id", nullable = true)
    private Monitor monitor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", nullable = true)
    private Service service;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String status; // OPEN, ACKNOWLEDGED, RESOLVED

    @Column(nullable = false)
    private String failureType; // crash, latency, rate-limit, token-overrun, timeout

    @Column(nullable = false)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(nullable = false)
    private Double anomalyScore;

    @Column(nullable = false)
    private Double confidence;

    @Column(length = 2000)
    private String rootCause;

    @Column(length = 8000)
    private String remediationAdvice;

    @Column(length = 4000)
    private String metricsJson;

    public Incident() {
        this.timestamp = LocalDateTime.now();
        this.status = "OPEN";
    }

    public Incident(
            Monitor monitor,
            Service service,
            String endpoint,
            String failureType,
            String severity,
            Double anomalyScore,
            Double confidence,
            String rootCause,
            String remediationAdvice,
            String metricsJson) {
        this.monitor = monitor;
        this.service = service;
        this.endpoint = endpoint;
        this.timestamp = LocalDateTime.now();
        this.status = "OPEN";
        this.failureType = failureType;
        this.severity = severity;
        this.anomalyScore = anomalyScore;
        this.confidence = confidence;
        this.rootCause = rootCause;
        this.remediationAdvice = remediationAdvice;
        this.metricsJson = metricsJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureType() {
        return failureType;
    }

    public void setFailureType(String failureType) {
        this.failureType = failureType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(Double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getRemediationAdvice() {
        return remediationAdvice;
    }

    public void setRemediationAdvice(String remediationAdvice) {
        this.remediationAdvice = remediationAdvice;
    }

    public String getMetricsJson() {
        return metricsJson;
    }

    public void setMetricsJson(String metricsJson) {
        this.metricsJson = metricsJson;
    }
}
