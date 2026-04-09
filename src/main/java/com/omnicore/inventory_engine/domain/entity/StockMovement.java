package com.omnicore.inventory_engine.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "product_sku", nullable = false)
    private String productSku;

    @Column(nullable = false)
    private Integer delta;

    @Column(nullable = false)
    private String reason;

    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
