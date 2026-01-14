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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    // ĐỌC CỜ RSA TỪ CONFIG
    @Value("${rsa.encryption.enabled:false}")
    private boolean rsaEnabled;

    /**
     * Xử lý giao dịch chuyển khoản
     * - Hỗ trợ cả RSA encrypted và plain text (tùy config)
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
            log.debug("Processing transaction with RSA encryption: {}", rsaEnabled);

            // BƯỚC 1: GIẢI MÃ RSA NẾU ENABLED, KHÔNG THÌ DÙNG PLAIN TEXT
            if (rsaEnabled) {
                // Giải mã RSA tất cả parameters
                transactionId = rsaUtils.decrypt(request.getTransactionId());
                sourceAccount = rsaUtils.decrypt(request.getSourceAccount());
                destAccount = rsaUtils.decrypt(request.getDestAccount());

                String amountStr = rsaUtils.decrypt(request.getAmount());
                amount = Double.parseDouble(amountStr);

                String timeStr = request.getTime();
                if (timeStr != null && !timeStr.isBlank()) {
                    timeStr = rsaUtils.decrypt(timeStr);
                    time = LocalDateTime.parse(timeStr, DATE_TIME_FORMATTER);
                } else {
                    time = LocalDateTime.now();
                }
            } else {
                // MODE PLAIN TEXT (CHO TEST LOCAL)
                transactionId = request.getTransactionId();
                sourceAccount = request.getSourceAccount();
                destAccount = request.getDestAccount();
                amount = Double.parseDouble(request.getAmount());

                String timeStr = request.getTime();
                if (timeStr != null && !timeStr.isBlank()) {
                    time = LocalDateTime.parse(timeStr, DATE_TIME_FORMATTER);
                } else {
                    time = LocalDateTime.now();
                }
            }

            // BƯỚC 2: VALIDATE SAU KHI GIẢI MÃ
            if (transactionId == null || transactionId.isBlank()) {
                throw new IllegalArgumentException("Transaction ID must not be blank");
            }
            if (sourceAccount == null || sourceAccount.isBlank()) {
                throw new IllegalArgumentException("Source account must not be blank");
            }
            if (destAccount == null || destAccount.isBlank()) {
                throw new IllegalArgumentException("Destination account must not be blank");
            }
            if (amount == null || amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }

            // BƯỚC 3: LOG AN TOÀN (đã che dữ liệu nhạy cảm)
            masker.logSafely("Processing transaction - TxID: {}, From: {}, To: {}, Amount: {}, Time: {}",
                    transactionId, sourceAccount, destAccount, amount, time);

            // BƯỚC 4: MÃ HÓA AES ACCOUNT TRƯỚC KHI LƯU DB
            String encryptedSourceAccount = aesUtils.encryptForDB(sourceAccount);
            String encryptedDestAccount = aesUtils.encryptForDB(destAccount);

            // BƯỚC 5: TẠO 2 BẢN GHI (NỢ + CÓ)
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

            // BƯỚC 6: MÃ HÓA RSA KẾT QUẢ TRẢ VỀ (NẾU ENABLED)
            if (rsaEnabled) {
                return TransactionResponse.builder()
                        .message("Transaction processed successfully")
                        .encryptedTransactionId(rsaUtils.encrypt(transactionId))
                        .encryptedSourceAccount(rsaUtils.encrypt(sourceAccount))
                        .encryptedDestAccount(rsaUtils.encrypt(destAccount))
                        .encryptedAmount(rsaUtils.encrypt(amount.toString()))
                        .encryptedTime(rsaUtils.encrypt(time.format(DATE_TIME_FORMATTER)))
                        .build();
            } else {
                // Plain text response (cho test)
                return TransactionResponse.builder()
                        .message("Transaction processed successfully")
                        .encryptedTransactionId(transactionId)
                        .encryptedSourceAccount(sourceAccount)
                        .encryptedDestAccount(destAccount)
                        .encryptedAmount(amount.toString())
                        .encryptedTime(time.format(DATE_TIME_FORMATTER))
                        .build();
            }

        } catch (NumberFormatException e) {
            // PHẢI CATCH NumberFormatException TRƯỚC vì nó là subclass của IllegalArgumentException
            log.error("Failed to parse amount: {}", masker.maskException(e));
            throw new TransactionProcessingException("Invalid amount format");

        } catch (DateTimeParseException e) {
            // XỬ LÝ LỖI PARSE TIME
            log.error("Failed to parse time: {}", masker.maskException(e));
            throw new TransactionProcessingException("Invalid time format");

        } catch (IllegalArgumentException e) {
            // LOG VÀ THROW EXCEPTION AN TOÀN (không lộ thông tin)
            log.warn("Validation failed: {}", masker.maskException(e));
            throw new IllegalArgumentException("Invalid transaction parameters");

        } catch (Exception e) {
            // XỬ LÝ CÁC LỖI KHÁC (RSA decrypt, DB...)
            log.error("Transaction processing error: {}", masker.maskException(e));
            throw new TransactionProcessingException(Translator.toLocale("transaction.failed"));
        }
    }

    /**
     * Lấy danh sách lịch sử giao dịch theo TransactionID.
     * - Truy vấn DB theo TransactionID (plain text)
     * - Giải mã AES số tài khoản thông qua mapper
     * - Trả về danh sách TransactionHistoryResponse
     */
    @Override
    public List<TransactionHistoryResponse> getTransactionsByTransactionId(String transactionId) {
        List<TransactionHistory> list = transactionHistoryRepository.findByTransactionId(transactionId);

        return list.stream()
                .map(transactionMapper::toHistoryResponse)
                .collect(Collectors.toList());
    }
}