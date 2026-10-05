package com.aris.ecom;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * E-Commerce Product Catalog Section:
 * Exposes product inventory, categories, and item availability.
 */
@RestController
public class ProductController {

    @GetMapping({"/api/products", "/api/ecom/products"})
    public Map<String, Object> getProducts() {
        return Map.of(
                "status", "ok",
                "items", 48,
                "category", "electronics",
                "inventoryAvailable", true
        );
    }
}
