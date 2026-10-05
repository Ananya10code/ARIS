package com.aris.probe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Random;

/**
 * Synthetic telemetry endpoints for the "Demo Project":
 * - /api/demo/slow: Latency spike simulation
 * - /api/demo/flaky: Intermittent DB connection pool failure
 */
@RestController
public class DemoController {
    private final Random rnd = new Random();

    @GetMapping("/api/demo/slow")
    public String slow() throws InterruptedException {
        Thread.sleep(rnd.nextInt(100) < 15 ? 2500 + rnd.nextInt(1000) : 100 + rnd.nextInt(200));
        return "ok";
    }

    @GetMapping("/api/demo/flaky")
    public String flaky() {
        if (rnd.nextInt(100) < 30) {
            throw new IllegalStateException("Simulated DB connection pool exhausted");
        }
        return "ok";
    }
}
