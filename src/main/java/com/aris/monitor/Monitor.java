package com.aris.monitor;
import com.aris.service.Service;
import jakarta.persistence.*;
@Entity
@Table(name = "monitors")
public class Monitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String endpoint;
    @Column(nullable = false)
    private String method;
    @Column(nullable = false)
    private Integer intervalSeconds;
    @Column(nullable = false)
    private Integer timeoutSeconds;
    @Column(nullable = false)
    private boolean enabled;
    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;
    public Monitor() {
    }
    public Monitor(
            String name,
            String endpoint,
            String method,
            Integer intervalSeconds,
            Integer timeoutSeconds,
            boolean enabled,
            Service service) {
        this.name = name;
        this.endpoint = endpoint;
        this.method = method;
        this.intervalSeconds = intervalSeconds;
        this.timeoutSeconds = timeoutSeconds;
        this.enabled = enabled;
        this.service = service;
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getEndpoint() {
        return endpoint;
    }
    public String getMethod() {
        return method;
    }
    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public Service getService() {
        return service;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public void setService(Service service) {
        this.service = service;
    }
}
