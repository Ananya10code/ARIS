package com.aris.ecom;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * E-Commerce Order Section:
 * Exposes order fulfillment, processing queue, and revenue tracking.
 */
@RestController
public class OrderController {

    @GetMapping({"/api/orders", "/api/ecom/orders"})
    public Map<String, Object> getOrders() {
        return Map.of(
                "status", "ok",
                "ordersToday", 142,
                "pendingFulfillment", 8,
                "revenue", 18950.0
        );
    }
}
