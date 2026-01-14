package com.example.Transaction.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryResponse {
    private String transactionId;
    private String account;
    private BigDecimal inDebt;
    private BigDecimal have;
    private LocalDateTime time;
}
