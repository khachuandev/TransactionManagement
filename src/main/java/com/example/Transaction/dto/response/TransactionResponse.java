package com.example.Transaction.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private String transactionId;
    private String sourceAccount;
    private String destAccount;
    private BigDecimal amount;
    private LocalDateTime time;
}
