package com.shopwave.service; // service klasörüne uygun paket tanımı

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

    public void maybeSleep() {
        if (!enabled) return;

        // Jitter (seğirme) hesaplama: Rastgele değişkenlik sınırı
        long extra = (jitterMs > 0) ?
                ThreadLocalRandom.current().nextLong(0, jitterMs + 1) : 0;

        long total = delayMs + extra;

        try {
            Thread.sleep(total); // Belirlenen toplam gecikme süresi kadar bekletir
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}