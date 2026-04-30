package com.shopwave.controller;

import com.shopwave.service.IdempotencyService;
import com.shopwave.service.InventoryService;
import com.shopwave.service.LatencyInjector;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;
    private final LatencyInjector latencyInjector;

    public InventoryController(InventoryService inventoryService,
                               IdempotencyService idempotencyService,
                               LatencyInjector latencyInjector) {
        this.inventoryService = inventoryService;
        this.idempotencyService = idempotencyService;
        this.latencyInjector = latencyInjector;
    }

    @PostMapping("/stock-update")
    public ResponseEntity<?> updateStock(
            @RequestHeader("X-Request-Id") String requestId,
            @RequestHeader(value = "X-Deadline-Ms", defaultValue = "2000") long timeoutMs,
            @RequestBody StockUpdateRequest request) {

        return idempotencyService.findRequest(requestId)
                .map(processed -> ResponseEntity.status(processed.getResponseStatus()).body(processed.getResponseBody()))
                .orElseGet(() -> {
                    try {
                        latencyInjector.setDeadline(timeoutMs);
                        latencyInjector.maybeSleep();

                        // Lombok çalışmazsa manuel getter kullanabilirsin: request.productId
                        inventoryService.decreaseStock(request.getProductId(), request.getQuantity());

                        String msg = "Stok basariyla guncellendi.";
                        idempotencyService.saveResponse(requestId, 200, msg);
                        return ResponseEntity.ok(msg);
                    } catch (Exception e) {
                        return ResponseEntity.status(408).body(e.getMessage());
                    } finally {
                        latencyInjector.clear();
                    }
                });
    }
}

// Bu sınıfın controller dosyasının en altında olduğundan emin ol
class StockUpdateRequest {
    private Long productId;
    private int quantity;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}