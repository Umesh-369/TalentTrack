package com.umesh.talenttrack.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    // General rate limiter per IP to prevent DDoS (e.g., 20 attempts per minute)
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    
    // Invariant 15: Failure counters and lockout timestamps per email+IP
    private final Map<String, Integer> failureCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockoutExpiry = new ConcurrentHashMap<>();

    private Bucket createIpBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(20).refillIntervally(20, Duration.ofMinutes(1)).build())
                .build();
    }

    public boolean checkIpRateLimit(String ipAddress) {
        Bucket bucket = ipBuckets.computeIfAbsent(ipAddress, k -> createIpBucket());
        return bucket.tryConsume(1);
    }

    public boolean isLockedOut(String email, String ipAddress) {
        String key = email.toLowerCase().trim() + ":" + ipAddress;
        LocalDateTime expiry = lockoutExpiry.get(key);
        
        if (expiry == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(expiry)) {
            // Lockout expired, reset counters
            lockoutExpiry.remove(key);
            failureCounts.remove(key);
            return false;
        }

        return true;
    }

    public void recordFailure(String email, String ipAddress) {
        String key = email.toLowerCase().trim() + ":" + ipAddress;
        int count = failureCounts.getOrDefault(key, 0) + 1;
        failureCounts.put(key, count);

        if (count >= 5) {
            lockoutExpiry.put(key, LocalDateTime.now().plusMinutes(15));
        }
    }

    public void resetFailures(String email, String ipAddress) {
        String key = email.toLowerCase().trim() + ":" + ipAddress;
        failureCounts.remove(key);
        lockoutExpiry.remove(key);
    }

    public long getRemainingSeconds(String email, String ipAddress) {
        String key = email.toLowerCase().trim() + ":" + ipAddress;
        LocalDateTime expiry = lockoutExpiry.get(key);
        if (expiry == null) return 0;
        long secs = Duration.between(LocalDateTime.now(), expiry).toSeconds();
        return Math.max(secs, 0);
    }
}
