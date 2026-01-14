package com.example.Transaction.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

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
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    private String time;
}
