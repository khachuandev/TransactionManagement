package com.example.Transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_time", columnList = "time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String transactionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String account;

    @Column(nullable = false)
    private Double inDebt;

    @Column(nullable = false)
    private Double have;

    @Column(nullable = false)
    private LocalDateTime time;
}
