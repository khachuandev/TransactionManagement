package com.example.Transaction.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private String message;
    private String encryptedTransactionId;
    private String encryptedSourceAccount;
    private String encryptedDestAccount;
    private String encryptedAmount;
    private String encryptedTime;
}
