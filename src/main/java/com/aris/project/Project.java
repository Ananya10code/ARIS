package com.aris.project;

import com.aris.auth.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 1000)
    private String sourcePath;   // folder with the project's source code (shown in the IDE view)

    @Column(length = 1000)
    private String logPath;      // the project's log file (stack traces are read from here)

    @Column(length = 100)
    private String apiKey;       // unique project API key for client integration

    public Project() {
        this.apiKey = generateKey();
    }

    public Project(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.apiKey = generateKey();
    }

    private String generateKey() {
        return "aris_live_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public User getOwner() { return owner; }
    public String getSourcePath() { return sourcePath; }
    public String getLogPath() { return logPath; }
    public String getApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = generateKey();
        }
        return apiKey;
    }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setOwner(User owner) { this.owner = owner; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
