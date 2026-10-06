package com.aris.config;

import com.aris.auth.Role;
import com.aris.auth.User;
import com.aris.auth.repository.UserRepository;
import com.aris.monitor.Monitor;
import com.aris.monitor.repository.MonitorRepository;
import com.aris.project.Project;
import com.aris.project.repository.ProjectRepository;
import com.aris.service.Service;
import com.aris.service.repository.ServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Seeds initial demo projects on first start:
 * 1. Demo Project (General reliability demo endpoints)
 * 2. E-Commerce Backend (Real-world APIs: /api/products, /api/orders, /api/payment)
 */
@Component
public class DemoSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final ProjectRepository projects;
    private final ServiceRepository services;
    private final MonitorRepository monitors;
    private final PasswordEncoder encoder;

    public DemoSeeder(UserRepository users, ProjectRepository projects, ServiceRepository services,
                      MonitorRepository monitors, PasswordEncoder encoder) {
        this.users = users;
        this.projects = projects;
        this.services = services;
        this.monitors = monitors;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        String src = Path.of("src/main/java").toAbsolutePath().toString();
        String log = Path.of("logs/aris.log").toAbsolutePath().toString();

        // Ensure admin user 'rakshit' exists with password 'admin123'
        User u = users.findByEmailIgnoreCase("rakshit")
                .or(() -> users.findByEmailIgnoreCase("rakshit@aris.dev"))
                .orElseGet(() -> users.save(new User("Rakshit", "rakshit", encoder.encode("admin123"), Role.ADMIN)));

        if (!encoder.matches("admin123", u.getPassword())) {
            u.setPassword(encoder.encode("admin123"));
            u.setRole(Role.ADMIN);
            u = users.save(u);
        }

        final User adminUser = u;
        projects.findAll().forEach(p -> {
            boolean changed = false;
            if (p.getOwner() == null || !p.getOwner().getId().equals(adminUser.getId())) {
                p.setOwner(adminUser);
                changed = true;
            }
            if (p.getSourcePath() == null || p.getSourcePath().isBlank()) {
                p.setSourcePath(src);
                changed = true;
            }
            if (p.getLogPath() == null || p.getLogPath().isBlank()) {
                p.setLogPath(log);
                changed = true;
            }
            if (changed) projects.save(p);
        });

        if (monitors.count() > 0) return;

        // 1. Seed Demo Project
        Project demo = new Project("Demo Project", "System health and latency benchmarks", adminUser);
        demo.setSourcePath(src);
        demo.setLogPath(log);
        Project p1 = projects.save(demo);
        Service s1 = services.save(new Service("Demo API", "Synthetic telemetry demo endpoints", "http://localhost:8080", p1));
        monitors.save(new Monitor("Health API", "/api/health", "GET", 5, 5, true, s1));
        monitors.save(new Monitor("Slow API", "/api/demo/slow", "GET", 5, 5, true, s1));
        monitors.save(new Monitor("Flaky API", "/api/demo/flaky", "GET", 5, 5, true, s1));

        // 2. Seed E-Commerce Backend (User's production-style example)
        Project ecom = new Project("E-Commerce Backend", "Production microservices: Catalog, Orders, Payments", adminUser);
        ecom.setSourcePath(src);
        ecom.setLogPath(log);
        Project p2 = projects.save(ecom);
        Service s2 = services.save(new Service("E-Commerce API", "Core E-Commerce shopping microservices", "http://localhost:8080", p2));
        monitors.save(new Monitor("Products Catalog API", "/api/products", "GET", 5, 5, true, s2));
        monitors.save(new Monitor("Order Processing API", "/api/orders", "GET", 5, 5, true, s2));
        monitors.save(new Monitor("Payment Gateway API", "/api/payment", "GET", 5, 5, true, s2));
    }
}
