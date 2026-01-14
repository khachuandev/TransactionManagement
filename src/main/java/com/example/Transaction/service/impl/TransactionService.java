package com.example.Transaction.service.impl;

import com.example.Transaction.config.Translator;
import com.example.Transaction.dto.request.TransactionRequest;
import com.example.Transaction.dto.response.TransactionHistoryResponse;
import com.example.Transaction.dto.response.TransactionResponse;
import com.example.Transaction.entity.TransactionHistory;
import com.example.Transaction.exception.TransactionProcessingException;
import com.example.Transaction.mapper.TransactionMapper;
import com.example.Transaction.repository.TransactionHistoryRepository;
import com.example.Transaction.service.ITransactionService;
import com.example.Transaction.util.AESUtils;
import com.example.Transaction.util.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService implements ITransactionService {
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final AESUtils aesUtils;
    private final SensitiveDataMasker masker;
    private final TransactionMapper transactionMapper;

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
            String transactionId = request.getTransactionId();
            String sourceAccount = request.getSourceAccount();
            String destAccount = request.getDestAccount();
            BigDecimal amount = request.getAmount();

            LocalDateTime time = (request.getTime() == null || request.getTime().isBlank())
                    ? LocalDateTime.now()
                    : LocalDateTime.parse(request.getTime(), FORMATTER);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }

            masker.logSafely(
                    "Processing TxID={}, From={}, To={}, Amount={}, Time={}",
                    transactionId, sourceAccount, destAccount, amount, time
            );

            String encryptedSource = aesUtils.encryptForDB(sourceAccount);
            String encryptedDest = aesUtils.encryptForDB(destAccount);

            transactionHistoryRepository.save(
                    TransactionHistory.builder()
                            .transactionId(transactionId)
                            .account(encryptedSource)
                            .inDebt(amount)
                            .have(ZERO_AMOUNT)
                            .time(time)
                            .build()
            );

            transactionHistoryRepository.save(
                    TransactionHistory.builder()
                            .transactionId(transactionId)
                            .account(encryptedDest)
                            .inDebt(ZERO_AMOUNT)
                            .have(amount)
                            .time(time)
                            .build()
            );

            return TransactionResponse.builder()
                    .message("Transaction processed successfully")
                    .encryptedTransactionId(transactionId)
                    .encryptedSourceAccount(sourceAccount)
                    .encryptedDestAccount(destAccount)
                    .encryptedAmount(String.valueOf(amount))
                    .encryptedTime(String.valueOf(time))
                    .build();

        } catch (Exception e) {
            log.error("Transaction failed", e);
            throw new TransactionProcessingException(
                    Translator.toLocale("transaction.failed")
            );
        }
    }

    /**
     * Lấy lịch sử giao dịch theo TransactionID
     */
    @Override
    public List<TransactionHistoryResponse> getTransactionsByTransactionId(String transactionId) {
        return transactionHistoryRepository.findByTransactionId(transactionId)
                .stream()
                .map(transactionMapper::toHistoryResponse)
                .collect(Collectors.toList());
    }
}
