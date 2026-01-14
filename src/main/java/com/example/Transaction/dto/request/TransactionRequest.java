package com.example.Transaction.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequest {
    @NotBlank(message = "Transaction ID must not be blank")
    private String transactionId;

    @NotBlank(message = "Source account must not be blank")
    private String sourceAccount;

    @NotBlank(message = "Destination account must not be blank")
    private String destAccount;

    @NotBlank(message = "Amount must not be blank")
    private String amount;

    private String time;
}
