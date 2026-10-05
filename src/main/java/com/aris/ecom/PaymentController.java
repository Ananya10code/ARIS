package com.aris.ecom;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Random;

/**
 * E-Commerce Payment Gateway Section:
 * Exposes payment processing with live connection pool simulation
 * and toggleable fault injection for ARIS SRE failure/recovery testing.
 */
@RestController
public class PaymentController {
    private final Random rnd = new Random();
    private static volatile boolean paymentFaultActive = false;

    @GetMapping({"/api/payment", "/api/ecom/payment"})
    public Map<String, Object> processPayment() {
        if (paymentFaultActive || rnd.nextInt(100) < 25) {
            throw new IllegalStateException("Payment provider gateway error: connection pool exhausted to bank switch");
        }
        return Map.of(
                "status", "ok",
                "transactionId", "TXN-" + System.currentTimeMillis(),
                "result", "SUCCESS",
                "currency", "USD"
        );
    }

    @PostMapping({"/api/demo/payment/toggle-fault", "/api/ecom/payment/toggle-fault"})
    public Map<String, Object> togglePaymentFault(@RequestParam(required = false) Boolean fail) {
        paymentFaultActive = fail != null ? fail : !paymentFaultActive;
        return Map.of(
                "paymentFaultActive", paymentFaultActive,
                "message", paymentFaultActive
                        ? "Fault injected: /api/payment will now fail with 500 error."
                        : "Fault cleared: /api/payment restored to normal operation."
        );
    }

    @GetMapping({"/api/demo/payment/fault-status", "/api/ecom/payment/fault-status"})
    public Map<String, Object> faultStatus() {
        return Map.of("paymentFaultActive", paymentFaultActive);
    }
}
