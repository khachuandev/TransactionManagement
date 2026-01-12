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
import com.example.Transaction.util.RSAUtils;
import com.example.Transaction.util.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService implements ITransactionService {
    public static final double ZERO_AMOUNT = 0.0;
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final AESUtils aesUtils;
    private final RSAUtils rsaUtils;
    private final SensitiveDataMasker masker;
    private final TransactionMapper transactionMapper;

    /**
     * Xử lý giao dịch chuyển khoản
     * - Giải mã RSA các parameters từ inter-service
     * - Mã hóa AES Account trước khi lưu DB
     * - Tạo 2 bản ghi: NỢ (source) và CÓ (dest)
     */
    @Override
    @Transactional
    public TransactionResponse processTransfer(TransactionRequest request) {
        String transactionId = null;
        String sourceAccount = null;
        String destAccount = null;
        Double amount = null;
        LocalDateTime time = null;

        try {
            transactionId = request.getTransactionId();
            sourceAccount = request.getSourceAccount();
            destAccount = request.getDestAccount();
            amount = request.getAmount();

            if (transactionId == null || transactionId.isBlank() ||
                    sourceAccount == null || sourceAccount.isBlank() ||
                    destAccount == null || destAccount.isBlank() ||
                    amount == null || amount <= 0) {
                throw new IllegalArgumentException(Translator.toLocale("transaction.invalid"));
            }

            String timeStr = request.getTime();
            time = (timeStr == null || timeStr.isBlank()) ?
                    LocalDateTime.now() :
                    LocalDateTime.parse(timeStr, DATE_TIME_FORMATTER);

            masker.logSafely("Processing transaction - TxID: {}, From: {}, To: {}, Amount: {}, Time: {}",
                    transactionId, sourceAccount, destAccount, amount, time);

            String encryptedSourceAccount = aesUtils.encryptForDB(sourceAccount);
            String encryptedDestAccount = aesUtils.encryptForDB(destAccount);

            TransactionHistory debitTx = TransactionHistory.builder()
                    .transactionId(transactionId)
                    .account(encryptedSourceAccount)
                    .inDebt(amount)
                    .have(ZERO_AMOUNT)
                    .time(time)
                    .build();

            TransactionHistory creditTx = TransactionHistory.builder()
                    .transactionId(transactionId)
                    .account(encryptedDestAccount)
                    .inDebt(ZERO_AMOUNT)
                    .have(amount)
                    .time(time)
                    .build();

            transactionHistoryRepository.save(debitTx);
            transactionHistoryRepository.save(creditTx);

            log.info("Transaction completed successfully - TxID: {}", masker.mask(transactionId));

            return TransactionResponse.builder()
                    .message("Transaction processed successfully")
                    .encryptedTransactionId(rsaUtils.encrypt(transactionId))
                    .encryptedSourceAccount(rsaUtils.encrypt(sourceAccount))
                    .encryptedDestAccount(rsaUtils.encrypt(destAccount))
                    .encryptedAmount(rsaUtils.encrypt(amount.toString()))
                    .encryptedTime(rsaUtils.encrypt(time.format(DATE_TIME_FORMATTER)))
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn("Validation failed: {}", masker.maskException(e));
            throw e;
        } catch (RuntimeException e) {
            log.error("Business or system error: {}", masker.maskException(e));
            throw new TransactionProcessingException(Translator.toLocale("transaction.failed"));
        }
    }

    /**
     * Lấy danh sách lịch sử giao dịch theo TransactionID.
     * - Truy vấn DB theo TransactionID.
     * - Giải mã AES số tài khoản.
     * - Trả về danh sách TransactionHistoryResponse.
     */
    @Override
    public List<TransactionHistoryResponse> getTransactionsByTransactionId(String transactionId) {
        List<TransactionHistory> list = transactionHistoryRepository.findByTransactionId(transactionId);

        return list.stream()
                .map(transactionMapper::toHistoryResponse)
                .collect(Collectors.toList());
    }
}

