package com.aris.project;

public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerEmail;
    private String apiKey;
    private String sourcePath;
    private String logPath;

    public ProjectResponse(Long id, String name, String description, Long ownerId, String ownerEmail) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerEmail = ownerEmail;
    }

    public ProjectResponse(Long id, String name, String description, Long ownerId, String ownerEmail,
                           String apiKey, String sourcePath, String logPath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerEmail = ownerEmail;
        this.apiKey = apiKey;
        this.sourcePath = sourcePath;
        this.logPath = logPath;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerEmail() { return ownerEmail; }
    public String getApiKey() { return apiKey; }
    public String getSourcePath() { return sourcePath; }
    public String getLogPath() { return logPath; }
}
