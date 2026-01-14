package com.example.Transaction.controller;

import com.example.Transaction.dto.request.TransactionRequest;
import com.example.Transaction.util.RSAUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("${api.prefix}/encrypt")
@RequiredArgsConstructor
@Tag(name = "Encryption Test", description = "RSA encryption APIs (Local/Dev only)")
public class EncryptionTestController {
    private final RSAUtils rsaUtils;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Operation(summary = "Mã hóa request giao dịch bằng RSA (test)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mã hóa thành công",
                    content = @Content(schema = @Schema(implementation = TransactionRequest.class)))
    })
    @PostMapping("/transaction")
    public TransactionRequest encryptTransaction(@RequestBody TransactionRequest request) {
        TransactionRequest encrypted = new TransactionRequest();

        encrypted.setTransactionId(rsaUtils.encrypt(request.getTransactionId()));
        encrypted.setSourceAccount(rsaUtils.encrypt(request.getSourceAccount()));
        encrypted.setDestAccount(rsaUtils.encrypt(request.getDestAccount()));
        encrypted.setAmount(rsaUtils.encrypt(request.getAmount()));

        String time = request.getTime();
        if (time == null || time.isBlank()) {
            time = LocalDateTime.now().format(FORMATTER);
        }
        encrypted.setTime(rsaUtils.encrypt(time));

        return encrypted;
    }
}
