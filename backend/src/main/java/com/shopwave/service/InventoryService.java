package com.shopwave.service;

import com.shopwave.domain.Inventory;
import com.shopwave.exception.InsufficientStockException;
import com.shopwave.exception.NotFoundException;
import com.shopwave.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * InventoryService — stok rezervasyon ve güncelleme işlemleri.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService { // Sınıf isminin dosya ismiyle aynı olduğundan emin ol

    private final InventoryRepository inventoryRepository;
    private final AuditService        auditService;

    // ─── Controller için Eklenen Metot (Terminal Testi İçin) ───

    /**
     * Terminalden gelen doğrudan stok düşürme isteğini işler.
     * Bu metot InventoryController tarafından çağrılır.
     */
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        log.info("Terminal istegi ile stok dusurme baslatildi. Ürün: {}, Miktar: {}", productId, quantity);

        // Mevcut deduct mantığını kullanarak işlemi gerçekleştiriyoruz
        this.deduct(productId, quantity);
    }

    // ─── Queries ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Inventory getByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found for product: " + productId));
    }

    @Transactional(readOnly = true)
    public List<Inventory> getLowStock(int threshold) {
        return inventoryRepository.findLowStock(threshold);
    }

    // ─── Commands ─────────────────────────────────────────────

    /**
     * Sipariş tesliminde veya doğrudan stok düşümünde fiziksel stoktan düşer.
     * Pessimistic lock ile race condition önlenir.
     */
    @Transactional
    public void deduct(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + productId));

        if (inv.getQuantity() < quantity) {
            throw new InsufficientStockException(productId, inv.getQuantity(), quantity);
        }

        inv.setQuantity(inv.getQuantity() - quantity);
        inventoryRepository.save(inv);

        auditService.log("STOCK_DEDUCTED", "Inventory", productId,
                "qty=" + quantity + " remaining=" + inv.getQuantity());
        log.info("Stock deducted productId={} qty={} remaining={}", productId, quantity, inv.getQuantity());
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + productId));

        if (!inv.canReserve(quantity)) {
            throw new InsufficientStockException(productId, inv.availableQuantity(), quantity);
        }

        inv.reserve(quantity);
        inventoryRepository.save(inv);

        auditService.log("STOCK_RESERVED", "Inventory", productId,
                "qty=" + quantity + " remaining=" + inv.availableQuantity());
    }

    @Transactional
    public void release(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + productId));

        inv.release(quantity);
        inventoryRepository.save(inv);

        auditService.log("STOCK_RELEASED", "Inventory", productId, "qty=" + quantity);
    }

    @Transactional
    public void addStock(Long productId, int quantity) {
        Inventory inv = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found: " + productId));

        inv.setQuantity(inv.getQuantity() + quantity);
        inventoryRepository.save(inv);

        auditService.log("STOCK_ADDED", "Inventory", productId,
                "added=" + quantity + " total=" + inv.getQuantity());
    }
}