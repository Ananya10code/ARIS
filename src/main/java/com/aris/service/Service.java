package com.aris.service;
import com.aris.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
@Entity
@Table(name = "services")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(length = 500)
    private String description;
    @Column(nullable = false)
    private String baseUrl;
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    public Service() {
    }
    public Service(
            String name,
            String description,
            String baseUrl,
            Project project) {
        this.name = name;
        this.description = description;
        this.baseUrl = baseUrl;
        this.project = project;
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public String getBaseUrl() {
        return baseUrl;
    }
    public Project getProject() {
        return project;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    public void setProject(Project project) {
        this.project = project;
    }
}