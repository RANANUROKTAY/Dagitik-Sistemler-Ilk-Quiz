package com.shopwave.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "request_deduplication")
public class RequestDeduplication {
    @Id
    private String requestId; // İstemciden gelen benzersiz X-Request-Id

    private int responseStatus; // HTTP Durum Kodu (Örn: 200)

    @Column(columnDefinition = "TEXT")
    private String responseBody; // İşlemin başarılı yanıt mesajı

    private LocalDateTime createdAt;
}