package com.shopwave.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LatencyInjector {

    @Value("${lab.latency.enabled:false}")
    private boolean enabled;

    @Value("${lab.latency.delayMs:0}")
    private long delayMs;

    @Value("${lab.latency.jitterMs:0}")
    private long jitterMs;

    // Her isteğin (thread) kendine özel zaman bütçesini taşıyan şerit
    private static final ThreadLocal<Long> deadlineTimestamp = new ThreadLocal<>();

    /**
     * İsteğin sistemde maksimum hayatta kalma süresini belirler.
     * @param timeoutMs İstemcinin belirlediği süre (ms)
     */
    public void setDeadline(long timeoutMs) {
        deadlineTimestamp.set(System.currentTimeMillis() + timeoutMs);
    }

    /**
     * İşlem bittikten sonra ThreadLocal alanını temizler.
     */
    public void clear() {
        deadlineTimestamp.remove();
    }

    /**
     * Gecikme simülasyonu yapar ve her adımda zaman bütçesini kontrol eder.
     */
    public void maybeSleep() {
        checkDeadline(); // İşlem başlamadan önce bütçe kontrolü

        if (!enabled) return;

        long extra = (jitterMs > 0) ?
                ThreadLocalRandom.current().nextLong(0, jitterMs + 1) : 0;

        try {
            Thread.sleep(delayMs + extra); // Jitter ile genişletilmiş gecikme[cite: 1]
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        checkDeadline(); // Beklemeden sonra bütçe kontrolü[cite: 5]
    }

    /**
     * Zaman bütçesi dolmuşsa işlemi anında keser (Güvenlik Kapısı).[cite: 5]
     */
    public void checkDeadline() {
        Long deadline = deadlineTimestamp.get();
        if (deadline != null && System.currentTimeMillis() > deadline) {
            // Sınırsız bekleme krizinden Graceful Failure mimarisine geçiş
            throw new RuntimeException("408 Request Timeout: Zaman bütçesi (Deadline) doldu!");
        }
    }
}