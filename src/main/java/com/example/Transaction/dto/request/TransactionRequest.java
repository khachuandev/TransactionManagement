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

    @NotNull(message = "Amount must not be null")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    private String time;
}
