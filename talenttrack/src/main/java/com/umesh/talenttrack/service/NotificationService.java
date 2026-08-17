package com.umesh.talenttrack.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendEmailFallback")
    public void sendResetEmail(String email, String token) {
        // Simulate outgoing HTTP integration call to external service
        if (email.contains("fail-external")) {
            throw new RuntimeException("External email API service is currently down");
        }
        
        // Log successful mock send
        System.out.println("Successfully sent password reset email to: " + email);
    }

    public void sendEmailFallback(String email, String token, Throwable t) {
        // Fallback logic when circuit breaker is open or service fails
        System.out.println("Resilience4j Fallback triggered for " + email + ". Reason: " + t.getMessage());
    }
}
