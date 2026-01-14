package com.example.Transaction.service.impl;

import com.example.Transaction.config.Translator;
import com.example.Transaction.dto.request.TransactionRequest;
import com.example.Transaction.dto.response.TransactionResponse;
import com.example.Transaction.entity.TransactionHistory;
import com.example.Transaction.exception.TransactionProcessingException;
import com.example.Transaction.repository.TransactionHistoryRepository;
import com.example.Transaction.service.ITransactionService;
import com.example.Transaction.util.AESUtils;
import com.example.Transaction.util.RSAUtils;
import com.example.Transaction.util.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService implements ITransactionService {
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final AESUtils aesUtils;
    private final RSAUtils rsaUtils;
    private final SensitiveDataMasker masker;

    /**
     * Xử lý giao dịch chuyển khoản
     * - Client gửi plain text
     * - AES mã hóa account trước khi lưu DB
     * - HTTPS đảm bảo an toàn khi truyền dữ liệu
     */
    @Override
    @Transactional
    public TransactionResponse processTransfer(TransactionRequest request) {
        try {
            // ===== RSA DECRYPT =====
            String transactionId = rsaUtils.decrypt(request.getTransactionId());
            String sourceAccount = rsaUtils.decrypt(request.getSourceAccount());
            String destAccount = rsaUtils.decrypt(request.getDestAccount());

            BigDecimal amount = new BigDecimal(
                    rsaUtils.decrypt(request.getAmount())
            );

            LocalDateTime time = request.getTime() == null
                    ? LocalDateTime.now()
                    : LocalDateTime.parse(
                    rsaUtils.decrypt(request.getTime()), FORMATTER);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        Translator.toLocale("transaction.amount.invalid"));
            }

            // ===== SAFE LOG =====
            masker.logSafely(
                    "Processing TxID={}, From={}, To={}, Amount={}, Time={}",
                    transactionId, sourceAccount, destAccount, amount, time
            );

            // ===== AES ENCRYPT FOR DB =====
            String encryptedSource = aesUtils.encryptForDB(sourceAccount);
            String encryptedDest = aesUtils.encryptForDB(destAccount);

            // ===== SAVE DEBIT =====
            transactionHistoryRepository.save(TransactionHistory.builder()
                    .transactionId(transactionId)
                    .account(encryptedSource)
                    .inDebt(amount)
                    .have(ZERO_AMOUNT)
                    .time(time)
                    .build()
            );

            // ===== SAVE CREDIT =====
            transactionHistoryRepository.save(TransactionHistory.builder()
                    .transactionId(transactionId)
                    .account(encryptedDest)
                    .inDebt(ZERO_AMOUNT)
                    .have(amount)
                    .time(time)
                    .build()
            );

            return TransactionResponse.builder()
                    .transactionId(transactionId)
                    .sourceAccount(sourceAccount)
                    .destAccount(destAccount)
                    .amount(amount)
                    .time(time)
                    .build();

        } catch (Exception e) {
            log.error("Transaction failed", e);
            throw new TransactionProcessingException(
                    Translator.toLocale("transaction.failed"));
        }
    }
}
