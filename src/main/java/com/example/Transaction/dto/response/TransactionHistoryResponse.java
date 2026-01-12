package com.example.Transaction.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryResponse {
    private String transactionId;
    private String account;
    private Double inDebt;
    private Double have;
    private LocalDateTime time;
}
